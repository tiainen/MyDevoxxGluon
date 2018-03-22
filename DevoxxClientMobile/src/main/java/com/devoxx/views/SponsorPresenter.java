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
import com.devoxx.model.Sponsor;
import com.devoxx.model.SponsorBadge;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.cell.BadgeCell;
import com.devoxx.views.helper.Placeholder;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BarcodeScanService;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableObject;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import java.util.Optional;

public class SponsorPresenter extends GluonPresenter<DevoxxApplication> {

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

    private CharmListView<SponsorBadge, String> lvBadges;
    private String name;
    private String slug;

    public void initialize() {

        sponsorView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.SPONSOR.getTitle());
            checkAndLoadView();
        });
        
        sponsorView.setOnHiding(event -> {
            sponsorView.getLayers().clear();
        });
    }

    public void setSponsor(String name, String slug) {
        this.name = name;
        this.slug = slug;
        checkAndLoadView();
    }

    @FXML
    private void signIn() {
        final GluonObservableObject<String> passwordObject = service.authenticateSponsor(password.getText());
        passwordObject.stateProperty().addListener((o, ov, nv) -> {
            if (nv == ConnectState.SUCCEEDED) {
                Services.get(SettingsService.class).ifPresent(service -> {
                    service.store(Sponsor.NAME, name);
                    service.store(Sponsor.SLUG, slug);
                });
                loadAuthenticatedView(name, slug);
                message.setText("");
                password.clear();
            } else if (nv == ConnectState.FAILED) {
                message.setText("Verification failed");
            }
        });
    }

    private void loadAuthenticatedView(String name, String slug) {
        final AppBar appBar = getApp().getAppBar();
        final Button shareButton = getApp().getShareButton();
        final Button logoutButton = MaterialDesignIcon.POWER_SETTINGS_NEW.button(e -> {
            Services.get(SettingsService.class).ifPresent(service -> {
                service.remove(Sponsor.NAME);
                service.remove(Sponsor.SLUG);
            });
            DevoxxView.SPONSORS.switchView();
        });
        appBar.getActionItems().setAll(shareButton, logoutButton);
        appBar.setTitleText("Badges for " + name);

        final ObservableList<SponsorBadge> badges = service.retrieveSponsorBadges();
        final FilteredList<SponsorBadge> filteredBadges = new FilteredList<>(badges, badge -> 
                badge != null && badge.getSlug() != null && badge.getSlug().equals(slug));
        lvBadges = new CharmListView<>();
        lvBadges.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.SPONSOR.getMenuIcon()));
        lvBadges.setCellFactory(param -> new BadgeCell<>());
        lvBadges.setItems(filteredBadges);
        sponsorView.setCenter(lvBadges);
        shareButton.disableProperty().bind(lvBadges.itemsProperty().emptyProperty());

        FloatingActionButton scan = new FloatingActionButton(MaterialDesignIcon.SCANNER.text, e -> {
            Services.get(BarcodeScanService.class).ifPresent(s -> {
                final Optional<String> scanQr = s.scan(DevoxxBundle.getString("OTN.BADGES.QR.TITLE", name), null, null);
                scanQr.ifPresent(qr -> {
                    SponsorBadge badge = new SponsorBadge(qr);
                    if (badge.getBadgeId() != null) {
                        boolean exists = false;
                        for (Badge b : filteredBadges) {
                            if (b.getBadgeId().equals(badge.getBadgeId())) {
                                Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.QR.EXISTS"));
                                toast.show();
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            badge.setSlug(slug);
                            badges.add(badge);
                            DevoxxView.BADGE.switchView().ifPresent(presenter -> ((BadgePresenter) presenter).setBadgeId(badge.getBadgeId(), slug));
                        }
                    } else {
                        Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.BAD.QR"));
                        toast.show();
                    }
                });
            });
        });
        sponsorView.getLayers().add(scan.getLayer());
    }

    private void checkAndLoadView() {
        Services.get(SettingsService.class).ifPresent(service -> {
            final String name = service.retrieve(Sponsor.NAME);
            final String sponsor = service.retrieve(Sponsor.SLUG);
            if (name != null && sponsor != null) {
                loadAuthenticatedView(name, sponsor);
            } else {
                sponsorView.setCenter(content);
                content.requestFocus();
            }
        });
    }
}
