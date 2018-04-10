/*
 * Copyright (c) 2016, 2018 Gluon Software
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
import com.devoxx.model.*;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.SessionVisuals.SessionListType;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.down.plugins.RuntimeArgsService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.data.*;
import com.gluonhq.cloudlink.client.push.PushClient;
import com.gluonhq.cloudlink.client.user.LoginMethod;
import com.gluonhq.cloudlink.client.user.User;
import com.gluonhq.cloudlink.client.user.UserClient;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.converter.InputStreamIterableInputConverter;
import com.gluonhq.connect.converter.JsonInputConverter;
import com.gluonhq.connect.converter.JsonIterableInputConverter;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.InputStreamListDataReader;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.RestClient;
import com.gluonhq.connect.source.BasicInputDataSource;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
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
            Services.get(RuntimeArgsService.class).ifPresent(ras -> {
                ras.addListener(RuntimeArgsService.LAUNCH_PUSH_NOTIFICATION_KEY, (f) -> {
                    System.out.println(">>> received a silent push notification with contents: " + f);
                    System.out.println("[DBG] writing reload file");
                    File reloadMe = new File (rootDir, "reload");
                    try (BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(reloadMe)))) {
                        br.write(f);
                        System.out.println("[DBG] writing reload file done");
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        System.out.println("[DBG] exception writing reload file "+ex);
                    }
                });
            });
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private GluonObservableList<Conference> conferences;
    private final ReadOnlyObjectWrapper<Conference> conference = new ReadOnlyObjectWrapper<>();

    private final UserClient authenticationClient;
    private final PushClient pushClient;
    private final DataClient localDataClient;
    private final DataClient cloudDataClient;

    private final StringProperty cfpUserUuid = new SimpleStringProperty(this, "cfpUserUuid", "");

    private final BooleanProperty ready = new SimpleBooleanProperty(false);

    /**
     * The sessions field is crucial. It is returned in the
     * retrieveSessions call that is used by the SessionsPresenter. Hence, the content of the sessions
     * directly reflect to the UI.
     */
    private final ReadOnlyListWrapper<Session> sessions = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSessions = new AtomicBoolean(false);

    private final ReadOnlyListWrapper<Speaker> speakers = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private final AtomicBoolean retrievingSpeakers = new AtomicBoolean(false);

    private ReadOnlyListWrapper<Track> tracks = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<ProposalType> proposalTypes = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    private ReadOnlyListWrapper<Floor> exhibitionMaps = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    // user specific data
    private ObservableList<Session> favoredSessions;
    private ObservableList<Session> scheduledSessions;
    private ObservableList<Note> notes;
    private ObservableList<Badge> badges;
    private ObservableList<SponsorBadge> sponsorBadges;

    private GluonObservableObject<Favorites> allFavorites;
    private ListChangeListener<Session> internalFavoredSessionsListener = null;
    private ListChangeListener<Session> internalScheduledSessionsListener = null;
    private ObservableList<Session> internalFavoredSessions = FXCollections.observableArrayList();
    private ObservableList<Session> internalScheduledSessions = FXCollections.observableArrayList();
    private ObservableList<Favorite> favorites = FXCollections.observableArrayList();
    private ObservableList<Sponsor> sponsors = FXCollections.observableArrayList();

    public DevoxxService() {
        ready.set(false);

        conferences = retrieveConferencesInternal();
        allFavorites = new GluonObservableObject<>();
        allFavorites.setState(ConnectState.SUCCEEDED);

        localDataClient = DataClientBuilder.create()
                .operationMode(OperationMode.LOCAL_ONLY)
                .build();

        authenticationClient = new UserClient();

        cloudDataClient = DataClientBuilder.create()
                .authenticateWith(authenticationClient)
                .operationMode(OperationMode.CLOUD_FIRST)
                .build();

        // enable push notifications and subscribe to the possibly selected conference
        pushClient = new PushClient();
        pushClient.enable(DevoxxNotifications.GCM_SENDER_ID);
        pushClient.enabledProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                Conference conference = getConference();
                if (conference != null) {
                    pushClient.subscribe(conference.getId());
                }
            }
        });

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

                    // unsubscribe from previous push notification topic
                    if (pushClient.isEnabled()) {
                        pushClient.unsubscribe(ov.getId());
                    }
                }

                // subscribe to push notification topic, named after the conference id
                if (pushClient.isEnabled()) {
                    pushClient.subscribe(nv.getId());
                }

                retrieveSessionsInternal();
                retrieveSpeakersInternal();
                retrieveTracksInternal();
                retrieveProposalTypesInternal();
                retrieveExhibitionMapsInternal();

                favorites.clear();
                refreshFavorites();
            }
        });

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            String configuredConference = settingsService.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (configuredConference != null) {
                if (conferences.isInitialized()) {
                    findAndSetConference(configuredConference, conferences);
                } else {
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
            } else {
                if (conferences.isInitialized()) {
                    ready.set(true);
                } else {
                    conferences.initializedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                            if (newValue) {
                                ready.set(true);
                                conferences.initializedProperty().removeListener(this);
                            }
                        }
                    });
                }
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

    @Override
    public BooleanProperty readyProperty() {
        return ready;
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
                if (!conferenceIdForReload.isEmpty() && conferenceIdForReload.equalsIgnoreCase(getConference().getId())) {
                    reload.delete();
                    retrieveSessionsInternal();
                    retrieveSpeakersInternal();
                    System.out.println("[DBB] data reloading for "+conferenceIdForReload);
                }
            }
        }
    }

    @Override
    public ObservableList<Session> reloadSessionsFromCFP(SessionListType sessionListType) {
        if (getConference() != null) {
            if (sessionListType == SessionListType.FAVORITES) {
                favoredSessions = null;
                if (internalFavoredSessions != null && internalFavoredSessionsListener != null) {
                    internalFavoredSessions.removeListener(internalFavoredSessionsListener);
                    internalFavoredSessions.clear();
                    return retrieveFavoredSessions();
                }
            } else if (sessionListType == SessionListType.SCHEDULED) {
                scheduledSessions = null;
                if (internalScheduledSessions != null && internalScheduledSessionsListener != null) {
                    internalScheduledSessions.removeListener(internalScheduledSessionsListener);
                    internalScheduledSessions.clear();
                    return retrieveScheduledSessions();
                }
            }
        }
        return FXCollections.emptyObservableList();
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

    private void retrieveSessionsInternal() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSessions.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving sessions from cfp, just return.");
            return;
        }

        sessions.clear();

        RemoteFunctionList fnSessions = RemoteFunctionBuilder.create("sessions")
                .param("cfpEndpoint", getConference().getCfpEndpoint())
                .param("conferenceId", getConference().getId())
                .list();

        GluonObservableList<Session> sessionsList = fnSessions.call(Session.class);
        ListChangeListener<Session> sessionsListChangeListener = change -> {
            while (change.next()) {
                for (Session session : change.getAddedSubList()) {
                    session.setStartDate(timeToZonedDateTime(session.getFromTimeMillis(), getConference().getConferenceZoneId()));
                    session.setEndDate(timeToZonedDateTime(session.getToTimeMillis(), getConference().getConferenceZoneId()));
                }
            }
        };
        sessionsList.addListener(sessionsListChangeListener);
        sessionsList.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.SUCCEEDED || nv == ConnectState.FAILED) {
                retrievingSessions.set(false);
                sessionsList.removeListener(sessionsListChangeListener);
            }
            if (nv == ConnectState.SUCCEEDED) {
                retrieveAuthenticatedUserSessionInformation();
            }
            if (nv == ConnectState.FAILED) {
                sessionsList.getException().printStackTrace();
            }
        });

        sessions.set(sessionsList);
    }

    @Override
    public ReadOnlyListProperty<Speaker> retrieveSpeakers() {
        return speakers.getReadOnlyProperty();
    }

    private void retrieveSpeakersInternal() {
        // if a retrieval is ongoing, don't initiate again
        if (!retrievingSpeakers.compareAndSet(false, true)) {
            LOG.log(Level.FINE, "Already retrieving speakers from cfp, just return.");
            return;
        }

        speakers.clear();

        RemoteFunctionList fnSpeakers = RemoteFunctionBuilder.create("speakers")
                .param("cfpEndpoint", getConference().getCfpEndpoint())
                .param("conferenceId", getConference().getId())
                .list();

        GluonObservableList<Speaker> speakersList = fnSpeakers.call(Speaker.class);
        speakersList.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                retrievingSpeakers.set(false);
            } else if (nv == ConnectState.SUCCEEDED) {
                speakers.setAll(speakersList);
                retrievingSpeakers.set(false);
            }
        });
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

    private void retrieveTracksInternal() {
        RemoteFunctionObject fnTracks = RemoteFunctionBuilder.create("tracks")
                .param("cfpEndpoint", getConference().getCfpEndpoint())
                .param("conferenceId", getConference().getId())
                .object();

        GluonObservableObject<Tracks> gluonTracks = fnTracks.call(Tracks.class);
        gluonTracks.addListener((obs, ov, nv) -> tracks.setAll(gluonTracks.get().getTracks()));
    }

    @Override
    public ReadOnlyListProperty<ProposalType> retrieveProposalTypes() {
        return proposalTypes.getReadOnlyProperty();
    }

    private void retrieveProposalTypesInternal() {
        RemoteFunctionObject fnProposalTypes = RemoteFunctionBuilder.create("proposalTypes")
                .param("cfpEndpoint", getConference().getCfpEndpoint())
                .param("conferenceId", getConference().getId())
                .object();

        GluonObservableObject<ProposalTypes> gluonProposalTypes = fnProposalTypes.call(ProposalTypes.class);
        gluonProposalTypes.addListener((obs, ov, nv) -> proposalTypes.setAll(gluonProposalTypes.get().getProposalTypes()));
    }

    @Override
    public ReadOnlyListProperty<Exhibitor> retrieveExhibitors() {
        return new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    }

    @Override
    public ReadOnlyListProperty<Floor> retrieveExhibitionMaps() {
        return exhibitionMaps.getReadOnlyProperty();
    }

    private void retrieveExhibitionMapsInternal() {
        Task<List<Floor>> task = new Task<List<Floor>>() {
            @Override
            protected List<Floor> call() throws Exception {
                boolean isTablet = Services.get(DisplayService.class).map(DisplayService::isTablet).orElse(false);
                boolean isPhone = Services.get(DisplayService.class).map(DisplayService::isPhone).orElse(false);
                boolean isDesktop = Services.get(DisplayService.class).map(DisplayService::isDesktop).orElse(true);

                List<Floor> floors = new ArrayList<>();
                for (Floor floor : getConference().getFloors()) {
                    if (floor.getImg().startsWith("http") &&
                            (("phone".equals(floor.getTarget()) && isPhone) ||
                                    ("tablet".equals(floor.getTarget()) && (isDesktop || isTablet)))) {
                        floors.add(floor);
                    }
                }

                return floors;
            }
        };

        task.setOnSucceeded(event -> exhibitionMaps.setAll(task.getValue()));

        Thread retrieveExhibitionMapsThread = new Thread(task);
        retrieveExhibitionMapsThread.setDaemon(true);
        retrieveExhibitionMapsThread.start();
    }

    @Override
    public ObservableList<Sponsor> retrieveSponsors() {
        // TODO: pass the CfpEndpoint
        RemoteFunctionObject fnSponsors = RemoteFunctionBuilder.create("sponsors").object();
        GluonObservableObject<EventSponsor> badgeSponsorsObject = fnSponsors.call(EventSponsor.class);
        badgeSponsorsObject.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                for (Event event : badgeSponsorsObject.get().getEvents()) {
                    //TODO: Uncomment this once everything is fixed
                    //if (event.getSlug().equalsIgnoreCase(getConference().getId())) {
                        sponsors.setAll(event.getSponsors());
                    //}
                }
            } 
        });
        badgeSponsorsObject.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                badgeSponsorsObject.getException().printStackTrace();
            }
        });
        return sponsors;
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
            try {
                DevoxxNotifications notifications = Injector.instantiateModelOrService(DevoxxNotifications.class);
                // stop recreating notifications, after the list of scheduled sessions is fully retrieved
                scheduledSessions = internalRetrieveScheduledSessions(notifications::preloadingScheduledSessionsDone);
                // start recreating notifications as soon as the scheduled sessions are being retrieved
                notifications.preloadScheduledSessions();
            } catch (IllegalStateException ise) {
                LOG.log(Level.WARNING, "Can't instantiate Notifications when running a background service");
            }
        }

        return scheduledSessions;
    }

    public ObservableList<Session> internalRetrieveFavoredSessions() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("An authenticated user that was verified at Devoxx CFP must be available when calling this method.");
        }

        RemoteFunctionObject fnFavored = RemoteFunctionBuilder.create("favored")
                .param("0", getConference().getCfpEndpoint())
                .param("1", cfpUserUuid.get())
                .object();

        GluonObservableObject<Favored> functionSessions = fnFavored.call(Favored.class);
        functionSessions.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                for (SessionId sessionId : functionSessions.get().getFavored()) {
                    findSession(sessionId.getId()).ifPresent(internalFavoredSessions::add);
                }

                internalFavoredSessionsListener = initializeSessionsListener(internalFavoredSessions, "favored");
                ready.set(true);
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

        RemoteFunctionObject fnScheduled = RemoteFunctionBuilder.create("scheduled")
                .param("0", getConference().getCfpEndpoint())
                .param("1", cfpUserUuid.get())
                .object();

        GluonObservableObject<Scheduled> functionSessions = fnScheduled.call(Scheduled.class);
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
                        RemoteFunctionObject fnRemove = RemoteFunctionBuilder.create(functionPrefix + "Remove")
                                .param("0", getConference().getCfpEndpoint())
                                .param("1", cfpUserUuid.get())
                                .param("2", session.getTalk().getId())
                                .object();
                        GluonObservableObject<String> response = fnRemove.call(String.class);
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
                        RemoteFunctionObject fnAdd = RemoteFunctionBuilder.create(functionPrefix + "Add")
                                .param("0", getConference().getCfpEndpoint())
                                .param("1", cfpUserUuid.get())
                                .param("2", session.getTalk().getId())
                                .object();
                        GluonObservableObject<String> response = fnAdd.call(String.class);
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
    public ObservableList<Badge> retrieveBadges() {
        if (!isAuthenticated() && DevoxxSettings.USE_REMOTE_NOTES) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }

        if (badges == null) {
            badges = internalRetrieveBadges();
        }

        return badges;
    }

    @Override
    public ObservableList<SponsorBadge> retrieveSponsorBadges() {
        if (!isAuthenticated() && DevoxxSettings.USE_REMOTE_NOTES) {
            throw new IllegalStateException("An authenticated user must be available when calling this method.");
        }
        
        if (sponsorBadges == null) {
            final GluonObservableList<SponsorBadge> sponsorBadges = internalRetrieveSponsorBadges();
            this.sponsorBadges = sponsorBadges;
            
            // every scanned sponsor badge must be posted with the remote function
            sponsorBadges.initializedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> o, Boolean ov, Boolean nv) {
                    if (nv) {
                        sponsorBadges.addListener((ListChangeListener<SponsorBadge>) c -> {
                            while (c.next()) {
                                if (c.wasAdded()) {
                                    for (SponsorBadge sponsorBadge : c.getAddedSubList()) {
                                        saveSponsorBadge(sponsorBadge);
                                    }
                                }
                            }
                        });
                    }
                    sponsorBadges.initializedProperty().removeListener(this);
                }
            });
        }

        return sponsorBadges;
    }

    @Override
    public void saveSponsorBadge(SponsorBadge sponsorBadge) {
        RemoteFunctionObject fnSponsorBadge = RemoteFunctionBuilder.create("saveSponsorBadge")
                .param("0", sponsorBadge.getSlug())
                .param("1", sponsorBadge.getBadgeId())
                .param("2", sponsorBadge.getFirstName())
                .param("3", sponsorBadge.getLastName())
                .param("4", sponsorBadge.getCompany())
                .param("5", sponsorBadge.getEmail())
                .param("6", sponsorBadge.getDetails())
                .param("7", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .object();
        GluonObservableObject<String> sponsorBadgeResult = fnSponsorBadge.call(String.class);
        sponsorBadgeResult.initializedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                LOG.log(Level.INFO, "Response from save sponsor badge: " + sponsorBadgeResult.get());
            }
        });
        sponsorBadgeResult.stateProperty().addListener((obs, ov, nv) -> {
            if (nv == ConnectState.FAILED) {
                LOG.log(Level.WARNING, "Failed to call save sponsor badge: ", sponsorBadgeResult.getException());
            }
        });
    }

    @Override
    public GluonObservableObject<String> authenticateSponsor() {
        RemoteFunctionObject fnValidateSponsor = RemoteFunctionBuilder.create("validateSponsor").cachingEnabled(false).object();
        return fnValidateSponsor.call(String.class);
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
            RemoteFunctionObject fnVoteTalk = RemoteFunctionBuilder.create("voteTalk")
                    .param("0", getConference().getCfpEndpoint())
                    .param("1", String.valueOf(vote.getValue()))
                    .param("2", authenticatedUser.getEmail())
                    .param("3", vote.getTalkId())
                    .param("4", vote.getDelivery())
                    .param("5", vote.getContent())
                    .param("6", vote.getOther())
                    .object();
            GluonObservableObject<String> voteResult = fnVoteTalk.call(String.class);
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

    @Override
    public ObservableList<Favorite> retrieveFavorites() {
        return favorites;
    }

    @Override
    public void refreshFavorites() {
        if (getConference() != null && DevoxxSettings.conferenceHasFavoriteCount(getConference()) && 
                (allFavorites.getState() == ConnectState.SUCCEEDED || allFavorites.getState() == ConnectState.FAILED)) {
            RemoteFunctionObject fnAllFavorites = RemoteFunctionBuilder.create("allFavorites")
                    .param("0", getConference().getCfpEndpoint())
                    .object();
            allFavorites = fnAllFavorites.call(new JsonInputConverter<>(Favorites.class));
            allFavorites.stateProperty().addListener(new ChangeListener<ConnectState>() {
                @Override
                public void changed(ObservableValue<? extends ConnectState> observable, ConnectState oldValue, ConnectState newValue) {
                    if (newValue == ConnectState.SUCCEEDED) {
                        for (Favorite favorite : allFavorites.get().getFavorites()) {
                            int index = 0;
                            for (; index < favorites.size(); index++) {
                                if (favorites.get(index).getId().equals(favorite.getId())) {
                                    favorites.get(index).setFavs(favorite.getFavs());
                                    break;
                                }
                            }
                            if (index == favorites.size()) {
                                favorites.add(favorite);
                            }
                        }
                        allFavorites.stateProperty().removeListener(this);
                    } else if (newValue == ConnectState.FAILED) {
                        allFavorites.stateProperty().removeListener(this);
                    }
                }
            });
        }
    }

    private ObservableList<Note> internalRetrieveNotes() {
        if (DevoxxSettings.USE_REMOTE_NOTES) {
            return DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_notes",
                    Note.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        } else {
            return DataProvider.retrieveList(localDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_notes",
                    Note.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        }
    }

    private ObservableList<Badge> internalRetrieveBadges() {
        if (DevoxxSettings.USE_REMOTE_NOTES) {
            return DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_badges",
                    Badge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        } else {
            return DataProvider.retrieveList(localDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_badges",
                    Badge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        }
    }

    private GluonObservableList<SponsorBadge> internalRetrieveSponsorBadges() {
        return DataProvider.retrieveList(cloudDataClient.createListDataReader(authenticationClient.getAuthenticatedUser().getKey() + "_sponsor_badges",
                SponsorBadge.class, SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
    }

    private void loadCfpAccount(User user, Runnable successRunnable) {
        if (cfpUserUuid.isEmpty().get()) {
            Services.get(SettingsService.class).ifPresent(settingsService -> {
                String devoxxCfpAccountUuid = settingsService.retrieve(DevoxxSettings.SAVED_ACCOUNT_ID);
                if (devoxxCfpAccountUuid == null) {
                    if (user.getLoginMethod() == LoginMethod.Type.CUSTOM) {
                        LOG.log(Level.INFO, "Logged in user " + user + " as account with uuid " + user.getNetworkId());
                        cfpUserUuid.set(user.getNetworkId());
                        settingsService.store(DevoxxSettings.SAVED_ACCOUNT_ID, user.getNetworkId());

                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                    } else {
                        RemoteFunctionObject fnVerifyAccount = RemoteFunctionBuilder.create("verifyAccount")
                                .param("0", getConference().getCfpEndpoint())
                                .param("1", user.getNetworkId())
                                .param("2", user.getLoginMethod().name())
                                .param("3", user.getEmail())
                                .object();
                        GluonObservableObject<String> accountUuid = fnVerifyAccount.call(String.class);
                        accountUuid.initializedProperty().addListener((obs, ov, nv) -> {
                            if (nv) {
                                LOG.log(Level.INFO, "Verified user " + user + " as account with uuid " + accountUuid);
                                cfpUserUuid.set(accountUuid.get());
                                settingsService.store(DevoxxSettings.SAVED_ACCOUNT_ID, accountUuid.get());

                                if (successRunnable != null) {
                                    successRunnable.run();
                                }
                            }
                        });
                    }
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
            if (DevoxxSettings.conferenceHasBadgeView(getConference())) {
                retrieveBadges();
                retrieveSponsors();
                retrieveSponsorBadges();
            }
            
            retrieveFavoredSessions();
            retrieveScheduledSessions();
        } else {
            ready.set(true);
        }
    }

    private void clearCfpAccount() {
        cfpUserUuid.set("");
        notes = null;
        badges = null;
        sponsorBadges = null;
        favoredSessions = null;
        scheduledSessions = null;
        sponsors.clear();
        internalFavoredSessions.clear();
        internalScheduledSessions.clear();
        ready.set(false);

        Services.get(SettingsService.class).ifPresent(settingsService -> {
            settingsService.remove(DevoxxSettings.SAVED_ACCOUNT_ID);
            settingsService.remove(DevoxxSettings.BADGE_TYPE);
            settingsService.remove(DevoxxSettings.SPONSOR_NAME);
            settingsService.remove(DevoxxSettings.SPONSOR_SLUG);
        });
    }

    private String readConferenceIdFromFile(File reload) {
        StringBuilder fileContent = new StringBuilder((int) reload.length());
        Scanner scanner = null;
        try {
            scanner = new Scanner(reload);
            String lineSeparator = System.getProperty("line.separator");
            while (scanner.hasNextLine()) {
                fileContent.append(scanner.nextLine()).append(lineSeparator);
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
            String trimmedContent = fileContent.replaceAll("\"", "")
                                               .replaceAll(" ", "")
                                               .replaceAll("\\}", ",");
            String[] keyValue = trimmedContent.split(",");
            for (String aKeyValue : keyValue) {
                if (aKeyValue.contains("body")) {
                    return aKeyValue.split(":")[1];
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

    private static ZonedDateTime timeToZonedDateTime(long time, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zoneId);
    }
}
