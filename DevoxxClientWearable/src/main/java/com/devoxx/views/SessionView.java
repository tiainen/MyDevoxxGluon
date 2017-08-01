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
import com.gluonhq.charm.glisten.animation.BounceInRightTransition;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.model.Session;
import com.devoxx.model.WearableModel;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Icon;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SessionView extends View {

    private Session session;
    private final Icon favIcon;
    private boolean no_auth;
    private boolean fav;
    private boolean retry;
    
    public SessionView(String name) {
        super(name);

        setShowTransitionFactory(BounceInRightTransition::new);
        
        final double maxWidth = WearUtils.getMaxWidth();
        
        Label title = new Label();
        title.getStyleClass().add("white");
        title.setWrapText(true);
        title.setMaxWidth(maxWidth - 60);
        
        favIcon = new Icon(MaterialDesignIcon.FAVORITE_BORDER);
        favIcon.getStyleClass().add("red");
        favIcon.setOnMouseClicked(e -> {
            if (!no_auth) {
                fav = !fav;
                Services.get(WearableService.class).ifPresent(service -> {
                    service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SET_FAV_PATH + "/" + session.getSlotId(), 
                            String.valueOf(fav), 
                            (boolean[] result) -> {
                                no_auth = result[0];
                                fav = result[1];
                                favIcon.setContent(fav ? MaterialDesignIcon.FAVORITE : MaterialDesignIcon.FAVORITE_BORDER);
                            });
                });
            } else {
                if (retry) {
                    getSessionData();
                } else {
                    MobileApplication.getInstance().switchView(GluonWearable.OPEN_MOBILE_VIEW)
                        .ifPresent(view -> ((OpenMobileView) view).setOnAction("Sign in first",
                            t -> {
                                retry = true;
                                Services.get(WearableService.class).ifPresent(service -> {
                                    service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.OPEN_MOBILE_PATH, WearableConstants.DATAMAP_OPEN_MOBILE_AUTH, null);
                                });
                            }));
                } 
            }
        });
        VBox.setVgrow(favIcon, Priority.ALWAYS);
        favIcon.setMinHeight(50);
        
        Label room = new Label();
        room.setWrapText(true);
        room.setMaxWidth((maxWidth - 40)/2);
        room.getStyleClass().add("red");
        
        Label day = new Label();
        day.getStyleClass().add("white");
        final int selectedDay = WearableModel.getInstance().getSelectedDay();
        ZonedDateTime zonedDay = WearableModel.getInstance().getSelectedConference().getDays()[selectedDay -1];
        day.setText(zonedDay.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        
        Pane gap = new Pane();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox hBox = new HBox(room, gap, day);
        hBox.setAlignment(Pos.CENTER);
        hBox.setMaxWidth(maxWidth);
        
        Label time = new Label();
        time.getStyleClass().add("green");
        
        VBox vBox = new VBox(5, title, favIcon, hBox, time);
        vBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        HBox hCenter = new HBox(vBox, MaterialDesignIcon.CHEVRON_RIGHT.button(e -> MobileApplication.getInstance().switchView(GluonWearable.SUMMARY_VIEW)));
        hCenter.setAlignment(Pos.CENTER);
        hCenter.setMaxWidth(maxWidth);
        hCenter.setPadding(new Insets(0, 0, 0, 20));
        
        VBox controls = new VBox(5.0, MaterialDesignIcon.CHEVRON_LEFT.button(e -> MobileApplication.getInstance().switchToPreviousView()),
                hCenter, 
                MaterialDesignIcon.ARROW_DOWNWARD.button(e -> MobileApplication.getInstance().switchView(GluonWearable.SPEAKERS_VIEW)));
        controls.setAlignment(Pos.CENTER);
        
        if (com.gluonhq.charm.down.Platform.isDesktop()) {
            setCenter(new StackPane(controls, WearUtils.getFrame()));
        } else {
            setCenter(controls);
        }
        
        setOnShowing(e -> {
            session = WearableModel.getInstance().getSelectedSession();
            title.setText(session.getTitle());
            room.setText(session.getRoomName());
            time.setText(session.getFromTime() + "-" + session.getToTime());
            favIcon.setContent(MaterialDesignIcon.FAVORITE_BORDER);
            getSessionData();
        });
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
 
    private void getSessionData() {
        Services.get(WearableService.class).ifPresent(service -> {
            setMouseTransparent(true);
            service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_FAV_PATH + "/" + session.getSlotId(), 
                    session.getSlotId(), 
                    (boolean[] result) -> {
                        no_auth = result[0];
                        fav = result[1];
                        if (!no_auth) {
                            favIcon.setContent(fav ? MaterialDesignIcon.FAVORITE : MaterialDesignIcon.FAVORITE_BORDER);
                        }
                        setMouseTransparent(false);
                    });
        });
    }
}
