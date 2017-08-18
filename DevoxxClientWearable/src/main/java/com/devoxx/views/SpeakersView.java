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
import com.devoxx.model.WearSpeaker;
import com.devoxx.model.WearableModel;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class SpeakersView extends View {

    private VBox vBox;
    
    private final double maxWidth;
    private final ScrollPane pane;
    
    public SpeakersView(String name) {
        super(name);

        setShowTransitionFactory(BounceInRightTransition::new);
        
        maxWidth = WearUtils.getMaxWidth();
        
        vBox = new VBox(15);
        vBox.setPrefWidth(maxWidth - 58);
        vBox.setMaxWidth(maxWidth - 58);
        vBox.setAlignment(Pos.CENTER);
        
        pane = new ScrollPane(vBox);
        pane.setPrefWidth(maxWidth - 50);
        
        VBox controls = new VBox(5.0, MaterialDesignIcon.ARROW_UPWARD.button(e -> MobileApplication.getInstance().switchToPreviousView()),
                pane);
        controls.setAlignment(Pos.CENTER);
        controls.setPrefWidth(maxWidth);
        controls.setPadding(new Insets(0, 25, 25, 25));
        
        if (com.gluonhq.charm.down.Platform.isDesktop()) {
            setCenter(new StackPane(controls, WearUtils.getFrame()));
        } else {
            setCenter(controls);
        }
        
        setOnShowing(e -> {
            
            Label label = new Label("Retrieving data...");
            label.getStyleClass().add("white");
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.CENTER);
            label.setAlignment(Pos.CENTER);
            label.setMaxWidth(maxWidth - 58);
            vBox.getChildren().setAll(label);
            
            getSpeakersData(WearableModel.getInstance().getSelectedSession().getSlotId());
        });
    }
    
    private VBox createSpeaker(WearSpeaker speaker) {
        VBox box = new VBox(10);
        box.setPrefWidth(maxWidth - 48);
        box.setAlignment(Pos.CENTER);
        
        Avatar avatar = new Avatar(40, speaker.getImage());
        
        Label title = new Label(speaker.getSpeaker().getFullName());
        title.getStyleClass().add("white");
        title.setWrapText(true);
        title.setPrefWidth(maxWidth - 58);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setAlignment(Pos.CENTER);
        VBox.setVgrow(avatar, Priority.ALWAYS);
        
        Label label = new Label("Follow me: ");
        label.getStyleClass().add("white");
        
        ImageView twitterIcon = new ImageView(new Image(SpeakersView.class.getResourceAsStream("twitter.png"), 32, 32, true, true));
        twitterIcon.setOnMouseClicked(e -> {
                MobileApplication.getInstance().switchView(GluonWearable.OPEN_MOBILE_VIEW)
                    .ifPresent(view -> ((OpenMobileView) view).setOnAction("Go to Twitter",
                        t -> {
                            Services.get(WearableService.class).ifPresent(service -> {
                                service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.TWITTER_PATH + "/" + speaker.getSpeaker().getTwitter(), 
                                        speaker.getSpeaker().getTwitter(), null);
                            });
                        }));
        });
        HBox hBox = new HBox(10, label, twitterIcon);
        hBox.setMaxWidth(maxWidth - 58);
        hBox.setAlignment(Pos.CENTER);
        
        box.getChildren().addAll(avatar, title, hBox);
        return box;
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
 
    private void getSpeakersData(String sessionSlotId) {
        Services.get(WearableService.class).ifPresent(service -> {
            setMouseTransparent(true);
            service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.SPEAKERS_PATH + "/" + sessionSlotId, 
                    sessionSlotId, 
                    (List<WearSpeaker> s) -> {
                        vBox.getChildren().clear();
                        for (WearSpeaker speaker : s) {
                            vBox.getChildren().add(createSpeaker(speaker));
                        }
                        pane.setVvalue(0);
                        setMouseTransparent(false);
                    });
        });
    }
}
