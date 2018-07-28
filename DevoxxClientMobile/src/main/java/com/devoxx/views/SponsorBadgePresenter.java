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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Badge;
import com.devoxx.model.BadgeType;
import com.devoxx.model.Sponsor;
import com.devoxx.model.SponsorBadge;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.cell.BadgeCell;
import com.devoxx.views.helper.Placeholder;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BarcodeScanService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.connect.GluonObservableObject;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Optional;

public class SponsorBadgePresenter extends GluonPresenter<DevoxxApplication> {

    private static final String EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.BADGES.EMPTY_LIST_MESSAGE");

    @FXML
    private View sponsorView;
    
    @FXML
    private VBox content;

    @FXML
    private PasswordField password;

    @FXML
    private Label message;
    
    @Inject
    private Service service;

    private Sponsor sponsor;
    private FloatingActionButton scan;
    private CharmListView<SponsorBadge, String> sponsorBadges;

    public void initialize() {

        sponsorView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavBackButton());
            appBar.setTitleText(DevoxxView.SPONSOR_BADGE.getTitle());
            checkAndLoadView();
        });
        
        sponsorView.setOnHidden(event -> {
            if (scan != null) {
                scan.hide();
            }
        });
    }

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;
        checkAndLoadView();
    }

    @FXML
    private void signIn() {
        message.setText("");
        message.setGraphic(new ProgressIndicator());
        final GluonObservableObject<String> passwordObject = service.authenticateSponsor();
        passwordObject.setOnFailed(e -> {
            message.setGraphic(null);
            message.setText(DevoxxBundle.getString("OTN.SPONSOR.VERIFICATION.FAILED"));
        });
        passwordObject.setOnSucceeded(e -> {
            message.setGraphic(null);
            if (password.getText().equals(passwordObject.get())) {
                Services.get(SettingsService.class).ifPresent(service -> {
                    service.store(DevoxxSettings.BADGE_TYPE, BadgeType.SPONSOR.toString());
                    service.store(DevoxxSettings.BADGE_SPONSOR, sponsor.toCSV());
                });
                final Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.LOGIN.SPONSOR", sponsor.getName()), Toast.LENGTH_LONG);
                toast.show();
                loadAuthenticatedView(sponsor);
                password.clear();
            } else {
                message.setText(DevoxxBundle.getString("OTN.SPONSOR.INCORRECT.PASSWORD"));
            } 
        });
    }

    private void loadAuthenticatedView(Sponsor sponsor) {
        
        final ObservableList<SponsorBadge> badges = service.retrieveSponsorBadges(sponsor);
        final FilteredList<SponsorBadge> filteredBadges = new FilteredList<>(badges, badge -> {
            return badge != null && badge.getSponsor() != null && badge.getSponsor().equals(sponsor);
        });
        
        if (sponsorBadges == null) {
            sponsorBadges = new CharmListView<>();
            sponsorBadges.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.SPONSOR_BADGE.getMenuIcon()));
            sponsorBadges.setCellFactory(param -> new BadgeCell<>());
            sponsorBadges.setItems(filteredBadges);
        }
        sponsorView.setCenter(sponsorBadges);

        final Button shareButton = getApp().getShareButton(BadgeType.SPONSOR, sponsor);
        final AppBar appBar = getApp().getAppBar();
        appBar.setTitleText(DevoxxBundle.getString("OTN.SPONSOR.BADGES.FOR", sponsor.getName()));
        appBar.setNavIcon(getApp().getNavMenuButton());
        appBar.getActionItems().setAll(shareButton);
        appBar.getMenuItems().addAll(getBadgeChangeMenuItem("Logout"));
        
        shareButton.disableProperty().bind(sponsorBadges.itemsProperty().emptyProperty());

        if (scan == null) {
            scan = new FloatingActionButton("", e -> {
                Services.get(BarcodeScanService.class).ifPresent(s -> {
                    final Optional<String> scanQr = s.scan(DevoxxBundle.getString("OTN.BADGES.SPONSOR.QR.TITLE", sponsor.getName()), null, null);
                    scanQr.ifPresent(qr -> {
                        SponsorBadge badge = new SponsorBadge(qr);
                        if (badge.getBadgeId() != null) {
                            boolean exists = false;
                            for (Badge b : filteredBadges) {
                                if (b != null && b.getBadgeId() != null && b.getBadgeId().equals(badge.getBadgeId())) {
                                    Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.QR.EXISTS"));
                                    toast.show();
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                badge.setSponsor(sponsor);
                                badges.add(badge);
                                DevoxxView.BADGE.switchView().ifPresent(presenter -> ((BadgePresenter) presenter).setBadge(badge, BadgeType.SPONSOR));
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

    private void checkAndLoadView() {
        Services.get(SettingsService.class).ifPresent(service -> {
            final Sponsor sponsor = Sponsor.fromCSV(service.retrieve(DevoxxSettings.BADGE_SPONSOR));
            // if found, no need to prompt password
            if (sponsor != null && sponsor.equals(this.sponsor)) {
                loadAuthenticatedView(sponsor);
            } else {
                if (scan != null) {
                    scan.hide();
                }
                // prompt password
                sponsorView.setCenter(content);
                content.requestFocus();
            }
        });
    }

    private MenuItem getBadgeChangeMenuItem(String text) {
        final MenuItem scanAsDifferentUser = new MenuItem(text);
        scanAsDifferentUser.setOnAction(e -> {
            Util.removeKeysFromSettings(DevoxxSettings.BADGE_TYPE, DevoxxSettings.BADGE_SPONSOR);
            DevoxxView.BADGES.switchView();
        });
        return scanAsDifferentUser;
    }
    
}
