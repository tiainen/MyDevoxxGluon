/**
 * Copyright (c) 2017, Gluon Software
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
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.views.helper.Placeholder;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BarcodeScanService;
import com.gluonhq.charm.down.plugins.ShareService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.layout.layer.FloatingActionButton;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javax.inject.Inject;

public class BadgesPresenter extends GluonPresenter<DevoxxApplication> {
    private static final Logger LOG = Logger.getLogger(BadgesPresenter.class.getName());
    private static final String ANONYMOUS_MESSAGE = DevoxxBundle.getString("OTN.BADGES.ANONYMOUS_MESSAGE");
    private static final String EMPTY_LIST_MESSAGE = DevoxxBundle.getString("OTN.BADGES.EMPTY_LIST_MESSAGE");

    @Inject
    private Service service;

    @FXML
    private View badgesView;

    private CharmListView<Badge, String> lvBadges;

    public void initialize() {
        Button shareButton = MaterialDesignIcon.SHARE.button(e -> {
            Services.get(ShareService.class).ifPresent(s -> {
                File root = Services.get(StorageService.class).flatMap(storage -> storage.getPublicStorage("Documents")).orElse(null);
                if (root != null) {
                    File file = new File(root, "DevoxxBE-badges.csv");
                    if (file.exists()) {
                        file.delete();
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write("First Name,Last Name,Company,Email,Details");
                        writer.newLine();
                        for (Badge badge : service.retrieveBadges()) {
                            writer.write(badge.toCSV());
                            writer.newLine();
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.WARNING, "Error writing csv file ", ex);
                    }
                    s.share(DevoxxBundle.getString("OTN.BADGES.SHARE.SUBJECT"), 
                            DevoxxBundle.getString("OTN.BADGES.SHARE.MESSAGE", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.now())), 
                            "text/plain", file);
                } else {
                    LOG.log(Level.WARNING, "Error accessing local storage");
                }
            });
        });
        lvBadges = new CharmListView<>();
        lvBadges.setPlaceholder(new Placeholder(EMPTY_LIST_MESSAGE, DevoxxView.BADGES.getMenuIcon()));
        lvBadges.setCellFactory(param -> new BadgeCell());
        shareButton.disableProperty().bind(lvBadges.itemsProperty().emptyProperty());

        badgesView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.BADGES.getTitle());
            appBar.getActionItems().addAll(getApp().getSearchButton(), shareButton);
            
            if (service.isAuthenticated() || !DevoxxSettings.USE_REMOTE_NOTES) {
                loadAuthenticatedView();
            } else {
                loadAnonymousView();
            }
        });
    }

    private void loadAnonymousView() {
        badgesView.setCenter(new LoginPrompter(service, ANONYMOUS_MESSAGE, DevoxxView.BADGES.getMenuIcon(), this::loadAuthenticatedView));
    }

    private void loadAuthenticatedView() {
        final ObservableList<Badge> badges = service.retrieveBadges();
        lvBadges.setItems(badges);
        badgesView.setCenter(lvBadges);
        
        FloatingActionButton scan = new FloatingActionButton(MaterialDesignIcon.SCANNER.text, e -> {
            Services.get(BarcodeScanService.class).ifPresent(s -> {
                final Optional<String> scanQr = s.scan(); 
                scanQr.ifPresent(qr -> {
                    Badge badge = new Badge(qr);
                    if (badge.getBadgeId() != null) {
                        boolean exists = false;
                        for (Badge b : service.retrieveBadges()) {
                            if (b.getBadgeId().equals(badge.getBadgeId())) {
                                Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.QR.EXISTS"));
                                toast.show();
                                exists = true;
                                break;
                            }
                        }
                        if (! exists) {
                            lvBadges.itemsProperty().add(badge);
                            DevoxxView.BADGE.switchView().ifPresent(presenter -> ((BadgePresenter) presenter).setBadgeId(badge.getBadgeId()));
                        }
                    } else {
                        Toast toast = new Toast(DevoxxBundle.getString("OTN.BADGES.BAD.QR"));
                        toast.show();
                    }
                });
            });
        });
        badgesView.getLayers().add(scan.getLayer());
    }

}
