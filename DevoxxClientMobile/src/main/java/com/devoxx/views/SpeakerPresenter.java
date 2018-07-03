/**
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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Session;
import com.devoxx.model.Speaker;
import com.devoxx.model.Talk;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.cell.ScheduleCell;
import com.devoxx.views.helper.SpeakerCard;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.BottomNavigation;
import com.gluonhq.charm.glisten.control.BottomNavigationButton;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;

public class SpeakerPresenter extends GluonPresenter<DevoxxApplication> {

    @FXML
    private View speakerView;

    @Inject
    private Service service;

    private CharmListView<Session, LocalDate> sessionsListView;

    private ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> resizeSpeakerCard();
    private ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> resizeSpeakerCard();

//    @Inject
//    private SessionVisuals sessionVisuals;

    public void initialize() {
        speakerView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.SPEAKER.getTitle());
            if(sessionsListView != null) {
                sessionsListView.setSelectedItem(null);
            }
            speakerView.widthProperty().addListener(widthListener);
            speakerView.heightProperty().addListener(heightListener);
        });

        speakerView.setOnHiding(event -> {
            speakerView.widthProperty().removeListener(widthListener);
            speakerView.heightProperty().removeListener(heightListener);
        });
    }

    public void setSpeaker(Speaker activeSpeaker) {
        if (!activeSpeaker.isDetailsRetrieved()) {
            ReadOnlyObjectProperty<Speaker> speaker = service.retrieveSpeaker(activeSpeaker.getUuid());
            if (speaker.get() != null) {
                updateWithSpeaker(speaker.get());
            } else {
                updateWithSpeaker(activeSpeaker);

                speaker.addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        updateWithSpeaker(newValue);
                    }
                });
            }
        } else {
            updateWithSpeaker(activeSpeaker);
        }
    }

    private void updateWithSpeaker(Speaker speaker) {
        // details about session (name, company, title)
        final SpeakerCard sessionCard = new SpeakerCard(speaker);
        speakerView.setTop(sessionCard);
        resizeSpeakerCard();

        // navigation between the two views (info, sessions)
        final BottomNavigation bottomNavigation = createBottomNavigation(speaker);
        speakerView.setBottom(bottomNavigation);
    }

    private BottomNavigation createBottomNavigation(final Speaker activeSpeaker) {

        BottomNavigation bottomNavigation = new BottomNavigation();

        final BottomNavigationButton infoButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.INFO"), MaterialDesignIcon.INFO.graphic(), e -> {
            // when clicked create a label in a scrollpane. Label will contain
            // the speaker summary
            Label speakerSummary = new Label(activeSpeaker.getSummary());
            speakerSummary.setWrapText(true);
            speakerSummary.getStyleClass().add("speaker-summary");
            speakerView.setCenter(createScrollPane(speakerSummary));
        });

        final BottomNavigationButton sessionsButton = new BottomNavigationButton(DevoxxBundle.getString("OTN.BUTTON.SESSIONS"), MaterialDesignIcon.EVENT_NOTE.graphic(), e -> {
            // when clicked we create a pane containing all sessions.
            speakerView.setCenter(createSessionsListView(activeSpeaker));
        });

        bottomNavigation.getActionItems().addAll(infoButton, sessionsButton);
        infoButton.fire();

        return bottomNavigation;
    }
    
    
    private ObservableList<Session> fetchSessions(Speaker activeSpeaker) {
        ObservableList<Session> speakerSessions = FXCollections.observableArrayList();
        if (activeSpeaker.getAcceptedTalks() != null) {
            List<Session> sessions = service.retrieveSessions();
            List<Talk> acceptedTalks = activeSpeaker.getAcceptedTalks();
            for (Talk acceptedTalk : acceptedTalks) {
                for (Session session : sessions) {
                    if (acceptedTalk.getId().equals(session.getTalk().getId())) {
                        speakerSessions.add(session);
                    }
                }
            }
        }
        return speakerSessions;
    }
    
    private CharmListView<Session, LocalDate> createSessionsListView(Speaker activeSpeaker) {
        sessionsListView = new CharmListView<>(fetchSessions(activeSpeaker));
        sessionsListView.getStyleClass().add("sessions-list");
        sessionsListView.setCellFactory(p -> new ScheduleCell(service, true));
        sessionsListView.setPlaceholder(new Label(DevoxxBundle.getString("OTN.SPEAKER.THERE_ARE_NO_SESSIONS")));
        return sessionsListView;
    }
    
    private ScrollPane createScrollPane(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        return sp;
    }

    private void resizeSpeakerCard() {
        if (speakerView.getTop() != null) {
            SpeakerCard speakerCard = (SpeakerCard) speakerView.getTop();
            speakerCard.setMaxHeight(speakerView.getHeight() / 2.5);
            speakerCard.requestLayout();
        }
    }
    
}
