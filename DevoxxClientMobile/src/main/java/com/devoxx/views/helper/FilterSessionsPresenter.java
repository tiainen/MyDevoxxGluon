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
package com.devoxx.views.helper;

import com.devoxx.DevoxxApplication;
import com.devoxx.filter.TimePeriod;
import com.devoxx.model.Conference;
import com.devoxx.model.ProposalType;
import com.devoxx.model.Session;
import com.devoxx.model.Track;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.function.Predicate;

import static com.devoxx.DevoxxApplication.POPUP_FILTER_SESSIONS_MENU;

public class FilterSessionsPresenter extends GluonPresenter<DevoxxApplication> {

    private static final TimePeriod DEFAULT_TIME_PERIOD = TimePeriod.MORE_THAN_ONE_HOUR_AGO;

    @FXML private VBox dayFilter;

    @FXML private VBox trackFilter;

    @FXML private VBox typeFilter;

    @FXML private VBox timePeriodFilter;

    @FXML private Tab tabDay;
    @FXML private Tab tabTrack;
    @FXML private Tab tabType;
    @FXML private Tab tabTimePeriod;

    @FXML private HBox buttonContainer;
    @FXML private Button applyButton;
    @FXML private Button resetButton;

    private ToggleGroup radioButtonGroup;
    private VBox periodRadioBtContainer;
    private CheckBox periodAll;
    private TimePeriod selectedTimePeriod = DEFAULT_TIME_PERIOD;

    @Inject
    private Service service;

    // A list to temporarily keep checkboxes whose selectedProperty has changed
    private final ObservableList<CheckBox> draftFilter = FXCollections.observableArrayList();

    public void initialize() {
        radioButtonGroup = new ToggleGroup();

        tabDay.setText(DevoxxBundle.getString("OTN.FILTER.TABDAY"));
        tabTrack.setText(DevoxxBundle.getString("OTN.FILTER.TABTRACK"));
        tabType.setText(DevoxxBundle.getString("OTN.FILTER.TABTYPE"));
        tabTimePeriod.setText(DevoxxBundle.getString("OTN.FILTER.TABTIMEPERIOD"));

        ObservableList<Track> tracks = service.retrieveTracks();
        for (Track track : tracks) {
            addTrackCheckBox(track);
        }
        tracks.addListener((ListChangeListener<Track>) c -> {
            while (c.next()) {
                for (Track track : c.getRemoved()) {
                    for (Iterator<Node> iterator = trackFilter.getChildren().iterator(); iterator.hasNext();) {
                        Node node = iterator.next();
                        if (((Track) node.getUserData()).getId().equals(track.getId())) {
                            iterator.remove();
                        }
                    }
                }
                for (Track track : c.getAddedSubList()) {
                    addTrackCheckBox(track);
                }
                updateIsFilterApplied();
            }
        });

        ObservableList<ProposalType> proposalTypes = service.retrieveProposalTypes();
        for (ProposalType proposalType : proposalTypes) {
            addProposalTypeCheckBox(proposalType);
        }
        proposalTypes.addListener((ListChangeListener<ProposalType>) c -> {
            while (c.next()) {
                for (ProposalType type : c.getRemoved()) {
                    for (Iterator<Node> iterator = typeFilter.getChildren().iterator(); iterator.hasNext();) {
                        Node node = iterator.next();
                        if (((ProposalType) node.getUserData()).getId().equals(type.getId())) {
                            iterator.remove();
                        }
                    }
                }
                for (ProposalType type : c.getAddedSubList()) {
                    addProposalTypeCheckBox(type);
                }
                updateIsFilterApplied();
            }
        });

        // TimePeriod Tab
        periodAll = new CheckBox(TimePeriod.ALL.toString());
        periodAll.setUserData(TimePeriod.ALL);
        periodAll.selectedProperty().addListener( (observable, oldValue, newValue) -> {
            for (Node child : periodRadioBtContainer.getChildren()) {
                child.setDisable(periodAll.isSelected());
            }
            if (periodAll.isSelected()) {
                selectedTimePeriod = TimePeriod.ALL;
            } else {
                selectedTimePeriod = (TimePeriod) radioButtonGroup.getSelectedToggle().getUserData();
            }
            updateIsFilterApplied();
        });
        timePeriodFilter.getChildren().add(periodAll);
        periodRadioBtContainer = new VBox();
        Label radioBtHeader = new Label(DevoxxBundle.getString("OTN.FILTER.TIME_PERIOD.HIDE_HEADER"));
        periodRadioBtContainer.getChildren().add(radioBtHeader);
        periodRadioBtContainer.getStyleClass().add("period-radio-btn-container");
        timePeriodFilter.getChildren().add(periodRadioBtContainer);
        for (TimePeriod timePeriod : TimePeriod.values()) {
            if (timePeriod == TimePeriod.ALL) {
                continue;
            }
            RadioButton radioButton = new RadioButton(timePeriod.toString());
            radioButton.setUserData(timePeriod);
            radioButton.setToggleGroup(radioButtonGroup);
            radioButton.setOnAction(event -> {
                selectedTimePeriod = (TimePeriod) radioButton.getUserData();
                updateIsFilterApplied();
            });
            if (timePeriod == DEFAULT_TIME_PERIOD) {
                radioButton.setSelected(true);
            }
            periodRadioBtContainer.getChildren().add(radioButton);
        }
        if (service.getConference() != null) {
            updateTimePeriodSelection();
            resetPeriodUI();
        }
        service.conferenceProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateTimePeriodSelection();
                resetPeriodUI();
            }
        });

        if (service.getConference() != null) {
            updateDayFilter(service.getConference());
        }
        service.conferenceProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                updateDayFilter(nv);
            }
        });

        applyButton.setText(DevoxxBundle.getString("OTN.BUTTON.APPLY"));
        resetButton.setText(DevoxxBundle.getString("OTN.FILTER.BUTTON.RESET"));

        showing.addListener((observable, oldValue, newValue) -> {
            if(!newValue) {
                for (CheckBox checkBox : draftFilter) {
                    checkBox.setSelected(!checkBox.isSelected());
                }
                draftFilter.clear();
            }
        });

        buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
        BorderPane.setAlignment(buttonContainer, Pos.BOTTOM_RIGHT);

        updateSearchPredicate();
    }

    private void updateTimePeriodSelection() {
        Conference conference = service.getConference();
        ZonedDateTime now = ZonedDateTime.now(conference.getConferenceZoneId());
        if (conference.getFromDateTime().isAfter(now) || conference.getEndDateTime().isBefore(now)) {
            selectedTimePeriod = TimePeriod.ALL;
        } else {
            selectedTimePeriod = DEFAULT_TIME_PERIOD;
        }
    }

    private void resetPeriodUI() {
        if (selectedTimePeriod == TimePeriod.ALL) {
            periodAll.setSelected(true);
        } else {
            periodAll.setSelected(false);
            for (Node child : periodRadioBtContainer.getChildren()) {
                if (child.getUserData() == selectedTimePeriod) {
                    ((RadioButton) child).setSelected(true);
                    return;
                }
            }
        }
    }

    private final ObjectProperty<Predicate<Session>> searchPredicateProperty = new SimpleObjectProperty<>();
    public final ObjectProperty<Predicate<Session>> searchPredicateProperty() {
        return searchPredicateProperty;
    }

    private final BooleanProperty isFilterApplied = new SimpleBooleanProperty();
    public final BooleanProperty filterAppliedProperty() {
        return isFilterApplied;
    }
    public final boolean isFilterApplied() { return isFilterApplied.get(); }

    private final BooleanProperty showing = new SimpleBooleanProperty();
    public final BooleanProperty showingProperty() {
        return showing;
    }

    @FXML
    private void addToFilter(ActionEvent actionEvent) {
        draftFilter.add((CheckBox) actionEvent.getSource());
    }

    @FXML
    private void apply(ActionEvent actionEvent) {
        apply();
    }

    private void addTrackCheckBox(Track track) {
        CheckBox cbTrack = new CheckBox(track.getName());
        cbTrack.setUserData(track);
        cbTrack.setOnAction(this::addToFilter);
        trackFilter.getChildren().add(cbTrack);
    }

    private void addProposalTypeCheckBox(ProposalType proposalType) {
        CheckBox cbProposalType = new CheckBox(proposalType.getLabel());
        cbProposalType.setUserData(proposalType);
        cbProposalType.setOnAction(this::addToFilter);
        typeFilter.getChildren().add(cbProposalType);
    }

    private void apply() {
        // update predicate with new search rules
        updateSearchPredicate();
        updateIsFilterApplied();
        draftFilter.clear();
        MobileApplication.getInstance().hideLayer(POPUP_FILTER_SESSIONS_MENU);
    }

    private void updateDayFilter(Conference conference) {
        DateTimeFormatter formatter = DevoxxSettings.DATE_FORMATTER;
        dayFilter.getChildren().clear();
        for (int i = 0; i < conference.getDays().length; i++) {
            CheckBox cbDay = new CheckBox(formatter.format(conference.getDays()[i]));
            cbDay.setUserData(i + 1);
            cbDay.setOnAction(this::addToFilter);
            dayFilter.getChildren().add(cbDay);
        }

        updateIsFilterApplied();
    }

    @FXML
    private void reset(ActionEvent actionEvent) {
        for (Node node : dayFilter.getChildren()) {
            ((CheckBox) node).setSelected(false);
        }

        for (Node node : trackFilter.getChildren()) {
            ((CheckBox) node).setSelected(false);
        }

        for (Node node : typeFilter.getChildren()) {
            ((CheckBox) node).setSelected(false);
        }
        resetPeriodUI();

        apply();
    }

    private boolean checkDay(Session session) {
        for (Node node : dayFilter.getChildren()) {
            if (((CheckBox) node).isSelected() && service.getConference().getConferenceDayIndex(session.getStartDate()) == (Integer) node.getUserData()) {
                return true;
            }
        }
        return false;
    }

    private boolean considerDayFilter() {
        return considerAnyCheckBoxSelected(dayFilter);
    }

    private boolean checkTrack(Session session) {
        if (session.getTalk() == null || session.getTalk().getTrackId() == null) {
            return false;
        }

        for (Node node : trackFilter.getChildren()) {
            if (((CheckBox) node).isSelected() && session.getTalk().getTrackId().equals(((Track) node.getUserData()).getName())) {
                return true;
            }
        }
        return false;
    }


    private boolean considerTrackFilter() {
        return considerAnyCheckBoxSelected(trackFilter);
    }

  
    private boolean checkType(Session session) {
        if (session.getTalk() == null || session.getTalk().getTalkType() == null) {
            return false;
        }

        for (Node node : typeFilter.getChildren()) {
            if (((CheckBox) node).isSelected() && session.getTalk().getTalkType().equals(((ProposalType) node.getUserData()).getLabel())) {
                return true;
            }
        }
        return false;
    }

    private boolean considerTypeFilter() {
        return considerAnyCheckBoxSelected(typeFilter);
    }

    private boolean checkPeriod(Session session) {
        return TimePeriod.isSessionWithinPeriod(session, selectedTimePeriod);
    }

    private boolean considerPeriodFilter() {
        return selectedTimePeriod != TimePeriod.ALL;
    }

    private void updateSearchPredicate() {
        searchPredicateProperty.set(session ->
                session != null &&
                (!considerDayFilter() || checkDay(session)) &&
                (!considerTrackFilter() || checkTrack(session)) &&
                (!considerTypeFilter() || checkType(session)) &&
                checkPeriod(session)
        );
    }



    private void updateIsFilterApplied() {
        isFilterApplied.set(considerDayFilter() || considerTrackFilter() || considerTypeFilter()
                || considerPeriodFilter());
    }

    private boolean considerAnyCheckBoxSelected(VBox container) {
        for (Node node : container.getChildren()) {
            if (((CheckBox) node).isSelected()) return true;
        }
        return false;
    }
}
