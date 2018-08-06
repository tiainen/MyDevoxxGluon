/**
 * Copyright (c) 2017, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.charm.down.plugins.android;

import android.os.Bundle;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import com.devoxx.model.Conference;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.model.Talk;
import com.devoxx.model.WearSpeaker;
import com.devoxx.util.WearableConstants;
import com.gluonhq.charm.down.plugins.WearableService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.image.Image;
import javafxports.android.AmbientHandler;
import javafxports.android.FXDalvikEntity;
import javafxports.android.FXWearableActivity;

public class AndroidWearableService implements WearableService, GoogleApiClient.ConnectionCallbacks, DataApi.DataListener {

    private static final Logger LOG = Logger.getLogger(AndroidWearableService.class.getName());

    private GoogleApiClient mApiClient;
    private Consumer dataHandler;

    private final ReadOnlyBooleanWrapper error = new ReadOnlyBooleanWrapper();
    
    @Override
    public void connect() {
        disconnect();
        mApiClient = new GoogleApiClient.Builder(FXWearableActivity.getInstance())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        LOG.log(Level.INFO, "Wearable Service disconnected");
        mApiClient.connect();
    }

    @Override
    public <T> void sendMessage(String path, String message, Consumer<T> dataHandler) {
        
        this.dataHandler = dataHandler;
        error.set(false);
                
        if (mApiClient == null || !mApiClient.isConnected()) {
            connect();
        }
        
        new Thread(() -> {
            // broadcast the message to all connected devices
            final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
            if (nodes.getNodes().isEmpty()) {
                LOG.log(Level.WARNING, "Wearable Service error: No nodes found");
                error.set(true);
            } else {
                LOG.log(Level.INFO, String.format("Sending message to handheld: %s %s", path, message));
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(mApiClient, node.getId(), path, message.getBytes()).await();
                }
            }
        }).start();
    }
    
    @Override
    public void disconnect() {
        LOG.log(Level.INFO, "Wearable Service disconnected");
        if (mApiClient != null && mApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mApiClient, this);
            mApiClient.disconnect();
        }
    }
    
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        error.set(false);
        for (DataEvent event : dataEventBuffer) {
            if (event != null && event.getType() == DataEvent.TYPE_CHANGED) {
            
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                if (dataMapItem == null || dataMapItem.getDataMap() == null) {
                    return;
                }
                
                final String eventPath = event.getDataItem().getUri().getPath();
                LOG.log(Level.INFO, String.format("Wearable Service Data from event type %s: %s", event.getType(), eventPath));

                // Check if we have received the conference list
                if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.CONFERENCES_PATH)) {
             
                    List<DataMap> conferencesDataMap = dataMapItem.getDataMap().getDataMapArrayList(WearableConstants.LIST_PATH);
                    if (conferencesDataMap == null) {
                        return;
                    }

                    // fetch the conferences
                    List<Conference> conferencesList = new ArrayList<>();

                    for (DataMap conferenceDataMap : conferencesDataMap) {
                        Conference conference = new Conference();
                        conference.setCountry(conferenceDataMap.getString(WearableConstants.DATAMAP_COUNTRY, ""));
                        conference.setId(conferenceDataMap.getString(WearableConstants.DATAMAP_COUNTRY_ID, ""));
                        conference.setSelected(conferenceDataMap.getBoolean(WearableConstants.DATAMAP_COUNTRY_SELECTED, false));

                        conference.setTimezone(conferenceDataMap.getString(WearableConstants.DATAMAP_COUNTRY_TIMEZONE, ""));
                        conference.setFromDate(conferenceDataMap.getString(WearableConstants.DATAMAP_COUNTRY_FROM_DAY, ""));
                        conference.setToDate(conferenceDataMap.getString(WearableConstants.DATAMAP_COUNTRY_TO_DAY, ""));

                        conferencesList.add(conference);
                    }

                    // send data back, update UI
                    if (dataHandler != null) {
                        Platform.runLater(() -> dataHandler.accept(conferencesList));
                    }
                } else if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSIONS_PATH)) {

                    List<DataMap> sessionsDataMap = dataMapItem.getDataMap().getDataMapArrayList(WearableConstants.LIST_PATH);
                    if (sessionsDataMap == null) {
                        return;
                    }

                    // fetch the sessions
                    List<Session> sessionsList = new ArrayList<>();

                    for (DataMap sessionDataMap : sessionsDataMap) {
                        Session session = new Session();
                        session.setRoomName(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_ROOM, ""));
                        session.setSlotId(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_SLOT_ID, ""));
                        session.setDay(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_DAY, ""));
                        Talk talk = new Talk();
                        talk.setTitle(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_TITLE, ""));
                        talk.setSummary(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_SUMMARY, ""));
                        session.setTalk(talk);
                        session.setFromTime(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_FROM_TIME, ""));
                        session.setToTime(sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_TO_TIME, ""));

                        sessionsList.add(session);
                    }

                    // send data back, update UI
                    if (dataHandler != null) {
                        Platform.runLater(() -> dataHandler.accept(sessionsList));
                    }
                } else if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_FAV_PATH)) {
                    DataMap dataMap = dataMapItem.getDataMap();
                    if (dataMap == null) {
                        return;
                    }

                    DataMap sessionDataMap = dataMapItem.getDataMap().getDataMap(WearableConstants.OBJECT_PATH);
                    final boolean no_auth = sessionDataMap.getBoolean(WearableConstants.DATAMAP_NO_AUTH);
                    final boolean is_fav = sessionDataMap.getBoolean(WearableConstants.DATAMAP_SESSION_IS_FAV);
                    boolean[] result = {no_auth, is_fav};

                    if (dataHandler != null) {
                        Platform.runLater(() -> dataHandler.accept(result));
                    }

                } else if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SPEAKERS_PATH)) {
                    List<DataMap> speakersDataMap = dataMapItem.getDataMap().getDataMapArrayList(WearableConstants.LIST_PATH);
                    if (speakersDataMap == null) {
                        return;
                    }

                    // fetch the sessions
                    List<WearSpeaker> speakersList = new ArrayList<>();

                    for (DataMap speakerDataMap : speakersDataMap) {
                        WearSpeaker speaker = new WearSpeaker();
                        Speaker s = new Speaker();
                        s.setFirstName(speakerDataMap.getString(WearableConstants.DATAMAP_SPEAKER_FIRST_NAME, ""));
                        s.setLastName(speakerDataMap.getString(WearableConstants.DATAMAP_SPEAKER_LAST_NAME, ""));
                        s.setTwitter(speakerDataMap.getString(WearableConstants.DATAMAP_SPEAKER_TWITTER, ""));
                        speaker.setSpeaker(s);

                        final String encoded = speakerDataMap.getString(WearableConstants.DATAMAP_SPEAKER_IMAGE);
                        if (encoded != null && !encoded.isEmpty()) {
                            byte[] imageBytes = Base64.decode(encoded, Base64.DEFAULT);

                            if (imageBytes != null) {
                                speaker.setImage(new Image(new ByteArrayInputStream(imageBytes)));
                            }
                        }
                        speakersList.add(speaker);
                    }

                    // send data back, update UI
                    if (dataHandler != null) {
                        Platform.runLater(() -> dataHandler.accept(speakersList));
                    }
                } else if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SUMMARY_PATH)) {
                    DataMap dataMap = dataMapItem.getDataMap();
                    if (dataMap == null) {
                        return;
                    }

                    DataMap sessionDataMap = dataMapItem.getDataMap().getDataMap(WearableConstants.OBJECT_PATH);
                    final String summary = sessionDataMap.getString(WearableConstants.DATAMAP_SESSION_SUMMARY, "");

                    if (dataHandler != null) {
                        Platform.runLater(() -> dataHandler.accept(summary));
                    }
                } else if (eventPath.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.ERROR_PATH)) {
                    DataMap dataMap = dataMapItem.getDataMap();
                    if (dataMap == null) {
                        return;
                    }
                    LOG.log(Level.INFO, String.format("Wearable Service error: ", dataMap.toBundle().toString()));
                    error.set(true);
                    break;
                }
            }
        }
    }

    @Override
    public boolean isAmbientMode() {
        return FXWearableActivity.getInstance().isAmbientMode();
    }

    @Override
    public void setAmbientHandler(Runnable onEnterAmbientHandler, Runnable onUpdateambientHandler, Runnable onExitAmbientHandler) {
        FXWearableActivity.getInstance().setAmbientHandler(new AmbientHandler() {
            @Override
            public void enterAmbient(Bundle bundle) {
                if (onEnterAmbientHandler != null) {
                    onEnterAmbientHandler.run();
                }
            }

            @Override
            public void updateAmbient() {
                if (onUpdateambientHandler != null) {
                    onUpdateambientHandler.run();
                }
            }
            
            @Override
            public void exitAmbient() {
                if (onExitAmbientHandler != null) {
                    onExitAmbientHandler.run();
                }
            }
        });
    }

    @Override
    public void keepScreenOn(boolean keep) {
        FXDalvikEntity.getActivity().runOnUiThread(() -> {
            Window window = FXDalvikEntity.getActivity().getWindow();
            if (keep) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }
    
    @Override
    public ReadOnlyBooleanProperty errorProperty() {
        return error.getReadOnlyProperty();
    }
    
}
