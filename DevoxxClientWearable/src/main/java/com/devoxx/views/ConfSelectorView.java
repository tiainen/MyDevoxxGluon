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

import com.devoxx.GluonWearable;
import static com.devoxx.GluonWearable.SCHEDULE_VIEW;
import com.gluonhq.charm.glisten.animation.BounceInRightTransition;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.control.CircularSelector;
import com.devoxx.model.Conference;
import com.devoxx.model.WearableModel;
import com.devoxx.util.DevoxxCountry;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ConfSelectorView extends View {

    private final double watchFactor = 0.6;
    
    private final CircularSelector<Conference> selector = new CircularSelector<>(item -> {

        Image image;
        if (item == null) {
            image = new Image(ConfSelectorView.class.getResource("circle.png").toExternalForm());
        } else {
            URL imageSource = ConfSelectorView.class.getResource("splash_btn_" + 
                    DevoxxCountry.getConfShortName(item.getCountry()).toLowerCase(Locale.ROOT) + ".png");
            if (imageSource != null) {
                image = new Image(imageSource.toExternalForm());
            } else {
                image = new Image(item.getImageURL(), true);
            }
        }

        ImageView iv = new ImageView(image);
        double size = item != null ? 60 * watchFactor : 179 * watchFactor; // TODO: base it on the component size
        iv.setPreserveRatio(true);
        iv.setFitWidth(size);

        return iv;
    });
    
    private final ObjectProperty<Conference> conference = new SimpleObjectProperty<>();
    private final int max = 5;
    private int count = 0;
    
    public ConfSelectorView(String name) {
        super(name);

        setShowTransitionFactory(BounceInRightTransition::new);
        selector.setMainCircleRadius(130 * watchFactor);
        selector.setSelectorCircleRadius(30 * watchFactor);
        selector.setMouseTransparent(true);
        selector.selectedItemProperty().addListener((obs, ov, nv) -> accessConference(nv));
        
        if (Platform.isDesktop()) {
            setCenter(new StackPane(selector, WearUtils.getFrame()));
        } else {
            setCenter(selector);
        }
        
        conference.addListener((obs, ov, nv) -> {
            if (nv != null) {
                PauseTransition pause = new PauseTransition(selector.getTransitionDuration());
                pause.setOnFinished(t -> selector.setSelectedItem(nv));
                pause.playFromStart();
            }
        });
        
        setOnShowing(e -> {
            if (selector.getItems().isEmpty() || WearableModel.getInstance().getSelectedConference() == null) {
                Services.get(WearableService.class).ifPresent(service -> {
                    service.errorProperty().addListener((obs, ov, nv) -> {
                        if (nv) {
                            if (count++ < max) { 
                                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                                pause.setOnFinished(t -> retrieveConferences());
                                pause.playFromStart();
                            }
                        }
                    });
                    if (getOnMouseClicked() == null) {
                        retrieveConferences();
                    }
                });
            }
        });
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
 
    private void retrieveConferences() {
        setOnMouseClicked(null);
        Services.get(WearableService.class).ifPresent(service -> {
            service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.CONFERENCES_PATH, 
                    "retrieve conferences", 
                    (List<Conference> p) -> {
                        WearableModel.getInstance().setConferences(p);
                        selector.getItems().setAll(p);
                        boolean found = false;
                        for (Conference c : p) {
                            if (c.isSelected()) {
                                conference.set(c);
                                found = true;
                                break;
                            }
                        } 
                        if (!found) {
                            MobileApplication.getInstance().switchView(GluonWearable.OPEN_MOBILE_VIEW)
                                    .ifPresent(view -> 
                                        ((OpenMobileView) view).setOnAction("Select a conference", 
                                            e -> {
                                                service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.OPEN_MOBILE_PATH, WearableConstants.DATAMAP_OPEN_MOBILE_SELECT, null);
                                                ConfSelectorView.this.setOnMouseClicked(event -> retrieveConferences());
                                            }));
                            
                        }
                    });
            });
    }
    private void accessConference(Conference conference) {
        PauseTransition pause = new PauseTransition(selector.getTransitionDuration());
        pause.setOnFinished(e -> {
            WearableModel.getInstance().setSelectedConference(conference);
            MobileApplication.getInstance().switchView(SCHEDULE_VIEW);
        });
        pause.playFromStart();
    }
}
