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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.control.CircularSelector;
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxCountry;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.StatusBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.connect.GluonObservableList;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import javax.inject.Inject;
import java.net.URL;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.devoxx.util.DevoxxSettings.LOCALE;

public class ConfSelectorPresenter extends GluonPresenter<DevoxxApplication> {

    private static final String CONF_CIRCLE_NAME = "circle.png";

    private static final DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("dd", LOCALE);
    private static final DateTimeFormatter DATE_FORMATTER_LONG = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
    private static final StatusBar STATUS_BAR = MobileApplication.getInstance().getStatusBar();
    private static final PseudoClass PSEUDO_CLASS_STATUS_CONF = PseudoClass.getPseudoClass("conf");

    private InvalidationListener selectNearestConferenceListener, selectSavedConferenceListener;
    private CircularSelector<Conference> selector = new CircularSelector<>(item -> {

        Image image;
        if (item == null) {
            image = new Image(ConfSelectorPresenter.class.getResource(CONF_CIRCLE_NAME).toExternalForm());
        } else {
            URL imageSource = ConfSelectorPresenter.class.getResource("splash_btn_" + 
                    DevoxxCountry.getConfShortName(item.getCountry()).toLowerCase(Locale.ROOT) + ".png");
            if (imageSource != null) {
                image = new Image(imageSource.toExternalForm());
            } else {
                image = new Image(item.getConfIcon(), true);
            }
        }

        ImageView iv = new ImageView(image);
        if (item != null) {
            double size = 30 * 2; // TODO: base it on the component size
            iv.setPreserveRatio(true);
            iv.setFitWidth(size);
        }
        return iv;
    });

    @FXML
    private ResourceBundle bundle = ResourceBundle.getBundle("com/devoxx/views/confselector");
    @FXML
    private BorderPane content;
    @FXML
    private View confSelectorView;
    @FXML
    private Label countryLabel;
    @FXML
    private Button countryButton;
    @FXML
    private Label dateLabel;
    @FXML
    private Label venueLabel;
    @FXML
    private Label daysLabel;
    @FXML
    private Label talksLabel;
    @FXML
    private Label capacityLabel;

    @Inject
    private Service service;

    public void initialize() {
        GluonObservableList<Conference> conferences = service.retrieveConferences();

        countryButton.setMaxWidth(Double.MAX_VALUE);
        Bindings.bindContent(selector.getItems(), conferences);

        selector.selectedItemProperty().addListener(e -> {
            Conference c = selector.getSelectedItem();

            // TODO: add the splash image somehow as the background image over the entire view??
            // ImageView background = new ImageView(new Image(c.getSplashImgURL()));

            countryLabel.setText(c.getCountry());

            dateLabel.setText(MessageFormat.format(bundle.getString("OTN.CONFERENCE_SELECTOR.INFO.DATE"),
                    c.getStartDate().format(DATE_FORMATTER_SHORT), c.getEndDate().format(DATE_FORMATTER_LONG)));

            long days = c.getDaysUntilStart();
            daysLabel.setText(days > 0 ? String.valueOf(days) : "0");
            talksLabel.setText(c.getSessions());
            capacityLabel.setText(c.getCapacity());
            venueLabel.setText(c.getVenue() + ", " + c.getCountry());
        });

        Optional<SettingsService> settingsService = Services.get(SettingsService.class);
        // If saved conference is found select it else select the nearest conference
        if (settingsService.isPresent()) {
            SettingsService service = settingsService.get();
            String savedConference = service.retrieve(DevoxxSettings.SAVED_CONFERENCE_ID);
            if (savedConference == null || savedConference.isEmpty()) {
                selectNearestConferenceFrom(conferences);
            } else {
                selectSavedConferenceFrom(conferences, savedConference);
            }
        } else {
            selectNearestConferenceFrom(conferences);
        }

        confSelectorView.setOnShowing(event -> {
            getApp().getAppBar().setVisible(false);
            content.setCenter(selector);
            // Add pseudo-class to status-bar
            STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_CONF, true);
        });

        // Remove pseudo-class from status-bar
        confSelectorView.setOnHiding(event -> STATUS_BAR.pseudoClassStateChanged(PSEUDO_CLASS_STATUS_CONF, false));
    }

    @FXML
    public void selectCountry() {
        if (selector.getSelectedItem().equals(service.getConference())) {
            DevoxxView.SESSIONS.switchView();
        } else {
            service.setConference(selector.getSelectedItem());
            Services.get(SettingsService.class).ifPresent(settingsService -> settingsService.store(DevoxxSettings.SAVED_CONFERENCE_ID, selector.getSelectedItem().getId()));
        }
    }

    private void selectSavedConferenceFrom(GluonObservableList<Conference> conferences, String configuredConference) {
        if (conferences.isInitialized()) {
            selectSavedConference(conferences, configuredConference);
        } else {
            selectSavedConferenceListener = o -> selectSavedConference(conferences, configuredConference);
            conferences.initializedProperty().addListener(selectSavedConferenceListener);
        }
    }

    private void selectSavedConference(GluonObservableList<Conference> conferences, String configuredConference) {
        for (Conference conference : conferences) {
            if (conference.getId().equals(configuredConference)) {
                selector.setSelectedItem(conference);
                break;
            }
        }
        if (selectSavedConferenceListener != null) {
            conferences.initializedProperty().removeListener(selectSavedConferenceListener);
        }
    }

    private void selectNearestConferenceFrom(GluonObservableList<Conference> conferences) {
        if (conferences.isInitialized()) {
            selectNearestConference(conferences);
        } else {
            selectNearestConferenceListener = o -> selectNearestConference(conferences);
            conferences.initializedProperty().addListener(selectNearestConferenceListener);
        }
    }

    private void selectNearestConference(GluonObservableList<Conference> conferences) {
        if (conferences.size() == 0) return;
        long minPositiveDaysUntil = Long.MAX_VALUE;
        long minNegativeDaysUntil = Long.MIN_VALUE;
        Conference nearestFuture = null;
        Conference nearestPast = conferences.get(0);
        for (Conference conference : conferences) {
            long days = conference.getDaysUntilEnd();
            if (days >= 0) {
                if (days < minPositiveDaysUntil) {
                    minPositiveDaysUntil = days;
                    nearestFuture = conference;
                }
            } else {
                if (days > minNegativeDaysUntil) {
                    minNegativeDaysUntil = days;
                    nearestPast = conference;
                }
            }
        }
        if (nearestFuture != null) {
            selector.setSelectedItem(nearestFuture);
        } else {
            selector.setSelectedItem(nearestPast);
        }
        if (selectNearestConferenceListener != null) {
            conferences.initializedProperty().removeListener(selectNearestConferenceListener);
        }
    }
}
