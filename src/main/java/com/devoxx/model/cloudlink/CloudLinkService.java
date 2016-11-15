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
package com.devoxx.model.cloudlink;

import com.devoxx.util.DevoxxLogging;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.converter.JsonIterableInputConverter;
import com.gluonhq.connect.gluoncloud.AuthenticationMode;
import com.gluonhq.connect.gluoncloud.GluonClient;
import com.gluonhq.connect.gluoncloud.GluonClientBuilder;
import com.gluonhq.connect.gluoncloud.GluonCredentials;
import com.gluonhq.connect.gluoncloud.OperationMode;
import com.gluonhq.connect.gluoncloud.SyncFlag;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.InputStreamListDataReader;
import com.gluonhq.connect.source.BasicInputDataSource;
import com.devoxx.model.BaseService;
import com.devoxx.model.Exhibitor;
import com.devoxx.model.Identifiable;
import com.devoxx.model.Mergeable;
import com.devoxx.model.News;
import com.devoxx.model.Note;
import com.devoxx.model.PushNotification;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.model.Sponsor;
import com.devoxx.model.Venue;
import com.devoxx.model.Vote;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import javax.json.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.InvalidationListener;

public class CloudLinkService extends BaseService {

    private static final Logger LOG = Logger.getLogger(CloudLinkService.class.getName());

    private static final long SYNC_INTERVAL = 1000 * 60 * 30;   // 30 minutes

    private final GluonClient localGluonClient;
    private final GluonClient cloudGluonClient;
    private final Map<String, Long> entitiesLastTimeSynced = new HashMap<>();

    // system wide data
    private ReadOnlyListWrapper<Exhibitor> exhibitors;
    private ReadOnlyListWrapper<Session> sessions;
    private ReadOnlyListWrapper<Speaker> speakers;
    private ReadOnlyListWrapper<Sponsor> sponsors;
    private ReadOnlyListWrapper<Venue> venues;
    private ReadOnlyListWrapper<News> news;
    private PushNotification pushNotification;

    private GluonView authenticationView;

    public CloudLinkService() {
        GluonCredentials gluonCredentials = new GluonCredentials(CloudLinkService.class.getResourceAsStream("/gluoncloudlink_config.json"));

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

   //     loadData();
    }

    @Override
    public GluonView getAuthenticationView() {
        if (authenticationView == null) {
            authenticationView = new GluonView(CloudLinkAuthenticationPresenter.class);
        }
        return authenticationView;
    }

    @Override
    public ReadOnlyListProperty<News> retrieveNews() {
        if (news == null) {
            GluonObservableList<News> gluonNews = DataProvider.retrieveList(cloudGluonClient.createListDataReader("activityFeed", News.class, SyncFlag.LIST_READ_THROUGH));
            SortedList<News> sortedNews = new SortedList<>(gluonNews);
            sortedNews.setComparator((n1, n2) -> n1.getCreationDate() == n2.getCreationDate() ? n1.getUuid().compareTo(n2.getUuid()) : Long.compare(n1.getCreationDate(), n2.getCreationDate()) * -1);
            news = new ReadOnlyListWrapper<>(sortedNews);
        }
        return news.getReadOnlyProperty();
    }

    @Override
    public PushNotification retrievePushNotification() {
        if (pushNotification == null) {
            pushNotification = new PushNotification();
            GluonObservableObject<PushNotification> gluonPushNotification = DataProvider.retrieveObject(cloudGluonClient.createObjectDataReader("pushNotification", PushNotification.class, SyncFlag.OBJECT_READ_THROUGH));
            gluonPushNotification.initializedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    PushNotification finalPushNotification = gluonPushNotification.get();
                    // start listening for changes
                    finalPushNotification.idProperty().addListener((obs2, ov2, nv2) -> {
                        pushNotification.idProperty().setValue(finalPushNotification.getId());
                        pushNotification.titleProperty().setValue(finalPushNotification.getTitle());
                        pushNotification.bodyProperty().setValue(finalPushNotification.getBody());
                    });
                }
            });
        }
        return pushNotification;
    }

    @Override
    public ReadOnlyListProperty<Session> retrieveSessions() {
        return sessions.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyListProperty<Speaker> retrieveSpeakers() {
        return speakers.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return exhibitors.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyListProperty<Sponsor> retrieveSponsors() {
        return sponsors.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyListProperty<Venue> retrieveVenues() {
        return venues.getReadOnlyProperty();
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
        return internalRetrieveData("notes", Note.class);
    }

    @Override
    public ObservableList<Vote> internalRetrieveVotes() {
        return internalRetrieveData("votes", Vote.class);
    }

    private <E extends Identifiable> ObservableList<E> internalRetrieveData(String listIdentifier, Class<E> targetClass) {
        GluonObservableList<E> internalDataLocal = DataProvider.retrieveList(localGluonClient.createListDataReader(getAuthenticatedUserId() + "_" + listIdentifier, targetClass,
                SyncFlag.LIST_READ_THROUGH, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_READ_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        internalDataLocal.initializedProperty().addListener((obsLocal, ovLocal, nvLocal) -> {
            if (nvLocal) {
                if (!listIdentifier.equals("notes") || DevoxxSettings.USE_REMOTE_NOTES) {
                    GluonObservableList<E> internalDataCloud = DataProvider.retrieveList(cloudGluonClient.createListDataReader(getAuthenticatedUserId() + "_" + listIdentifier, targetClass,
                            SyncFlag.LIST_READ_THROUGH, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_READ_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
                    internalDataCloud.initializedProperty().addListener((obsCloud, ovCloud, nvCloud) -> {
                        if (nvCloud) {
                            for (E dataCloud : internalDataCloud) {
                                boolean existsLocally = false;
                                for (E dataLocal : internalDataLocal) {
                                    if (dataLocal.getUuid().equals(dataCloud.getUuid())) {
                                        existsLocally = true;
                                        break;
                                    }
                                }
                                if (!existsLocally) {
                                    internalDataLocal.add(dataCloud);
                                }
                            }

                            for (Iterator<E> dataLocalIter = internalDataLocal.iterator(); dataLocalIter.hasNext();) {
                                boolean existsCloud = false;
                                E dataLocal = dataLocalIter.next();
                                for (E dataCloud : internalDataCloud) {
                                    if (dataCloud.getUuid().equals(dataLocal.getUuid())) {
                                        existsCloud = true;
                                        break;
                                    }
                                }
                                if (!existsCloud) {
                                    dataLocalIter.remove();
                                }
                            }

                            Bindings.bindContent(internalDataCloud, internalDataLocal);
                        }
                    });
                }
            }
        });
        return internalDataLocal;
    }

//
//    private void loadData() {
//        GluonObservableList<Exhibitor> localExhibitors = loadEntities("exhibitors", Exhibitor.class, ServiceUtils::mapJsonToExhibitor);
//        exhibitors = new ReadOnlyListWrapper<>(localExhibitors);
//        GluonObservableList<Session> localSessions = loadEntities("sessions", Session.class, ServiceUtils::mapJsonToSession);
//        sessions = new ReadOnlyListWrapper<>(localSessions);
//        localSessions.initializedProperty().addListener(new InvalidationListener() {
//            @Override
//            public void invalidated(javafx.beans.Observable observable) {
//                if (localSessions.isInitialized()) {
//                    // now that we have started retrieving the list of sessions we can
//                    // call retrieveAuthenticatedUser as sessions won't be empty
//                    localSessions.initializedProperty().removeListener(this);
//                    retrieveAuthenticatedUser();
//                }
//            }
//        });
//
//        GluonObservableList<Speaker> localSpeakers = loadEntities("speakers", Speaker.class, ServiceUtils::mapJsonToSpeaker);
//        speakers = new ReadOnlyListWrapper<>(localSpeakers);
//        GluonObservableList<Sponsor> localSponsors = loadEntities("sponsors", Sponsor.class, ServiceUtils::mapJsonToSponsor);
//        sponsors = new ReadOnlyListWrapper<>(localSponsors);
//        GluonObservableList<Venue> localVenues = loadEntities("venues", Venue.class, ServiceUtils::mapJsonToVenue);
//        venues = new ReadOnlyListWrapper<>(localVenues);
//    }

    private <E extends Mergeable<E>> GluonObservableList<E> loadEntities(String identifier, Class<E> targetClass, Function<JsonObject, E> mapper) {
        GluonObservableList<E> local = DataProvider.retrieveList(localGluonClient.createListDataReader(identifier, targetClass, new SyncFlag[]{}));
        local.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv && local.isEmpty()) {
                loadLocally(identifier, targetClass, local, mapper);
            } else if (nv) {
                loadCloudLink(identifier, targetClass, local);
            }
        });
        local.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.WARNING, "Failed to load " + identifier + " from local device, reloading from classpath resource.", local.getException());
                }
                if (rootDir != null) {
                    File entitiesFile = new File(rootDir, identifier);
                    if (entitiesFile.delete()) {
                        loadLocally(identifier, targetClass, local, mapper);
                    } else {
                        if (DevoxxLogging.LOGGING_ENABLED) {
                            LOG.log(Level.WARNING, "Failed to delete local file for " + identifier + ".");
                        }
                    }
                }
            }
        });
        return local;
    }

    private <E extends Mergeable<E>> void loadLocally(String identifier, Class<E> targetClass, GluonObservableList<E> local, Function<JsonObject, E> mapper) {
        BasicInputDataSource source = new BasicInputDataSource(CloudLinkService.class.getResourceAsStream("/data/" + identifier + ".json"));
        JsonIterableInputConverter<JsonObject> converter = new JsonIterableInputConverter<>(JsonObject.class);

        GluonObservableList<JsonObject> resource = DataProvider.retrieveList(new InputStreamListDataReader<>(source, converter));
        resource.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                for (JsonObject jsonObject : resource) {
                    local.add(mapper.apply(jsonObject));
                }
                localGluonClient.push(local, false);

                loadCloudLink(identifier, targetClass, local);
            }
        });
    }

    private <E extends Mergeable<E>> void loadCloudLink(String identifier, Class<E> targetClass, GluonObservableList<E> local) {
        long now = System.currentTimeMillis();
        Long entityLastTimeSynced = entitiesLastTimeSynced.get(identifier);
        if (entityLastTimeSynced == null) {
            entityLastTimeSynced = 0L;
        }

        if (entityLastTimeSynced + SYNC_INTERVAL < now) {
            entitiesLastTimeSynced.put(identifier, now);
            GluonObservableList<E> cloudlink = DataProvider.retrieveList(cloudGluonClient.createListDataReader(identifier, targetClass, new SyncFlag[]{}));
            cloudlink.stateProperty().addListener((obs, ov, nv) -> {
                if (nv == ConnectState.SUCCEEDED) {
                    // when the list returned from gluon cloudlink is empty, just keep the current local list,
                    // because otherwise we would have no data
                    if (!cloudlink.isEmpty()) {
                        for (E cloudEntity : cloudlink) {
                            boolean entityExistsLocally = false;
                            for (E localEntity : local) {
                                if (localEntity.getUuid().equals(cloudEntity.getUuid())) {
                                    if (localEntity.merge(cloudEntity)) {
                                        local.set(local.indexOf(localEntity), localEntity);
                                    }
                                    entityExistsLocally = true;
                                    break;
                                }
                            }
                            if (!entityExistsLocally) {
                                local.add(cloudEntity);
                            }
                        }
                        for (Iterator<E> localEntityIter = local.iterator(); localEntityIter.hasNext();) {
                            E localEntity = localEntityIter.next();
                            boolean entityExistsRemote = false;
                            for (E cloudEntity : cloudlink) {
                                if (cloudEntity.getUuid().equals(localEntity.getUuid())) {
                                    entityExistsRemote = true;
                                    break;
                                }
                            }
                            if (!entityExistsRemote) {
                                localEntityIter.remove();
                            }
                        }

                        localGluonClient.push(local, false);
                    }
                }
            });
        }
    }

    @Override
    public ReadOnlyObjectProperty<Speaker> retrieveSpeaker(String uuid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
