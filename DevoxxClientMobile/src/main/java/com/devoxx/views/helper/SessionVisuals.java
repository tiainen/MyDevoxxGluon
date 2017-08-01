/*
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
package com.devoxx.views.helper;

import com.devoxx.model.Favorite;
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxNotifications;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.dialog.SessionConflictDialog;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.visual.GlistenStyleClasses;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.util.DevoxxLogging.LOGGING_ENABLED;
import static com.gluonhq.charm.glisten.visual.MaterialDesignIcon.*;

@Singleton
public class SessionVisuals {

    private static final Logger LOG = Logger.getLogger(SessionVisuals.class.getName());
    private static final String SCH_FAV_DIALOG_MSG_1 = "Add a session to your schedule with ";
    private static final String SCH_FAV_DIALOG_MSG_2 = "Add a session to your favorites with ";
    private static final String SCH_FAV_DIALOG_MSG_INTRO = "You can favorite multiple sessions but can only schedule one in the same time slot.";

    public enum SessionListType {
        FAVORITES("favorites",  "favorite",  FAVORITE_BORDER, FAVORITE),
        SCHEDULED("scheduled",  "scheduled", STAR_BORDER,     STAR);

        private final String id;
        private final MaterialDesignIcon offIcon;
        private final MaterialDesignIcon onIcon;
        private final String style;

        SessionListType(String description, String style, MaterialDesignIcon offIcon, MaterialDesignIcon onIcon) {
            id = description;
            this.style = style;
            this.offIcon = offIcon;
            this.onIcon = onIcon;
        }

        public String getStyleClass() {
            return style;
        }

        public Node getOffGraphic() {
            return offIcon.graphic();
        }

        public Node getOnGraphic() {
            return onIcon.graphic();
        }

        public MaterialDesignIcon getOnIcon() {
            return onIcon;
        }

        public SessionListType other() {
            return this == FAVORITES? SCHEDULED: FAVORITES;
        }
    }

    @Inject
    private Service service;
    
    @Inject
    private DevoxxNotifications devoxxNotifications;

    private boolean usingOfflineEmptyLists = true;
    private final Map<SessionListType, ObservableList<Session>> cloudLists = new HashMap<>();

    // Method marked with @PostConstruct is called immediately after
    // instance is created and and members are injected
    // It should be used for initialization instead of constructor in DI contexts
    @PostConstruct
    private void init() {
        retrieveLists();
    }

    public Service getService() {
        return service;
    }

    public String formatMultilineInfo(Session session) {
        ZonedDateTime startDate = session.getStartDate();
        ZonedDateTime endDate = session.getEndDate();
        return String.format(DevoxxBundle.getString("OTN.VISUALS.FORMAT.MULTILINE",
                session.getTalk().getTrack(),
                service.getConference().getConferenceDayIndex(startDate),
                startDate.format(DevoxxSettings.TIME_FORMATTER),
                endDate.format(DevoxxSettings.TIME_FORMATTER),
                session.getRoomName())
        );
    }


    public String formatOneLineInfo(Session session) {
        ZonedDateTime startDate = session.getStartDate();
        ZonedDateTime endDate = session.getEndDate();
        return String.format(DevoxxBundle.getString("OTN.VISUALS.FORMAT.ONELINE",
                service.getConference().getConferenceDayIndex(startDate),
                startDate.format(DevoxxSettings.TIME_FORMATTER),
                endDate.format(DevoxxSettings.TIME_FORMATTER),
                session.getLocation())
        );

    }

    public Node getFavoriteButton(Session session) {
        ToggleButton favButton = buildButton(SessionListType.FAVORITES, session);
        Label favCounter = new Label();
        favCounter.getStyleClass().add("fav-counter");
        Optional<Favorite> favorite = Optional.empty();
        for (Favorite fav : service.retrieveFavorites()) {
            if (fav.getId().equals(session.getTalk().getId())) {
                favorite = Optional.of(fav);
                break;
            }
        }
        Favorite fav = favorite.orElseGet(() -> {
            Favorite emptyFavorite = new Favorite();
            emptyFavorite.setId(session.getTalk().getId());
            service.retrieveFavorites().add(emptyFavorite);
            return emptyFavorite;
        });

        favCounter.textProperty().bind(fav.favsProperty().asString());
        favCounter.managedProperty().bind(fav.favsProperty().greaterThanOrEqualTo(10));
        HBox favBox = new HBox(favButton, favCounter);
        favBox.getStyleClass().add("fav-box");
        return favBox;
    }

    public ToggleButton getSelectedButton(Session session) {
        return buildButton(SessionListType.SCHEDULED, session);
    }

    private ToggleButton buildButton(SessionListType listType, Session session) {
        ToggleButton button = new ToggleButton("", listType.getOffGraphic());
        button.getStyleClass().addAll(
                listType.getStyleClass(),
                GlistenStyleClasses.BUTTON_FLAT,
                GlistenStyleClasses.BUTTON_ROUND);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        // should be a separate listener or styles are messed up
        button.selectedProperty().addListener((o, ov, selected) -> {
            button.setGraphic(selected ? listType.getOnGraphic() : listType.getOffGraphic());
        });

        // setup  binding between button and appropriate list of session ids
        boolean isSelected = listContains(session, listType);
        button.setSelected(isSelected);
        button.selectedProperty().addListener((o, ov, selected) -> {
            if (button.getUserData() == LOCK) return;

            if (!service.isAuthenticated()) {
                service.authenticate(() -> handleAuthenticatedAction(listType, button, selected), () -> {
                    quietUpdateBtnState(button, () -> button.setSelected(ov));
                });
            } else {
                handleAuthenticatedAction(listType, button, selected);
            }
        });
        updateButton(button, isSelected, session);
        return button;
    }

    private void handleAuthenticatedAction(SessionListType listType, ToggleButton button, boolean selected) {
        // we must use the session stored in the button - not the one provided
        // as an argument to the buildButton method
        Session actualSession = (Session) button.getProperties().get(SESSION_KEY);

        if (selected) {
            // Resolve scheduling conflicts
            if (listType == SessionListType.SCHEDULED) {
                Optional<Session> conflictOption = findConflictingSession(actualSession);
                if (conflictOption.isPresent()) {
                    Session conflict = conflictOption.get();
                    Optional<Session> dialogResult = new SessionConflictDialog(conflict, actualSession, this).showAndWait();
                    if (dialogResult.isPresent()) {
                        Session selectedSession = dialogResult.get();
                        if (selectedSession.equals(actualSession)) {
                            // It is important to preserve this order: first remove and then add because of the
                            // toast messages. This way "session scheduled" will be displayed.
                            listRemove(conflict, listType);
                            listAdd(actualSession, listType);
                            updateButton(button, true, actualSession);
                        } else {
                            button.setSelected(false);
                        }
                    } else {
                        button.setSelected(false);
                    }
                } else {
                    listAdd(actualSession, listType);
                }
            } else {
                listAdd(actualSession, listType);
            }
        } else {
            listRemove(actualSession, listType);
        }
        if (listType == SessionListType.FAVORITES) {
            service.refreshFavorites();
        }
    }

    private static final Object LOCK = new Object();
    private static final String SESSION_KEY = "SESSION";

    private ObservableList<Session> getList(SessionListType listType) {
        if (usingOfflineEmptyLists && service.isAuthenticated()) {
            // OTN-513 - First time user logs in: stop the listener
            devoxxNotifications.preloadingScheduledSessionsDone();
            
            retrieveLists();
        }
        return cloudLists.get(listType);
    }

    /**
     * Is session a part of the list
     *
     * @param session
     * @param listType
     * @return
     */
    private boolean listContains(Session session, SessionListType listType) {
        return getList(listType).contains(session);
    }

    /**
     * Add session to a list
     *
     * @param session
     * @param listType
     */
    private void listAdd(Session session, SessionListType listType) {
        if (!service.isAuthenticated()) {
            service.authenticate(() -> listAdd(session, listType));
        } else {
            retrieveLists();

            if (!listContains(session, listType)) {
                if (listType == SessionListType.FAVORITES || listType == SessionListType.SCHEDULED) {
                    if (listType == SessionListType.SCHEDULED) {
                        devoxxNotifications.addScheduledSessionNotifications(session);
                    }
                    Services.get(SettingsService.class).ifPresent(settings -> {
                        String skip = settings.retrieve(DevoxxSettings.SKIP_SCH_FAV_DIALOG);
                        if (skip == null || skip.isEmpty() || !Boolean.parseBoolean(skip)) {
                            final Dialog<TextFlow> information = createSchFavDialog();
                            information.showAndWait();
                            settings.store(DevoxxSettings.SKIP_SCH_FAV_DIALOG, Boolean.TRUE.toString());
                        }
                    });
                }

                getList(listType).add(session);
                showToast(listType, true);
            }
        }
    }

    /**
     * Remove session from a list
     *
     * @param session
     * @param listType
     */
    private void listRemove(Session session, SessionListType listType) {
        if (!service.isAuthenticated()) {
            service.authenticate(() -> listRemove(session, listType));
        } else {
            retrieveLists();

            if (listType == SessionListType.SCHEDULED) {
                devoxxNotifications.removeScheduledSessionNotifications(session);
            }

            getList(listType).remove(session);
            showToast(listType, false);
        }
    }

    private void retrieveLists() {
        if (service.isAuthenticated()) {
            cloudLists.put(SessionListType.FAVORITES, service.retrieveFavoredSessions());
            cloudLists.put(SessionListType.SCHEDULED, service.retrieveScheduledSessions());
            usingOfflineEmptyLists = false;
        } else {
            cloudLists.put(SessionListType.FAVORITES, FXCollections.emptyObservableList());
            cloudLists.put(SessionListType.SCHEDULED, FXCollections.emptyObservableList());
            usingOfflineEmptyLists = true;
        }
    }

    private Optional<Session> findConflictingSession(Session session) {
        if (session != null) {
            for (Session s : getList(SessionListType.SCHEDULED)) {
                if (session.equals(s)) continue;

                if (s == null) {
                    if (LOGGING_ENABLED) {
                        LOG.log(Level.WARNING, String.format("Session %s is not found in the session index!", session.getSlotId()));
                    }
                } else {
                    if (isSessionOverlapping(s, session)) {
                        return Optional.of(s);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isSessionOverlapping(Session session, Session other) {
        return dateInRange(other.getEndDate(), session.getStartDate(), session.getEndDate()) ||
                dateInRange(other.getStartDate(), session.getStartDate(), session.getEndDate());
    }

    private static boolean dateInRange( ZonedDateTime dateTime, ZonedDateTime rangeStart, ZonedDateTime rangeEnd ) {
        // DEVOXX-131: Allow overlapping if one session starts right at the same time the other one ends
        return dateTime.compareTo(rangeStart) >= 0 && dateTime.compareTo(rangeEnd) < 0;
    }

    private void updateButton(ToggleButton button, boolean selected, Session session) {
        quietUpdateBtnState(button, () -> {
            button.setSelected(selected);
            button.getProperties().put(SESSION_KEY, session);
        });
    }

    private void quietUpdateBtnState(ToggleButton button, Runnable r) {
        button.setUserData(LOCK);
        r.run();
        button.setUserData(null);
    }

    private void showToast(SessionListType listType, boolean added) {
        Toast toast = new Toast();
        if (added) {
            if (listType.equals(SessionListType.FAVORITES)) {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_MARKED_AS_FAVORITE"));
            } else {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_SCHEDULED"));
            }
        } else {
            if (listType.equals(SessionListType.FAVORITES)) {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_UNFAVORITED"));
            } else {
                toast.setMessage(DevoxxBundle.getString("OTN.VISUALS.SESSION_UNSCHEDULED"));
            }
        }
        toast.show();
    }

    private Dialog<TextFlow> createSchFavDialog() {
        final Dialog<TextFlow> information = new Dialog<>();
        final Node graphicSch = MaterialDesignIcon.STAR.graphic();
        final Node graphicFav = MaterialDesignIcon.FAVORITE.graphic();
        final Text schText = new Text(SCH_FAV_DIALOG_MSG_1);
        final Text favText = new Text(SCH_FAV_DIALOG_MSG_2);
        graphicSch.getStyleClass().add("sch-dialog-icon");
        graphicFav.getStyleClass().add("fav-dialog-icon");
        schText.getStyleClass().add("text");
        favText.getStyleClass().add("text");

        final TextFlow textFlowSch = new TextFlow(schText, graphicSch);
        final TextFlow textFlowFav = new TextFlow(favText, graphicFav);
        final Label schFavText = new Label(SCH_FAV_DIALOG_MSG_INTRO);
        schFavText.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            information.hide();
        });
        information.getButtons().add(okButton);

        final VBox content = new VBox(10, schFavText, textFlowSch, textFlowFav);
        content.getStyleClass().add("sch-fav-dialog");
        information.setContent(content);
        information.setTitleText("Scheduling and Favorites");
        return information;
    }
}
