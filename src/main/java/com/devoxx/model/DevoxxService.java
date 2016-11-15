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

import com.gluonhq.charm.glisten.afterburner.GluonView;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.RestClient;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.logging.Logger;

public class DevoxxService extends BaseService {

    private static final Logger LOG = Logger.getLogger(DevoxxService.class.getName());

    private static final String DEVOXX_HOST = "http://cfp.devoxx.be/api/conferences/DV16";

    private ReadOnlyListWrapper<Session> sessions;
    private ReadOnlyListWrapper<Speaker> speakers;

    public DevoxxService() {
        sessions = new ReadOnlyListWrapper<>(retrieveSessionsInternal());
        speakers = new ReadOnlyListWrapper<>(retrieveSpeakersInternal());
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
            if(newValue != null) {
                for (Link link : newValue.getLinks()) {
                    if (link.getHref() != null || !link.getHref().isEmpty()) {
                        fillAllSessionsOfDay(sessions, link);
                    }
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
        scheduleOfDay.addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
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
        return null;
    }

    @Override
    public ObservableList<Session> internalRetrieveScheduledSessions(Runnable onStateSucceeded) {
        return null;
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
    public void retrieveSurveyAnswers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PushNotification retrievePushNotification() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<Venue> retrieveVenues() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void storeSurveyAnswers(String answers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSurveyCompleted() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty<EnabledOTNExperiences> retrieveEnabledOTNExperiences() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<OTNCoffee> retrieveOTNCoffees() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OTNCoffeeOrder orderOTNCoffee(OTNCoffee coffee, int strength) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OTNCarvedBadgeOrder orderOTNCarveABadge(String shape) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<OTNGame> retrieveOTNGames() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<OTNEmbroidery> retrieveOTNEmbroideries() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyListProperty<OTN3DModel> retrieveOTN3DModels() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canVoteForOTN3DModel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty<LatestClearThreeDModelVotes> retrieveLatestClearVotes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void voteForOTN3DModel(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
