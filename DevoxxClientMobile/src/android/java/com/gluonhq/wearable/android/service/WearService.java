/**
 * Copyright (c) 2017 Gluon Software
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
package com.gluonhq.wearable.android.service;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import com.devoxx.model.Conference;
import com.devoxx.model.Link;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.model.TalkSpeaker;
import com.devoxx.service.DevoxxService;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.connect.GluonObservableList;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.sun.javafx.application.PlatformImpl;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.util.DevoxxSettings.TWITTER_URL;

public class WearService extends WearableListenerService {
    
    private static final Logger LOG = Logger.getLogger(WearService.class.getName());

    private GoogleApiClient mApiClient;
    
    private Service service;
    
    @Override
    public void onCreate() {
        super.onCreate(); 
        
        // from javafx.platform.properties
        System.setProperty("javafx.platform", "android");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("embedded", "monocle");
        // Headless toolkit -- it doesn't support anything UI-related
        System.setProperty("javafx.toolkit", "com.sun.javafx.tk.HeadlessToolkit");
        // from custom.properties
        System.setProperty("enable.logging", "true");

        // launch toolkit
        PlatformImpl.startup(() -> LOG.log(Level.INFO, "HeadlessToolkit started"));

        service = new DevoxxService();
        
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        LOG.log(Level.INFO, "Wearable Service connected");
        mApiClient.connect();
    }
    
    @Override
    public void onDestroy() {
        LOG.log(Level.INFO, "Wearable Service disconnected");
        if (mApiClient != null && mApiClient.isConnected()) {
            mApiClient.disconnect();
        }
        super.onDestroy();
    }
    
    private boolean connectWearApi(final Runnable onConnectedAction) {

        if (service == null) {
            service = new DevoxxService();
        }
        
        if (mApiClient != null && mApiClient.isConnected()) {
            onConnectedAction.run();
        } else {
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            onConnectedAction.run();
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {

                        }
                    }).build();
            mApiClient.connect();
        }
        return true;
    }
    
    private void sendMessage(final PutDataMapRequest putDataMapRequest) {
        LOG.log(Level.INFO, "Wearable Service: Sending message");
        int count = 0, max = 5;
        while (count < max && !connectWearApi(() -> 
                Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest()))) {
            count++;
            try {
                Thread.sleep(2000);
                LOG.log(Level.WARNING, String.format("Wearable Service: Error sending message [%d/%d]", count, max));
            } catch (InterruptedException ie) {
                //no-op
            }
        }
    }
    
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();
        final String data = new String(messageEvent.getData());

        LOG.log(Level.INFO, String.format("Wearable Service: Message received : %s - %s", path, data));
        if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.CONFERENCES_PATH)) {
            // send conferences to the Wearable
            sendConferences();
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSIONS_PATH)) {
            // send sessions to the Wearable
            if (service.retrieveSessions().size() > 0) {
                sendSessions(Integer.parseInt(data));
            } else {
                service.retrieveSessions().addListener((obs, ov, nv) -> {
                    if (nv != null && nv.size() > 0) {
                        sendSessions(Integer.parseInt(data));
                    }
                });
            }
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SET_FAV_PATH)) {
            setFavSession(path.substring((WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SET_FAV_PATH).length() + 1), 
                    Boolean.parseBoolean(data));
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_FAV_PATH)) {
            sendFavSession(data);
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SPEAKERS_PATH)) {
            sendSpeakers(data);
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.TWITTER_PATH)) {
            followOnTwitter(data);
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SUMMARY_PATH)) {
            sendSummary(data);
        } else if (path.startsWith(WearableConstants.CHANNEL_ID + WearableConstants.OPEN_MOBILE_PATH)) {
            openMobile(data);
        } 
    }
    
    private void sendConferences() {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.CONFERENCES_PATH);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);
        
        if (service == null) {
            sendError("No service. Try again");
            return;
        }
        
        final GluonObservableList<Conference> retrieveConferences = service.retrieveConferences();
        if (retrieveConferences.isInitialized()) {
            // store the list schedules
            putDataMapRequest.getDataMap().putDataMapArrayList(WearableConstants.LIST_PATH, conferencesMap());
            sendMessage(putDataMapRequest);
        } else {
            retrieveConferences.setOnSucceeded(e -> {
                // store the list schedules
                putDataMapRequest.getDataMap().putDataMapArrayList(WearableConstants.LIST_PATH, conferencesMap());
                sendMessage(putDataMapRequest);
            });
        }
    }

    private String configuredConference;
    private ArrayList<DataMap> conferencesMap() {
        // Prepare and save the schedule
        ArrayList<DataMap> conferencesDataMap = new ArrayList<>();
        
        if (service.getConference() == null || service.getConference().getId() == null || service.getConference().getId().isEmpty()) {
            Services.get(SettingsService.class).ifPresent(settingsService ->
                configuredConference = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID)
            );
        } else {
            configuredConference = service.getConference().getId();
        }
        
        for (Conference conference : service.retrieveConferences()) {
            
            final DataMap conferenceDataMap = new DataMap();
            
            // process and push conference's data
            conferenceDataMap.putString(WearableConstants.DATAMAP_COUNTRY, conference.getCountry());
            conferenceDataMap.putString(WearableConstants.DATAMAP_COUNTRY_ID, conference.getId());
            conferenceDataMap.putBoolean(WearableConstants.DATAMAP_COUNTRY_SELECTED,
                    configuredConference != null && conference.getId().equals(configuredConference));
            
            conferenceDataMap.putString(WearableConstants.DATAMAP_COUNTRY_TIMEZONE, conference.getTimezone());
            conferenceDataMap.putString(WearableConstants.DATAMAP_COUNTRY_FROM_DAY, conference.getFromDate());
            conferenceDataMap.putString(WearableConstants.DATAMAP_COUNTRY_TO_DAY, conference.getToDate());
            
            conferencesDataMap.add(conferenceDataMap);
        }
        
        LOG.log(Level.INFO, String.format("Wearable Service: Adding %d conferences, selected: %s", conferencesDataMap.size(), configuredConference));
        return conferencesDataMap;
    }
    
    private void sendSessions(int day) {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.SESSIONS_PATH + "/" + day);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);
        
        // Prepare and save the schedule
        ArrayList<DataMap> sessionsDataMap = new ArrayList<>();
        
        if (service == null) {
            sendError("No service. Try again");
            return;
        }
        
        Conference conference = service.getConference();
        
        for (Session session : service.retrieveSessions()) {

                final DataMap sessionDataMap = new DataMap();

                if (conference.getConferenceDayIndex(session.getStartDate()) == day) {
                    // process and push session's data
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_DAY, session.getDay());
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_ROOM, session.getRoomName());
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_SLOT_ID, session.getSlotId());
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_TITLE, session.getTitle());
                    //sessionDataMap.putString(DevoxxSettings.DATAMAP_SESSION_SUMMARY, session.getSummary());
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_FROM_TIME, session.getFromTime());
                    sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_TO_TIME, session.getToTime());

                    sessionsDataMap.add(sessionDataMap);
                }
        }

        // store the list sessions
        LOG.log(Level.INFO, String.format("Wearable Service: Adding %d sessions", sessionsDataMap.size()));
        putDataMapRequest.getDataMap().putDataMapArrayList(WearableConstants.LIST_PATH, sessionsDataMap);
        
        sendMessage(putDataMapRequest);
    }
    
    private void setFavSession(String sessionSlotId, boolean fav) {
        if (service.readyProperty().get()) {
            doSetFavSession(sessionSlotId, fav);
        } else {
            service.readyProperty().addListener((obs, ov, nv) -> doSetFavSession(sessionSlotId, fav));
        }
    }

    private void doSetFavSession(String sessionSlotId, boolean fav) {
        if (service.isAuthenticated()) {
            for (Session session : service.retrieveSessions()) {
                if (session.getSlotId().equals(sessionSlotId)) {
                    if (fav) {
                        LOG.log(Level.INFO, String.format("Wearable Service: Added favored: %s", session.getTitle()));
                        service.retrieveFavoredSessions().add(session);
                    } else {
                        LOG.log(Level.INFO, String.format("Wearable Service: Removed favored: %s", session.getTitle()));
                        service.retrieveFavoredSessions().remove(session);
                    }
                    sendFavSession(sessionSlotId);
                    break;
                }
            }
        }
    }
    
    private void sendFavSession(String sessionSlotId) {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_FAV_PATH + "/" + sessionSlotId);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);
        
        if (service.readyProperty().get()) {
            putDataMapRequest.getDataMap().putDataMap(WearableConstants.OBJECT_PATH, getFavSession(sessionSlotId));
            sendMessage(putDataMapRequest);
        } else {
            service.readyProperty().addListener((obs, ov, nv) -> {
                putDataMapRequest.getDataMap().putDataMap(WearableConstants.OBJECT_PATH, getFavSession(sessionSlotId));
                sendMessage(putDataMapRequest);
            });
        }
    }

    private DataMap getFavSession(String sessionSlotId) {
        final DataMap sessionDataMap = new DataMap();
        if (service.isAuthenticated()) {
            sessionDataMap.putBoolean(WearableConstants.DATAMAP_NO_AUTH, false);
            
            ObservableList<Session> retrieveFavoredSessions = service.retrieveFavoredSessions();
            boolean found = false;
            for (Session s : retrieveFavoredSessions) {
                if (s.getSlotId().equalsIgnoreCase(sessionSlotId)) {
                    found = true;
                    break;
                }
            }
            sessionDataMap.putBoolean(WearableConstants.DATAMAP_SESSION_IS_FAV, found);
            
        } else {
            sessionDataMap.putBoolean(WearableConstants.DATAMAP_NO_AUTH, true);
            sessionDataMap.putBoolean(WearableConstants.DATAMAP_SESSION_IS_FAV, false);
        }
        return sessionDataMap;
    }
    
    private void sendSpeakers(String sessionSlotId) {
        if (service == null) {
            sendError("No service. Try again");
            return;
        }
       
        if (service.readyProperty().get()) {
            getSpeakers(sessionSlotId);
        } else {
            service.readyProperty().addListener((obs, ov, nv) -> getSpeakers(sessionSlotId));
        }
        
    }
    
    private void getSpeakers(String sessionSlotId) {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.SPEAKERS_PATH + "/" + sessionSlotId);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);

        // Prepare and save the schedule
        ArrayList<DataMap> speakersDataMap = new ArrayList<>();
        ArrayList<String> speakersUUID = new ArrayList<>();
         
        for (Session session : service.retrieveSessions()) {
                
            if (session.getSlotId().equals(sessionSlotId)) {
                
                LOG.log(Level.INFO, String.format("Wearable Service: Session has %d speakers", session.getTalk().getSpeakers().size()));
                for (TalkSpeaker talkSpeaker : session.getTalk().getSpeakers()) {
                    Link link = talkSpeaker.getLink();
                    if (link != null && link.getHref() != null && !link.getHref().isEmpty()) {
                        String speakerUUID = link.getHref().substring(link.getHref().lastIndexOf('/') + 1);
                        speakersUUID.add(speakerUUID);
                    }
                }
                break;
            }
        }
        
        CountDownLatch fillSpeakersCountDownLatch = new CountDownLatch(speakersUUID.size());
        final AtomicInteger processed = new AtomicInteger(0);
                
        new Thread(() -> {
            try {
                boolean ok = fillSpeakersCountDownLatch.await(10, TimeUnit.SECONDS);
                if (ok && processed.get() == speakersUUID.size()) {
                    putDataMapRequest.getDataMap().putDataMapArrayList(WearableConstants.LIST_PATH, speakersDataMap);
                    sendMessage(putDataMapRequest);
                } else {
                    LOG.log(Level.WARNING, "There was an error retrieving speakers");
                    sendError("There was an error retrieving speakers. Try again");
                }
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, "Error retrieving speakers", ex);
                sendError("Error retrieving speakers" + ex.getMessage() + ". Try again");
            }
        }).start();
        
        for (String uuid : speakersUUID) {
            new Thread(() -> {
                ReadOnlyObjectProperty<Speaker> speaker = service.retrieveSpeaker(uuid);
                if (speaker.get() != null) {
                    speakersDataMap.add(getSpeaker(speaker.get()));
                    processed.getAndIncrement();
                    fillSpeakersCountDownLatch.countDown();
                } else {
                    speaker.addListener((obs, ov, nv) -> {
                        speakersDataMap.add(getSpeaker(speaker.get()));
                        processed.getAndIncrement();
                        fillSpeakersCountDownLatch.countDown();
                    });
                }
            }).start();
        }
    }

    private DataMap getSpeaker(Speaker speaker) {
        final DataMap speakerDataMap = new DataMap();

    // process and push speaker's data
        speakerDataMap.putString(WearableConstants.DATAMAP_SPEAKER_FIRST_NAME, speaker.getFirstName());
        speakerDataMap.putString(WearableConstants.DATAMAP_SPEAKER_LAST_NAME, speaker.getLastName());
        speakerDataMap.putString(WearableConstants.DATAMAP_SPEAKER_TWITTER, speaker.getTwitter());
        
        String path = speaker.getPicture();
        Bitmap bitmap = decodeAndScaleInputStream(path);
//            Asset asset = createAssetFromBitmap(bitmap);
//            speakerDataMap.putAsset(WearableConstants.DATAMAP_SPEAKER_IMAGE, asset);
        if (bitmap == null) {
            bitmap = decodeAndScaleInputStream("");
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        speakerDataMap.putString(WearableConstants.DATAMAP_SPEAKER_IMAGE, encoded);
        return speakerDataMap;
    }
    
    // Decodes image and scales it to reduce memory consumption
    private Bitmap decodeAndScaleInputStream(String path) {
        try {
            InputStream is;
            if (path == null || path.isEmpty()) {
                is = Util.class.getResourceAsStream("speaker.jpeg");
            } else {
                is = new URL(path).openStream();
            }
            
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);
            // The new size we want to scale to
            final int IMAGE_MAX_SIZE = 96;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE / 
                   (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            InputStream is2;
            if (path == null || path.isEmpty()) {
                is2 = Util.class.getResourceAsStream("speaker.jpeg");
            } else {
                is2 = new URL(path).openStream();
            }
            return BitmapFactory.decodeStream(is2, null, o2);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Wearable Service: Error in decodeAndScaleInputStream", e);
        }
        return null;
    }
    
    private void sendSummary(String sessionSlotId) {
        
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SUMMARY_PATH + "/" + sessionSlotId);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);
        
        if (service.readyProperty().get()) {
            putDataMapRequest.getDataMap().putDataMap(WearableConstants.OBJECT_PATH, getSessionSummary(sessionSlotId));
            sendMessage(putDataMapRequest);
        } else {
            service.readyProperty().addListener((obs, ov, nv) -> {
                putDataMapRequest.getDataMap().putDataMap(WearableConstants.OBJECT_PATH, getSessionSummary(sessionSlotId));
                sendMessage(putDataMapRequest);
            });
        }
    }
    
    private DataMap getSessionSummary(String sessionSlotId) {
        final DataMap sessionDataMap = new DataMap();
        for (Session session : service.retrieveSessions()) {
            if (session.getSlotId().equals(sessionSlotId)) {
                sessionDataMap.putString(WearableConstants.DATAMAP_SESSION_SUMMARY, session.getSummary());
                break;
            }
        }
        return sessionDataMap;
    }
    
    private void sendError(String message) {
        final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WearableConstants.CHANNEL_ID + WearableConstants.ERROR_PATH);
        
        // set the header (timestamp is used to force a onDataChanged event on the wearable)
        final DataMap headerMap = new DataMap();
        headerMap.putString(WearableConstants.DATAMAP_TIMESTAMP, new Date().toString());
        headerMap.putString(WearableConstants.DATAMAP_ERROR, message);
        putDataMapRequest.getDataMap().putDataMap(WearableConstants.HEADER_PATH, headerMap);

        sendMessage(putDataMapRequest);
    }
    
    private void openMobile(String data) {
        LOG.log(Level.INFO, "Wearable Service: Opening application");
        Intent intent = getApplication().getPackageManager().getLaunchIntentForPackage("com.devoxx");
        if (intent == null) {
            return;
        }
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (WearableConstants.DATAMAP_OPEN_MOBILE_AUTH.equals(data)) {
            // open app and trigger authentication view
            Services.get(SettingsService.class).ifPresent(settings -> settings.store(DevoxxSettings.SIGN_UP, Boolean.TRUE.toString()));
        } else if (WearableConstants.DATAMAP_OPEN_MOBILE_SELECT.equals(data)) {
            // nothing to do, app will show the conference selector
        }
        getApplication().startActivity(intent);
    }
    
    private void followOnTwitter(String data) {
        if (data != null && !data.isEmpty() && data.startsWith("@")) {
            Services.get(BrowserService.class).ifPresent(b -> {
                try {
                    String url = TWITTER_URL + data.substring(1);
                    b.launchExternalBrowser(url);
                } catch (IOException | URISyntaxException ex) {
                    //no-op
                }
            });
        }
    }
    
}
