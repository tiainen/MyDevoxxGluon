/**
 * Copyright (c) 2016, Gluon Software
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
package com.devoxx.model;

import com.devoxx.model.cloudlink.CloudLinkService;
import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.gluoncloud.AuthenticationMode;
import com.gluonhq.connect.gluoncloud.GluonClient;
import com.gluonhq.connect.gluoncloud.GluonClientBuilder;
import com.gluonhq.connect.gluoncloud.GluonCredentials;
import com.gluonhq.connect.gluoncloud.OperationMode;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.RestClient;
import java.io.InputStream;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DevoxxService extends BaseService {

    private static final Logger LOG = Logger.getLogger(DevoxxService.class.getName());

    private static final String DEVOXX_HOST = "http://cfp.devoxx.be/api/conferences/DV16";

    private GluonClient localGluonClient;
    private GluonClient cloudGluonClient;

    private BooleanProperty allSessionsAvailable = new SimpleBooleanProperty(false);
    private CountDownLatch fillSessionsCountDownLatch;
    private ReadOnlyListWrapper<Session> sessions;
    private ReadOnlyListWrapper<Speaker> speakers;

    public DevoxxService() {
        InputStream is = CloudLinkService.class.getResourceAsStream("/gluoncloudlink_config.json");
        if (is == null) {
            System.err.println("Can't find a file named gluoncloudlink_config.json on the classpath. This file should be in src/main/resources.");
        } else {
            GluonCredentials gluonCredentials = new GluonCredentials(is);

            localGluonClient = GluonClientBuilder.create()
                .credentials(gluonCredentials)
                .authenticationMode(AuthenticationMode.PUBLIC)
                .operationMode(OperationMode.LOCAL_ONLY)
                .build();

            cloudGluonClient = GluonClientBuilder.create()
                .credentials(gluonCredentials)
                .authenticationMode(AuthenticationMode.PUBLIC)
                .operationMode(OperationMode.CLOUD_FIRST)
                .build();

            sessions = new ReadOnlyListWrapper<>(retrieveSessionsInternal());
            speakers = new ReadOnlyListWrapper<>(retrieveSpeakersInternal());

            allSessionsAvailable.addListener((obs, ov, nv) -> {
                if (nv) {
                    System.out.println("SESSIONS AVAILABLE, LOADING AUTHENTICATION STUFF!");
                    retrieveAuthenticatedUser();
                }
            });
        }
    }

    @Override
    public ReadOnlyListProperty<News> retrieveNews() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Session> retrieveSessions() {
        return sessions.getReadOnlyProperty();
    }

    private ObservableList<Session> retrieveSessionsInternal() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(DEVOXX_HOST)
                .path("/schedules/");
        GluonObservableObject<Schedules> linkOfSessions = DataProvider.retrieveObject(restClient.createObjectDataReader(Schedules.class));
        linkOfSessions.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // define number of links
                List<Link> linksToProcess = new ArrayList<>();
                for (Link link : newValue.getLinks()) {
                    if (link.getHref() != null && !link.getHref().isEmpty()) {
                        linksToProcess.add(link);
                    }
                }

                fillSessionsCountDownLatch = new CountDownLatch(linksToProcess.size());
                Thread thread = new Thread(() -> {
                    try {
                        fillSessionsCountDownLatch.await(2, TimeUnit.MINUTES);
                        allSessionsAvailable.set(true);
                    } catch (InterruptedException e) {
                        LOG.log(Level.INFO, "FillSessions thread was interrupted.", e);
                    }
                }, "FillSessions");
                thread.setDaemon(true);
                thread.start();

                for (Link link : linksToProcess) {
                    fillAllSessionsOfDay(sessions, link);
                }
            }
        });

        return sessions;
    }

    /**
     * Retrieve all sessions for a specific day, and add them to the list of all sessions.
     * Adding sessions happens on the FX App thread, so no concurrency issues are expected.
     * @param allSessions the list of all sessions from all days, passed by the caller.
     * @param link a link containing info about the URL for retrieving the sessions for a specific day
     */
    private void fillAllSessionsOfDay(ObservableList<Session> allSessions, Link link) {
        String day = link.getHref().substring(link.getHref().lastIndexOf('/') + 1);
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(DEVOXX_HOST)
                .path("/schedules/" + day);
        GluonObservableObject<SchedulesOfDay> scheduleOfDay = DataProvider.retrieveObject(restClient.createObjectDataReader(SchedulesOfDay.class));
        scheduleOfDay.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == ConnectState.FAILED || newState == ConnectState.SUCCEEDED) {
                fillSessionsCountDownLatch.countDown();
            }
        });
        scheduleOfDay.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                for (Session session : newValue.getSlots()) {
                    if (session.getTalk() != null) {
                        allSessions.add(session);
                    }
                }
            }
        });
    }

    @Override
    public ReadOnlyListProperty<Speaker> retrieveSpeakers() {
        return speakers;
    }

    private ObservableList<Speaker> retrieveSpeakersInternal() {
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(DEVOXX_HOST)
                .path("/speakers");
        return DataProvider.retrieveList(restClient.createListDataReader(Speaker.class));
    }

    public ReadOnlyObjectProperty<Speaker> retrieveSpeaker(String uuid) {
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(DEVOXX_HOST)
                .path("/speakers/" + uuid);
        return DataProvider.retrieveObject(restClient.createObjectDataReader(Speaker.class));
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Sponsor> retrieveSponsors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ObservableList<Session> internalRetrieveFavoriteSessions() {
        return internalRetrieveSessions("favorite_sessions", null);
    }

    @Override
    public ObservableList<Session> internalRetrieveScheduledSessions(Runnable onStateSucceeded) {
        return internalRetrieveSessions("scheduled_sessions", onStateSucceeded);
    }

    private ObservableList<Session> internalRetrieveSessions(String listIdentifierSuffix, Runnable onStateSucceeded) {
        ObservableList<Session> internalSessions = FXCollections.observableArrayList();
        GluonObservableList<String> internalSessionsLocal = DataProvider.retrieveList(localGluonClient.createListDataReader(getAuthenticatedUserId() + "_" + listIdentifierSuffix, String.class));
        internalSessionsLocal.initializedProperty().addListener((obsLocal, ovLocal, nvLocal) -> {
            if (nvLocal) {
                GluonObservableList<String> internalSessionsCloud = DataProvider.retrieveList(cloudGluonClient.createListDataReader(getAuthenticatedUserId() + "_" + listIdentifierSuffix, String.class));

                // keep reference to the list of sessions that were added while loading the internal list
                // the most likely use case is when a user is not yet logged in and clicks the favorite or
                // schedule button to add a session
                List<String> addedSessionBeforeInitialized = new ArrayList<>();
                for (Session session : internalSessions) {
                    addedSessionBeforeInitialized.add(session.getSlotId());
                }

                for (String uuidLocal : internalSessionsLocal) {
                    findSession(uuidLocal).ifPresent(internalSessions::add);
                }

                internalSessions.addListener(new ListChangeListener<Session>() {
                    @Override
                    public void onChanged(Change<? extends Session> c) {
                        while (c.next()) {
                            if (c.wasRemoved()) {
                                for (Session session : c.getRemoved()) {
                                    internalSessionsLocal.remove(session.getSlotId());
                                    internalSessionsCloud.remove(session.getSlotId());
                                }
                            }
                            if (c.wasAdded()) {
                                for (Session session : c.getAddedSubList()) {
                                    LOG.log(Level.INFO, "Adding Session: " + session.getSlotId() + " / " + session.getTitle());
                                    internalSessionsLocal.add(session.getSlotId());
                                    internalSessionsCloud.add(session.getSlotId());
                                }
                            }
                        }
                    }
                });

                if (onStateSucceeded != null) {
                    internalSessionsCloud.stateProperty().addListener(new InvalidationListener() {
                        @Override
                        public void invalidated(javafx.beans.Observable observable) {
                            if (internalSessionsCloud.getState().equals(ConnectState.SUCCEEDED)) {
                                internalSessionsCloud.stateProperty().removeListener(this);
                                onStateSucceeded.run();
                            }
                        }
                    });
                }

                internalSessionsCloud.initializedProperty().addListener((obsCloud, ovCloud, nvCloud) -> {
                    if (nvCloud) {
                        // add sessions to the local list that exist in cloudlink but not locally
                        List<Session> cloudSessionsToAdd = new ArrayList<>();
                        for (String uuidCloud : internalSessionsCloud) {
                            if (!internalSessionsLocal.contains(uuidCloud)) {
                                findSession(uuidCloud).ifPresent(cloudSessionsToAdd::add);
                            }
                        }
                        internalSessions.addAll(cloudSessionsToAdd);

                        // remove sessions from the local list that exist locally but not in cloudlink
                        List<Session> localSessionsToRemove = new ArrayList<>();
                        for (String uuidLocal : internalSessionsLocal) {
                            if (!internalSessionsCloud.contains(uuidLocal)) {
                                findSession(uuidLocal).ifPresent(localSessionsToRemove::add);
                            }
                        }
                        internalSessions.removeAll(localSessionsToRemove);

                        // add the sessions that were added before initialization was complete to the local
                        // and cloud lists
                        for (String uuidSession : addedSessionBeforeInitialized) {
                            if (!internalSessionsLocal.contains(uuidSession)) {
                                internalSessionsLocal.add(uuidSession);
                            }
                            if (!internalSessionsCloud.contains(uuidSession)) {
                                internalSessionsCloud.add(uuidSession);
                            }
                        }
                    }
                });
            }
        });
        return internalSessions;
    }

    @Override
    public ObservableList<Note> internalRetrieveNotes() {
        return null;
    }

    @Override
    public ObservableList<Vote> internalRetrieveVotes() {
        return null;
    }

    @Override
    public GluonView getAuthenticationView() {
        return null;
    }

    @Override
    public PushNotification retrievePushNotification() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<Venue> retrieveVenues() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

}
