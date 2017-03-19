/**
 * Copyright (c) 2016, 2017, Gluon Software
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
package com.devoxx.service;

import com.airhacks.afterburner.injection.Injector;
import com.devoxx.model.Conference;
import com.devoxx.model.Exhibitor;
import com.devoxx.model.Favored;
import com.devoxx.model.Floor;
import com.devoxx.model.Identifiable;
import com.devoxx.model.Link;
import com.devoxx.model.Note;
import com.devoxx.model.ProposalType;
import com.devoxx.model.ProposalTypes;
import com.devoxx.model.PushNotification;
import com.devoxx.model.Scheduled;
import com.devoxx.model.Schedules;
import com.devoxx.model.SchedulesOfDay;
import com.devoxx.model.Session;
import com.devoxx.model.SessionId;
import com.devoxx.model.Sessions;
import com.devoxx.model.Speaker;
import com.devoxx.model.Speakers;
import com.devoxx.model.Sponsor;
import com.devoxx.model.Track;
import com.devoxx.model.Tracks;
import com.devoxx.model.Vote;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.down.plugins.RuntimeArgsService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.data.DataClient;
import com.gluonhq.cloudlink.client.data.DataClientBuilder;
import com.gluonhq.cloudlink.client.data.OperationMode;
import com.gluonhq.cloudlink.client.data.SyncFlag;
import com.gluonhq.cloudlink.client.user.User;
import com.gluonhq.cloudlink.client.user.UserClient;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.converter.InputStreamIterableInputConverter;
import com.gluonhq.connect.converter.JsonInputConverter;
import com.gluonhq.connect.converter.JsonIterableInputConverter;
import com.gluonhq.connect.converter.JsonOutputConverter;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.FileClient;
import com.gluonhq.connect.provider.InputStreamListDataReader;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import com.gluonhq.connect.source.BasicInputDataSource;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DevoxxService implements Service {

    private static final Logger LOG = Logger.getLogger(DevoxxService.class.getName());

//    private static final String DEVOXX_CFP_DATA_URL = "https://s3-eu-west-1.amazonaws.com/cfpdevoxx/cfp.json";

    private static File rootDir;
    static {
        try {
            rootDir = Services.get(StorageService.class)
                    .flatMap(StorageService::getPrivateStorage)
                    .orElseThrow(() -> new IOException("Private storage file not available"));
            RuntimeArgsService ras = Services.get(RuntimeArgsService.class).get();
            ras.addListener(RuntimeArgsService.LAUNCH_PUSH_NOTIFICATION_KEY, (f) -> {
                System.out.println(">>> received a silent push notification with contents: " + f);
                System.out.println("[DBG] writing reload file");
                File reloadMe = new File (rootDir, "reload");
                    try {
                        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream (reloadMe)));
                        br.write(f);
                        br.close();
                        System.out.println("[DBG] writing reload file done");
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DevoxxService.class.getName()).log(Level.SEVERE, null, ex);
                        System.out.println("[DBG] exception writing reload file "+ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DevoxxService.class.getName()).log(Level.SEVERE, null, ex);
                        System.out.println("[DBG] exception writing reload file "+ex);
                    }
                }
            );
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private GluonObservableList<Conference> conferences;
    private ReadOnlyObjectWrapper<Conference> conference = new ReadOnlyObjectWrapper<>();

    private DataClient localDataClient;
    private UserClient authenticationClient;
    private DataClient cloudDataClient;

    private StringProperty cfpUserUuid = new SimpleStringProperty(this, "cfpUserUuid", "");

    /**
     * The sessions field is crucial. It is returned in the
     * retrieveSessions call that is used by the SessionsPresenter. Hence, the content of the sessions
     * directly reflect to the UI.
     */
    private final ReadOnlyListWrapper<Session> sessions = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSessionsFromCfp = new AtomicBoolean(false);

    private final ReadOnlyListWrapper<Speaker> speakers = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSpeakersFromCfp = new AtomicBoolean(false);

    private ObservableList<Track> internalTracks = null;
    private ReadOnlyListWrapper<Track> tracks = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ObservableList<ProposalType> internalProposalTypes = null;
    private ReadOnlyListWrapper<ProposalType> proposalTypes = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<Floor> exhibitionMaps;

    // user specific data
    private ObservableList<Session> favoredSessions;
    private ObservableList<Session> scheduledSessions;
    private ObservableList<Note> notes;
    private ObservableList<Vote> votes;

    private ObservableList<Session> internalFavoredSessions = FXCollections.observableArrayList();
    private ListChangeListener<Session> internalFavoredSessionsListener = null;
    private ObservableList<Session> internalScheduledSessions = FXCollections.observableArrayList();
    private ListChangeListener<Session> internalScheduledSessionsListener = null;

    public DevoxxService() {
        conferences = retrieveConferencesInternal();

        localDataClient = DataClientBuilder.create()
            .operationMode(OperationMode.LOCAL_ONLY)
            .build();

        authenticationClient = new UserClient();

        cloudDataClient = DataClientBuilder.create()
            .authenticateWith(authenticationClient)
            .operationMode(OperationMode.CLOUD_FIRST)
            .build();

        cfpUserUuid.addListener((obs, ov, nv) -> {
            if ("".equals(nv)) {
                if (internalFavoredSessions != null && internalFavoredSessionsListener != null) {
                    internalFavoredSessions.removeListener(internalFavoredSessionsListener);
                }
                if (internalScheduledSessions != null && internalScheduledSessionsListener != null) {
                    internalScheduledSessions.removeListener(internalScheduledSessionsListener);
                }
            }
        });

        conferenceProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                if (ov != null) {
                    clearCfpAccount();

                    if (authenticationClient.isAuthenticated()) {
                        // Load all authenticated data once user has been authenticated
                        loadCfpAccount(authenticationClient.getAuthenticatedUser(), this::retrieveAuthenticatedUserSessionInformation);
                    }
                }

                retrieveSessionsInternal();

                retrieveSpeakersInternal();

                if (internalTracks != null) {
                    internalTracks.clear();
                    Bindings.unbindContent(tracks, internalTracks);
                }
                internalTracks = retrieveTracksInternal();
                Bindings.bindContent(tracks, internalTracks);

                if (internalProposalTypes != null) {
                    internalProposalTypes.clear();
                    Bindings.unbindContent(proposalTypes, internalProposalTypes);
                }
                internalProposalTypes = retrieveProposalTypesInternal();
                Bindings.bindContent(proposalTypes, internalProposalTypes);

                exhibitionMaps = new ReadOnlyListWrapper<>(retrieveExhibitionMapsInternal());
            }
        });

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            String configuredConference = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (configuredConference != null) {
                conferences.initializedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            findAndSetConference(configuredConference, conferences);
                            conferences.initializedProperty().removeListener(this);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void authenticate(Runnable successRunnable) {
        authenticationClient.authenticate(user -> loadCfpAccount(user, successRunnable));
    }

    @Override
    public void authenticate(Runnable successRunnable, Runnable failureRunnable) {
        authenticationClient.authenticate(user -> loadCfpAccount(user, successRunnable), message -> {
            if (failureRunnable != null) {
                failureRunnable.run();
            }
        });
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationClient.isAuthenticated() && cfpUserUuid.isNotEmpty().get();
    }

    private boolean loggedOut;

    @Override
    public boolean logOut() {
        loggedOut = false;

        Dialog<Button> dialog = new Dialog<>();
        Placeholder logoutDialogContent = new Placeholder(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.TITLE"), DevoxxBundle.getString("OTN.LOGOUT_DIALOG.CONTENT"), MaterialDesignIcon.HELP);

        // FIXME: Too narrow Dialogs in Glisten
        logoutDialogContent.setPrefWidth(MobileApplication.getInstance().getView().getScene().getWidth() - 40);

        dialog.setContent(logoutDialogContent);
        Button yesButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.YES"));
        Button noButton = new Button(DevoxxBundle.getString("OTN.LOGOUT_DIALOG.NO"));
        yesButton.setOnAction(e -> {
            loggedOut = internalLogOut();
            dialog.hide();
        });
        noButton.setOnAction(e -> dialog.hide());
        dialog.getButtons().addAll(noButton, yesButton);

        dialog.showAndWait();

        return loggedOut;
    }

    private boolean internalLogOut() {
        authenticationClient.signOut();
        if (!authenticationClient.isAuthenticated()) {
            clearCfpAccount();
        }
        return true;
    }

    @Override
    public void setConference(Conference conference) {
        this.conference.set(conference);
    }

    @Override
    public Conference getConference() {
        return conference.get();
    }

    @Override
    public ReadOnlyObjectProperty<Conference> conferenceProperty() {
        return conference.getReadOnlyProperty();
    }

    @Override
    public GluonObservableList<Conference> retrieveConferences() {
        return conferences;
    }

    private GluonObservableList<Conference> retrieveConferencesInternal() {
        InputStreamIterableInputConverter<Conference> converter = new JsonIterableInputConverter<>(Conference.class);
        ListDataReader<Conference> listDataReader = new InputStreamListDataReader<>(new BasicInputDataSource(DevoxxService.class.getResourceAsStream("cfp.json")), converter);

        // todo: make it possible to refresh the enclosed json conference data with data loaded from the web??
//        ListDataReader<Conference> listDataReader = RestClient.create()
//                .method("GET")
//                .host(DEVOXX_CFP_DATA_URL)
//                .connectTimeout(30000)
//                .readTimeout(60000)
//                .createListDataReader(converter);

        GluonObservableList<Conference> conferences = DataProvider.retrieveList(listDataReader);
        conferences.exceptionProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                nv.printStackTrace();
            }
        });

        return conferences;
    }

    @Override
    public void checkIfReloadRequested() {
        if (rootDir != null && getConference() != null) {
            File reload = new File(rootDir, "reload");
            LOG.log(Level.INFO, "Reload requested? " + reload.exists());
            if (reload.exists()) {
                String conferenceIdForReload = readConferenceIdFromFile(reload);
                LOG.log(Level.INFO, "Reload requested for conference: " + conferenceIdForReload + ", current conference: " + getConference().getId());
                System.out.println("[DBB] reload exists for conference: '" + conferenceIdForReload + "', current conference: '" + getConference().getId()+"'");
                if (!conferenceIdForReload.isEmpty() && conferenceIdForReload.equals(getConference().getId())) {
                    reload.delete();
                    retrieveSessionsInternalCfp();
                    retrieveSpeakersInternalCfp();
                    System.out.println("[DBB] data reloading for "+conferenceIdForReload);
                }
            }
        }
    }

    @Override
    public Optional<Session> findSession(String uuid) {
        for (Session session : retrieveSessions()) {
            if (session.getTalk() != null && session.getTalk().getId().equals(uuid)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    @Override
    public ReadOnlyListProperty<Session> retrieveSessions() {
        return sessions.getReadOnlyProperty();
    }

    /**
     * Refreshes the global sessions list with the provided sessions list, by setting the provided
     * sessions to the global sessions. Everything that is currently listed in the global sessions
     * list will be cleared.
     *
     * @param newSessions The new list of sessions to be set on the global list of sessions.
     */
    private void loadingOfSessionsFinished(List<Session> newSessions) {
        for (Session session : newSessions) {
            session.setStartDate(Util.timeToZonedDateTime(session.getFromTimeMillis(), getConference().getConferenceZoneId()));
            session.setEndDate(Util.timeToZonedDateTime(session.getToTimeMillis(), getConference().getConferenceZoneId()));
        }

        sessions.setAll(newSessions);

        retrieveAuthenticatedUserSessionInformation();
    }

    /**
     * This method will first try to read sessions from a conference-specific json file
     * in the app-specific storage. 
     * If it can't find this file, it will call retrieveSessionsInternalCfp() which will
     * retrieve the sessions from the CFP backend, and it will immediately return while
     * the list might not be complete.
     * <br/>
     * If it can find that file, it will retrieve and parse its content. Once this is 
     * done, the <code>sessions</code> field is filled with the parsed content.
     * <br/>
     * When the retrieving from the local file is completed (with state failed or succeeeded)),
     * it will call retrieveSessionsInternalCfp() to always reload data from the CFP backend.
     */
    private void retrieveSessionsInternal() {
        // clear the current list of sessions. this is needed in case we switched
        // the conference. the call to clear will clear the list of sessions
        // that are still present from the previously selected conference. if we don't
        // clear the sessions at this point, the user will see the sessions from the
        // previous conference until the sessions for the newly selected conference are
        // fully loaded (either from disk or from cfp backend)
        sessions.clear();

        File local = null;
        if (rootDir != null) {
            local = new File(rootDir, getConference().getId() + "_sessions.json");
        }

        if (local != null && local.exists()) {
            LOG.log(Level.INFO, "Loading sessions from local storage.");

            FileClient fileClient = FileClient.create(local);
            GluonObservableObject<Sessions> localSessions = DataProvider.retrieveObject(fileClient.createObjectDataReader(new JsonInputConverter<>(Sessions.class)));
            localSessions.stateProperty().addListener((obsState, oldState, newState) -> {
                LOG.log(Level.INFO, "Loading sessions from local storage: " + oldState + " -> " + newState);

                // when local retrieval succeeded, add the local sessions into the final sessions
                if (newState == ConnectState.SUCCEEDED) {
                    loadingOfSessionsFinished(localSessions.get().getSessions());
                }

                // when local retrieval failed or succeeded, load the data again from cfp
                if (newState == ConnectState.FAILED || newState == ConnectState.SUCCEEDED) {
                    retrieveSessionsInternalCfp();
                }
            });
        } else {
            // we have no local storage, retrieve sessions from cfp
            retrieveSessionsInternalCfp();
        }
    }

    /**
     * Retrieve the sessions from the web endpoint. If retrieval was successful, the
     * sessions will also be written to the storage root.
     * This method must be called on the FX App Thread. It returns immediately,
     * and when all data is retrieved it will store the data.
     * Also, calling this method again while retrieval is in progress, will not
     * start a new retrieval process and return immediately from the method.
     */
    private void retrieveSessionsInternalCfp() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSessionsFromCfp.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving sessions from cfp, just return.");
            return;
        }

        ObservableList<Session> internalSessionsList = FXCollections.observableArrayList();

        LOG.log(Level.INFO, "Loading sessions from Devoxx CFP endpoint.");
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(getConference().getCfpEndpoint())
                .path("/conferences/" + getConference().getId() + "/schedules/")
                .connectTimeout(15000);
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

                CountDownLatch fillSessionsCountDownLatch = new CountDownLatch(linksToProcess.size());
                final AtomicInteger processed = new AtomicInteger(0);
                Thread thread = new Thread(() -> {
                    try {
                        boolean ok = fillSessionsCountDownLatch.await(2, TimeUnit.MINUTES);
                        LOG.log(Level.FINE, "Fill sessions countdown latch ok? " + ok + "; Links processed successfully: " + processed.get() + " / " + linksToProcess.size());

                        // only refresh global sessions and store info when all links are processed without problems
                        if (ok && processed.get() == linksToProcess.size()) {
                            Platform.runLater(() -> {
                                loadingOfSessionsFinished(internalSessionsList);
                                GluonObservableObject<Sessions> localSessions = storeSessions(internalSessionsList, new File(rootDir, getConference().getId() + "_sessions.json"));
                                localSessions.stateProperty().addListener((obs, ov, nv) -> {
                                    if (nv == ConnectState.FAILED || nv == ConnectState.SUCCEEDED) {
                                        retrievingSessionsFromCfp.set(false);
                                    }
                                });
                            });
                        } else {
                            retrievingSessionsFromCfp.set(false);
                        }
                    } catch (InterruptedException e) {
                        retrievingSessionsFromCfp.set(false);
                        LOG.log(Level.INFO, "FillSessions thread was interrupted.", e);
                    }
                }, "FillSessions");
                thread.setDaemon(true);
                thread.start();

                LOG.log(Level.FINE, "Need to process " + linksToProcess.size() + " links.");
                for (Link link : linksToProcess) {
                    fillAllSessionsOfDay(internalSessionsList, link, fillSessionsCountDownLatch, processed);
                }
            }
        });

        // when the loading of sessions failed, set retrieving boolean to false
        linkOfSessions.stateProperty().addListener((obs, ov, nv) -> {
            LOG.log(Level.FINE, "linkOfSessions State: " + ov + " -> " + nv);
            if (nv == ConnectState.FAILED) {
                retrievingSessionsFromCfp.set(false);
                LOG.log(Level.INFO, "Failed to retrieve links.", linkOfSessions.getException());
            }
        });
    }

    /**
     * Store the supplied sessions in the supplied file.
     * This method must be called on the FX App Thread, and it returns immediately.
     * @param origSessions
     * @param storageFile
     * @return
     */
    private GluonObservableObject<Sessions> storeSessions(List<Session> origSessions, File storageFile) {
        checkFxThread();
        LinkedList<Session> mySessions = new LinkedList<>(origSessions);
        ObjectDataWriter<Sessions> sessionsWriter = FileClient.create(storageFile)
            .createObjectDataWriter(new JsonOutputConverter<>(Sessions.class));
        return DataProvider.storeObject(new Sessions(mySessions), sessionsWriter);
    }

    /**
     * Retrieve all sessions for a specific day, and add them to the list of all sessions.
     * Adding sessions happens on the FX App thread, so no concurrency issues are expected.
     * @param allSessions the list of all sessions from all days, passed by the caller.
     * @param link a link containing info about the URL for retrieving the sessions for a specific day
     */
    private void fillAllSessionsOfDay(ObservableList<Session> allSessions, Link link,
                                      CountDownLatch fillSessionsCountDownLatch, AtomicInteger processed) {
        String day = link.getHref().substring(link.getHref().lastIndexOf('/') + 1);
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(getConference().getCfpEndpoint())
                .path("/conferences/" + getConference().getId() + "/schedules/" + day)
                .connectTimeout(15000);
        GluonObservableObject<SchedulesOfDay> scheduleOfDay = DataProvider.retrieveObject(restClient.createObjectDataReader(SchedulesOfDay.class));
        scheduleOfDay.stateProperty().addListener((obs, oldState, newState) -> {
            LOG.log(Level.FINE, "scheduleOfDay State for link " + link + ": " + oldState + " -> " + newState);
            if (newState == ConnectState.FAILED) {
                fillSessionsCountDownLatch.countDown();
                scheduleOfDay.getException().printStackTrace();
            } else if (newState == ConnectState.SUCCEEDED) {
                processed.incrementAndGet();
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
        return speakers.getReadOnlyProperty();
    }

    /**
     * Refreshes the global speakers list with the provided speakers list, by setting the provided
     * speakers to the global speakers. Everything that is currently listed in the global speakers
     * list will be cleared.
     *
     * @param newSpeakers The new list of speakers to be set on the global list of speakers.
     */
    private void loadingOfSpeakersFinished(List<Speaker> newSpeakers) {
        speakers.setAll(newSpeakers);
    }

    private void retrieveSpeakersInternal() {
        speakers.clear();

        File local = null;
        if (rootDir != null) {
            local = new File(rootDir, getConference().getId() + "_speakers.json");
        }

        if (local != null && local.exists()) {
            LOG.log(Level.INFO, "Loading speakers from local storage.");

            FileClient fileClient = FileClient.create(local);
            GluonObservableObject<Speakers> localSpeakers = DataProvider.retrieveObject(fileClient.createObjectDataReader(new JsonInputConverter<>(Speakers.class)));
            localSpeakers.stateProperty().addListener((obsState, oldState, newState) -> {
                if (newState == ConnectState.SUCCEEDED) {
                    loadingOfSpeakersFinished(localSpeakers.get().getSpeakers());
                }

                if (newState == ConnectState.FAILED || newState == ConnectState.SUCCEEDED) {
                    retrieveSpeakersInternalCfp();
                }
            });
        } else {
            retrieveSpeakersInternalCfp();
        }
    }

    private void retrieveSpeakersInternalCfp() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSpeakersFromCfp.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving speakers from cfp, just return.");
            return;
        }

        LOG.log(Level.INFO, "Loading speakers from Devoxx CFP endpoint.");
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(getConference().getCfpEndpoint())
                .path("/conferences/" + getConference().getId() + "/speakers")
                .connectTimeout(15000);

        GluonObservableList<Speaker> speakersList = DataProvider.retrieveList(restClient.createListDataReader(Speaker.class));
        speakersList.stateProperty().addListener((obsState, oldState, newState) -> {
            if (newState == ConnectState.SUCCEEDED) {
                loadingOfSpeakersFinished(speakersList);

                if (rootDir != null) {
                    GluonObservableObject<Speakers> localSpeakers = storeSpeakers(speakersList, new File(rootDir, getConference().getId() + "_speakers.json"));
                    localSpeakers.stateProperty().addListener((obs, ov, nv) -> {
                        if (nv == ConnectState.FAILED || nv == ConnectState.SUCCEEDED) {
                            retrievingSpeakersFromCfp.set(false);
                        }
                    });
                } else {
                    LOG.log(Level.WARNING, "Could not store speakers locally, because private storage location could not be retrieved.");
                    retrievingSpeakersFromCfp.set(false);
                }
            } else if (newState == ConnectState.FAILED) {
                retrievingSpeakersFromCfp.set(false);
            }
        });
    }

    /**
     * Store the supplied speakers in the supplied file.
     * This method must be called on the FX App Thread, and it returns immediately.
     * @param origSpeakers
     * @param storageFile
     * @return
     */
    private GluonObservableObject<Speakers> storeSpeakers(List<Speaker> origSpeakers, File storageFile) {
        checkFxThread();
        LinkedList<Speaker> mySpeakers = new LinkedList<>(origSpeakers);
        ObjectDataWriter<Speakers> speakersWriter = FileClient.create(storageFile)
                .createObjectDataWriter(new JsonOutputConverter<>(Speakers.class));
        return DataProvider.storeObject(new Speakers(mySpeakers), speakersWriter);
    }

    @Override
    public ReadOnlyObjectProperty<Speaker> retrieveSpeaker(String uuid) {
        Speaker speakerWithUuid = null;
        for (Speaker speaker : speakers) {
            if (uuid.equals(speaker.getUuid())) {
                speakerWithUuid = speaker;
                break;
            }
        }

        if (speakerWithUuid != null) {
            if (speakerWithUuid.isDetailsRetrieved()) {
                return new ReadOnlyObjectWrapper<>(speakerWithUuid).getReadOnlyProperty();
            } else {
                RestClient restClient = RestClient.create()
                        .method("GET")
                        .host(getConference().getCfpEndpoint())
                        .path("/conferences/" + getConference().getId() + "/speakers/" + uuid)
                        .connectTimeout(15000);

                GluonObservableObject<Speaker> gluonSpeaker = DataProvider.retrieveObject(restClient.createObjectDataReader(Speaker.class));
                gluonSpeaker.initializedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue != null && newValue) {
                            updateSpeakerDetails(gluonSpeaker.get());
                            gluonSpeaker.initializedProperty().removeListener(this);
                        }
                    }
                });
                return gluonSpeaker;
            }
        }

        return new ReadOnlyObjectWrapper<>();
    }

    private void updateSpeakerDetails(Speaker updatedSpeaker) {
        for (Speaker speaker : speakers) {
            if (speaker.getUuid().equals(updatedSpeaker.getUuid())) {
                speaker.setAcceptedTalks(updatedSpeaker.getAcceptedTalks());
                speaker.setAvatarURL(updatedSpeaker.getAvatarURL());
                speaker.setBio(updatedSpeaker.getBio());
                speaker.setBioAsHtml(updatedSpeaker.getBioAsHtml());
                speaker.setBlog(updatedSpeaker.getBlog());
                speaker.setCompany(updatedSpeaker.getCompany());
                speaker.setFirstName(updatedSpeaker.getFirstName());
                speaker.setLang(updatedSpeaker.getLang());
                speaker.setLastName(updatedSpeaker.getLastName());
                speaker.setTwitter(updatedSpeaker.getTwitter());
                speaker.setDetailsRetrieved(true);
            }
        }
    }

    @Override
    public ReadOnlyListProperty<Track> retrieveTracks() {
        return tracks.getReadOnlyProperty();
    }

    private ObservableList<Track> retrieveTracksInternal() {
        ObservableList<Track> tracks = FXCollections.observableArrayList();

        File local = null;
        if (rootDir != null) {
            local = new File(rootDir, getConference().getId() + "_tracks.json");
        }

        if (local != null && local.exists()) {
            LOG.log(Level.INFO, "Loading tracks from local storage.");

            FileClient fileClient = FileClient.create(local);
            GluonObservableObject<Tracks> localTracks = DataProvider.retrieveObject(fileClient.createObjectDataReader(new JsonInputConverter<>(Tracks.class)));
            localTracks.initializedProperty().addListener((obs, ov, nv) -> {
                tracks.addAll(localTracks.get().getTracks());
            });

            // in case of failure, load tracks from cfp anyway
            localTracks.stateProperty().addListener((obs, ov, nv) -> {
                if (nv == ConnectState.FAILED) {
                    Bindings.bindContent(tracks, retrieveTracksInternalCfp());
                }
            });
            return tracks;
        }

        return retrieveTracksInternalCfp();
    }

    private ObservableList<Track> retrieveTracksInternalCfp() {
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(getConference().getCfpEndpoint())
                .path("/conferences/" + getConference().getId() + "/tracks")
                .connectTimeout(15000);

        ObservableList<Track> observableTracks = FXCollections.observableArrayList();
        GluonObservableObject<Tracks> tracks = DataProvider.retrieveObject(restClient.createObjectDataReader(Tracks.class));

        if (rootDir != null) {
            tracks.initializedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    ObjectDataWriter<Tracks> tracksWriter = FileClient.create(new File(rootDir, getConference().getId() + "_tracks.json"))
                            .createObjectDataWriter(new JsonOutputConverter<>(Tracks.class));
                    GluonObservableObject<Tracks> localTracks = DataProvider.storeObject(tracks.get(), tracksWriter);
                    localTracks.stateProperty().addListener((obs, ov, nv) -> {
                        if (nv == ConnectState.SUCCEEDED) {
                            LOG.log(Level.INFO, "Tracks were successfully stored to the local storage of the device.");
                        } else if (nv == ConnectState.FAILED) {
                            LOG.log(Level.WARNING, "Failed to write tracks to local storage.", localTracks.getException());
                        }
                    });

                    tracks.initializedProperty().removeListener(this);
                    observableTracks.addAll(tracks.get().getTracks());
                }
            });
        } else {
            LOG.log(Level.WARNING, "Could not store tracks locally, because private storage location could not be retrieved.");
        }

        return observableTracks;
    }

    @Override
    public ReadOnlyListProperty<ProposalType> retrieveProposalTypes() {
        return proposalTypes.getReadOnlyProperty();
    }

    private ObservableList<ProposalType> retrieveProposalTypesInternal() {
        ObservableList<ProposalType> proposalTypes = FXCollections.observableArrayList();

        File local = null;
        if (rootDir != null) {
            local = new File(rootDir, getConference().getId() + "_proposalTypes.json");
        }

        if (local != null && local.exists()) {
            LOG.log(Level.INFO, "Loading proposal types from local storage.");

            FileClient fileClient = FileClient.create(local);
            GluonObservableObject<ProposalTypes> localProposalTypes = DataProvider.retrieveObject(fileClient.createObjectDataReader(new JsonInputConverter<>(ProposalTypes.class)));
            localProposalTypes.initializedProperty().addListener((obs, ov, nv) -> {
                proposalTypes.addAll(localProposalTypes.get().getProposalTypes());
            });

            // in case of failure, load proposal types from cfp anyway
            localProposalTypes.stateProperty().addListener((obs, ov, nv) -> {
                if (nv == ConnectState.FAILED) {
                    Bindings.bindContent(proposalTypes, retrieveProposalTypesInternalCfp());
                }
            });
            return proposalTypes;
        }

        return retrieveProposalTypesInternalCfp();
    }

    private ObservableList<ProposalType> retrieveProposalTypesInternalCfp() {
        RestClient restClient = RestClient.create()
                .method("GET")
                .host(getConference().getCfpEndpoint())
                .path("/conferences/" + getConference().getId() + "/proposalTypes")
                .connectTimeout(15000);

        ObservableList<ProposalType> observableProposalTypes = FXCollections.observableArrayList();
        GluonObservableObject<ProposalTypes> proposalTypes = DataProvider.retrieveObject(restClient.createObjectDataReader(ProposalTypes.class));

        if (rootDir != null) {
            proposalTypes.initializedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    ObjectDataWriter<ProposalTypes> proposalTypesWriter = FileClient.create(new File(rootDir, getConference().getId() + "_proposalTypes.json"))
                            .createObjectDataWriter(new JsonOutputConverter<>(ProposalTypes.class));
                    GluonObservableObject<ProposalTypes> localProposalTypes = DataProvider.storeObject(proposalTypes.get(), proposalTypesWriter);
                    localProposalTypes.stateProperty().addListener((obs, ov, nv) -> {
                        if (nv == ConnectState.SUCCEEDED) {
                            LOG.log(Level.INFO, "Proposal types were successfully stored to the local storage of the device.");
                        } else if (nv == ConnectState.FAILED) {
                            LOG.log(Level.WARNING, "Failed to write proposal types to local storage.", localProposalTypes.getException());
                        }
                    });

                    proposalTypes.initializedProperty().removeListener(this);
                    observableProposalTypes.addAll(proposalTypes.get().getProposalTypes());
                }
            });
        } else {
            LOG.log(Level.WARNING, "Could not store proposal types locally, because private storage location could not be retrieved.");
        }

        return observableProposalTypes;
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Floor> retrieveExhibitionMaps() {
        return exhibitionMaps.getReadOnlyProperty();
    }

    private ObservableList<Floor> retrieveExhibitionMapsInternal() {
        boolean isTablet = Services.get(DisplayService.class).map(DisplayService::isTablet).orElse(false);
        boolean isPhone = Services.get(DisplayService.class).map(DisplayService::isPhone).orElse(false);
        boolean isDesktop = Services.get(DisplayService.class).map(DisplayService::isDesktop).orElse(true);

        ObservableList<Floor> floors = FXCollections.observableArrayList();
        for (Floor floor : getConference().getFloors()) {
            if (floor.getImg().startsWith("http") &&
                    (("phone".equals(floor.getTarget()) && isPhone) ||
                            ("tablet".equals(floor.getTarget()) && (isDesktop || isTablet)))) {
                floors.add(floor);
            }
        }
        return floors;
    }

    @Override
    public ReadOnlyListProperty<Sponsor> retrieveSponsors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ObservableList<Session> retrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (favoredSessions == null) {
            favoredSessions = internalRetrieveFavoredSessions();
        }

        return favoredSessions;
    }

    @Override
    public ObservableList<Session> retrieveScheduledSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (scheduledSessions == null) {
            DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
            // stop recreating notifications, after the list of scheduled sessions is fully retrieved
            scheduledSessions = internalRetrieveScheduledSessions(notifications::preloadingScheduledSessionsDone);
            // start recreating notifications as soon as the scheduled sessions are being retrieved
            notifications.preloadScheduledSessions();

        }

        return scheduledSessions;
    }

    public ObservableList<Session> internalRetrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user that was verified at Devoxx CFP must be available when calling this method.");
        }

        GluonObservableObject<Favored> functionSessions = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader("favored", Favored.class, getConference().getCfpEndpoint(), cfpUserUuid.get()));
        functionSessions.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                for (SessionId sessionId : functionSessions.get().getFavored()) {
                    findSession(sessionId.getId()).ifPresent(internalFavoredSessions::add);
                }

                internalFavoredSessionsListener = initializeSessionsListener(internalFavoredSessions, "favored");
            }
        });
        functionSessions.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                functionSessions.getException().printStackTrace();
            }
        });

        return internalFavoredSessions;
    }

    public ObservableList<Session> internalRetrieveScheduledSessions(Runnable onStateSucceeded) {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user that was verified at Devoxx CFP must be available when calling this method.");
        }

        GluonObservableObject<Scheduled> functionSessions = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader("scheduled", Scheduled.class, getConference().getCfpEndpoint(), cfpUserUuid.get()));
        functionSessions.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                for (SessionId sessionId : functionSessions.get().getScheduled()) {
                    findSession(sessionId.getId()).ifPresent(internalScheduledSessions::add);
                }

                internalScheduledSessionsListener = initializeSessionsListener(internalScheduledSessions, "scheduled");
            }
        });
        functionSessions.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                functionSessions.getException().printStackTrace();
            }
        });

        if (onStateSucceeded != null) {
            functionSessions.stateProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    if (functionSessions.getState().equals(ConnectState.SUCCEEDED)) {
                        functionSessions.stateProperty().removeListener(this);
                        onStateSucceeded.run();
                    }
                }
            });
        }

        return internalScheduledSessions;
    }

    private ListChangeListener<Session> initializeSessionsListener(ObservableList<Session> sessions, String functionPrefix) {
        ListChangeListener<Session> listChangeListener = c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Session session : c.getRemoved()) {
                        LOG.log(Level.INFO, "Removing Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        GluonObservableObject<String> response = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader(functionPrefix + "Remove", String.class, getConference().getCfpEndpoint(), cfpUserUuid.get(), session.getTalk().getId()));
                        response.stateProperty().addListener((obs, ov, nv) -> {
                            if (nv == ConnectState.FAILED) {
                                LOG.log(Level.WARNING, "Failed to remove session " + session.getTalk().getId() + " from " + functionPrefix + ": " + response.getException().getMessage());
                            }
                        });
                    }
                }
                if (c.wasAdded()) {
                    for (Session session : c.getAddedSubList()) {
                        LOG.log(Level.INFO, "Adding Session: " + session.getTalk().getId() + " / " + session.getTitle());
                        GluonObservableObject<String> response = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader(functionPrefix + "Add", String.class, getConference().getCfpEndpoint(), cfpUserUuid.get(), session.getTalk().getId()));
                        response.stateProperty().addListener((obs, ov, nv) -> {
                            if (nv == ConnectState.FAILED) {
                                LOG.log(Level.WARNING, "Failed to add session " + session.getTalk().getId() + " to " + functionPrefix + ": " + response.getException().getMessage());
                            }
                        });
                    }
                }
            }
        };
        sessions.addListener(listChangeListener);
        return listChangeListener;
    }

    @Override
    public ObservableList<Note> retrieveNotes() {
        if (!isAuthenticated() && DevoxxSettings.USE_REMOTE_NOTES) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (notes == null) {
            notes = internalRetrieveNotes();
        }

        return notes;
    }

    @Override
    public ObservableList<Vote> retrieveVotes() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (votes == null) {
            votes = internalRetrieveVotes();
        }

        return votes;
    }

    @Override
    public void voteTalk(Vote vote) {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        User authenticatedUser = authenticationClient.getAuthenticatedUser();
        if (authenticatedUser.getEmail() == null || authenticatedUser.getEmail().isEmpty()) {
            LOG.log(Level.WARNING, "Can not send vote, authenticated user doesn't have an email address.");
        } else {
            GluonObservableObject<String> voteResult = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader("voteTalk", String.class,
                    getConference().getCfpEndpoint(), String.valueOf(vote.getValue()), authenticatedUser.getEmail(), vote.getTalkId(), vote.getDelivery(), vote.getContent(), vote.getOther()));
            voteResult.initializedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    LOG.log(Level.INFO, "Response from vote: " + voteResult.get());
                }
            });
            voteResult.stateProperty().addListener((obs, ov, nv) -> {
                if (nv == ConnectState.FAILED) {
                    LOG.log(Level.WARNING, "Failed to call vote.", voteResult.getException());
                }
            });
        }
    }

    public ObservableList<Note> internalRetrieveNotes() {
        return internalRetrieveData("notes", Note.class);
    }

    public ObservableList<Vote> internalRetrieveVotes() {
        return internalRetrieveData("votes", Vote.class);
    }

    private <E extends Identifiable> ObservableList<E> internalRetrieveData(String listIdentifier, Class<E> targetClass) {
        GluonObservableList<E> internalDataLocal = DataProvider.retrieveList(localDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_" + listIdentifier, targetClass,
                SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        internalDataLocal.initializedProperty().addListener((obsLocal, ovLocal, nvLocal) -> {
            if (nvLocal) {
                if (!listIdentifier.equals("notes") || DevoxxSettings.USE_REMOTE_NOTES) {
                    GluonObservableList<E> internalDataCloud = DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_" + listIdentifier, targetClass,
                            SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
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

    private void loadCfpAccount(User user, Runnable successRunnable) {
        if (cfpUserUuid.isEmpty().get()) {
            Services.get(SettingsService.class).ifPresent(settingsService -> {
                String devoxxCfpAccountUuid = settingsService.retrieve(DevoxxSettings.SAVED_ACCOUNT_ID);
                if (devoxxCfpAccountUuid == null) {
                    GluonObservableObject<String> accountUuid = DataProvider.retrieveObject(cloudDataClient.createFunctionObjectDataReader("verifyAccount", String.class,
                            getConference().getCfpEndpoint(), user.getNetworkId(), user.getLoginMethod().name(), user.getEmail()));
                    accountUuid.initializedProperty().addListener((obs, ov, nv) -> {
                        LOG.log(Level.INFO, "Verified user " + user + " as account with uuid " + accountUuid);
                        cfpUserUuid.set(accountUuid.get());
                        settingsService.store(DevoxxSettings.SAVED_ACCOUNT_ID, accountUuid.get());

                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                    });
                } else {
                    LOG.log(Level.INFO, "Verified user " + user + " retrieved from settings " + devoxxCfpAccountUuid);
                    cfpUserUuid.set(devoxxCfpAccountUuid);
                }
            });
        }
    }

    private void retrieveAuthenticatedUserSessionInformation() {
        if (isAuthenticated()) {
            retrieveNotes();
            retrieveVotes();

            retrieveFavoredSessions();
            retrieveScheduledSessions();
        }
    }

    private void clearCfpAccount() {
        cfpUserUuid.set("");
        notes = null;
        votes = null;
        favoredSessions = null;
        scheduledSessions = null;
        internalFavoredSessions.clear();
        internalScheduledSessions.clear();

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            settingsService.remove(DevoxxSettings.SAVED_ACCOUNT_ID);
        });
    }

    @Override
    public PushNotification retrievePushNotification() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void checkFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Not on FX AppThread!");
        }
    }

    private String readConferenceIdFromFile(File reload) {
        StringBuilder fileContent = new StringBuilder((int) reload.length());
        Scanner scanner = null;
        try {
            scanner = new Scanner(reload);
            String lineSeparator = System.getProperty("line.separator");
            while (scanner.hasNextLine()) {
                fileContent.append(scanner.nextLine() + lineSeparator);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(scanner != null) {
                scanner.close();
            }
        }
        System.out.println("read reload file '"+fileContent.toString()+"'");
        return findConferenceIdFromString(fileContent.toString());
    }

    private String findConferenceIdFromString(String fileContent) {
        try {
            String trimmedContent = fileContent.replaceAll("\"", "").replaceAll(" ", "").replaceAll("\\}", ",");
            String[] keyValue = trimmedContent.split(",");
            for (int i = 0; i < keyValue.length; i++) {
                if (keyValue[i].contains("body")) {
                    return keyValue[i].split(":")[1];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void findAndSetConference(String configuredConference, GluonObservableList<Conference> conferences) {
        boolean found = false;
        for (Conference conference : conferences) {
            if (conference.getId().equals(configuredConference)) {
                found = true;
                setConferenceAndLoadAuthenticatedUser(conference);
                break;
            }
        }
        if (!found) {
            conferences.addListener(new ListChangeListener<Conference>() {
                @Override
                public void onChanged(Change<? extends Conference> c) {
                    while (c.next()) {
                        for (Conference conference : c.getAddedSubList()) {
                            if (conference.getId().equals(configuredConference)) {
                                setConferenceAndLoadAuthenticatedUser(conference);
                                retrieveConferences().removeListener(this);
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    private void setConferenceAndLoadAuthenticatedUser(Conference conference) {
        setConference(conference);
        if (authenticationClient.isAuthenticated()) {
            loadCfpAccount(authenticationClient.getAuthenticatedUser(), null);
        }
    }
}
