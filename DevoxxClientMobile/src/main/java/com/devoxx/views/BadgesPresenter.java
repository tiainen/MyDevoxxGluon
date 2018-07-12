/*
 * Copyright (c) 2017, 2018 Gluon Software
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
import com.devoxx.model.Badge;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.BadgeCell;
import com.devoxx.views.helper.LoginPrompter;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BarcodeScanService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BadgesPresenter extends GluonPresenter<DevoxxApplication> {
    
    private static final String ANONYMOUS_MESSAGE = DevoxxBundle.getString("OTN.BADGES.ANONYMOUS_MESSAGE");
    private static final String EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.BADGES.EMPTY_LIST_MESSAGE");

    @FXML
    private View badgesView;

    @FXML
    private VBox content;

    @FXML
    private Button sponsor;

    @FXML
    private Button attendee;
    
    @Inject
    private Service service;
    
    private CharmListView<Badge, String> lvBadges;
    private FloatingActionButton scan;

    public void initialize() {
        lvBadges = new CharmListView<>();
        lvBadges.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.BADGES.getMenuIcon()));
        lvBadges.setCellFactory(param -> new BadgeCell<>());
        
        badgesView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.BADGES.getTitle());
            
            sponsor.setOnAction(e -> showSponsor());
            attendee.setOnAction(e -> authenticate());

            loadContent();
        });
        
        badgesView.setOnHiding(event -> {
            if (scan != null) {
                scan.hide();
            }
        });
    }

    private List<MenuItem> getMenuItems() {
        final MenuItem scanAsDifferentUser = new MenuItem("Scan as Different User");
        scanAsDifferentUser.setOnAction(ev -> reload());
        return Collections.singletonList(scanAsDifferentUser);
    }

    private void reload() {
        Util.removeKeysFromSettings(DevoxxSettings.BADGE_TYPE, DevoxxSettings.SPONSOR_NAME, DevoxxSettings.SPONSOR_SLUG);
        loadContent();
    }

    private void loadContent() {
        if (scan != null) {
            scan.hide();
        }
        getApp().getAppBar().getMenuItems().clear();
        final String badgeType = loadPreviousSelection();
        if (badgeType == null) {
            badgesView.setCenter(content);
        } else {
            switch (badgeType) {
                case DevoxxSettings.BADGE_TYPE_SPONSOR:
                    showSponsor();
                    break;
                case DevoxxSettings.BADGE_TYPE_ATTENDEE:
                    authenticate();
                    break;
            }
        }
    }

    private void authenticate() {
        getApp().getAppBar().getMenuItems().setAll(getMenuItems());
        getApp().getAppBar().setTitleText(DevoxxBundle.getString("OTN.VIEW.ATTENDEE"));
        if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
            loadAuthenticatedView();
        } else {
            loadAnonymousView();
        }
    }

    private String loadPreviousSelection() {
        final Optional<SettingsService> settingsService = Services.get(SettingsService.class);
        if (settingsService.isPresent()) {
            final SettingsService service = settingsService.get();
            return service.retrieve(DevoxxSettings.BADGE_TYPE);
        }
        return null;
    }

    private void loadAnonymousView() {
        Util.showToast(DevoxxBundle.getString("OTN.BADGES.SELECT.ATTENDEE"), Duration.seconds(5));
        badgesView.setCenter(new LoginPrompter(service, ANONYMOUS_MESSAGE, DevoxxView.BADGES.getMenuIcon(), this::loadAuthenticatedView));
    }

    private void loadAuthenticatedView() {
        Services.get(SettingsService.class).ifPresent(service -> {
            service.store(DevoxxSettings.BADGE_TYPE, DevoxxSettings.BADGE_TYPE_ATTENDEE);
        });
        final ObservableList<MenuItem> menuItems = getApp().getAppBar().getMenuItems();
        if (menuItems.size() > 0) {
            menuItems.get(0).setText("Logout");
        }
        Util.showToast(DevoxxBundle.getString("OTN.BADGES.LOGIN.ATTENDEE"), Duration.seconds(5));
        showAttendee();
    }

    private void showAttendee() {
        final ObservableList<Badge> badges = service.retrieveBadges();
        lvBadges.setItems(badges);
        badgesView.setCenter(lvBadges);

        final Button shareButton = getApp().getShareButton(DevoxxSettings.BADGE_TYPE_ATTENDEE);
        shareButton.disableProperty().bind(lvBadges.itemsProperty().emptyProperty());
        getApp().getAppBar().getActionItems().setAll(getApp().getSearchButton(), shareButton);

        if (scan == null) {
            scan = new FloatingActionButton("", e -> {
                Services.get(BarcodeScanService.class).ifPresent(s -> {
                    final Optional<String> scanQr = s.scan(DevoxxBundle.getString("OTN.BADGES.ATTENDEE.QR.TITLE"), null, null);
                    scanQr.ifPresent(qr -> {
                        Badge badge = new Badge(qr);
                        if (badge.getBadgeId() != null) {
                            boolean exists = false;
                            for (Badge b : badges) {
                                if (b.getBadgeId().equals(badge.getBadgeId())) {
                                    Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.QR.EXISTS"));
                                    toast.show();
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                lvBadges.itemsProperty().add(badge);
                                DevoxxView.BADGE.switchView().ifPresent(presenter -> ((BadgePresenter) presenter).setBadgeId(badge.getBadgeId(), null));
                            }
                        } else {
                            Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.BAD.QR"));
                            toast.show();
                        }
                    });
                });
            });
            scan.getStyleClass().add("badge-scanner");
        }
        scan.show();
    }

    private void showSponsor() {
        final Optional<SettingsService> settingsService = Services.get(SettingsService.class);
        if (settingsService.isPresent()) {
            SettingsService service = settingsService.get();
            final String sponsorName = service.retrieve(DevoxxSettings.SPONSOR_NAME);
            final String sponsorSlug = service.retrieve(DevoxxSettings.SPONSOR_SLUG);
            if (sponsorSlug == null) {
                DevoxxView.SPONSORS.switchView();
            } else {
                DevoxxView.SPONSOR.switchView().ifPresent(presenter -> ((SponsorPresenter) presenter).setSponsor(sponsorName, sponsorSlug));
            }
        }
    }
}
