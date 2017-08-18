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

import com.devoxx.model.Session;
import com.gluonhq.charm.glisten.animation.BounceInRightTransition;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.model.WearableModel;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SummaryView extends View {

    private final double maxWidth;

    private final Label title;
    private final Label summary;
    private final ScrollPane pane;
    
    public SummaryView(String name) {
        super(name);

        setShowTransitionFactory(BounceInRightTransition::new);
        
        maxWidth = WearUtils.getMaxWidth();
        
        title = new Label();
        title.getStyleClass().add("green");
        title.setWrapText(true);
        title.setMaxWidth(maxWidth - 50);
        
        HBox boxTitle = new HBox(title);
        boxTitle.setMaxWidth(maxWidth - 50);
        
        summary = new Label();
        summary.getStyleClass().add("white");
        summary.setWrapText(true);
        summary.setMaxWidth(maxWidth - 58);
        
        pane = new ScrollPane(summary);
        pane.setPrefWidth(maxWidth - 50);
        VBox.setVgrow(pane, Priority.ALWAYS);
        
        VBox controls = new VBox(10.0, MaterialDesignIcon.CHEVRON_LEFT.button(e -> MobileApplication.getInstance().switchToPreviousView()),
                boxTitle, pane);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 25, 30, 25));
        
        if (com.gluonhq.charm.down.Platform.isDesktop()) {
            setCenter(new StackPane(controls, WearUtils.getFrame()));
        } else {
            setCenter(controls);
        }
        
        setOnShowing(e -> {
            final Session selectedSession = WearableModel.getInstance().getSelectedSession();
            title.setText(selectedSession.getTitle());
            getSessionData(selectedSession.getSlotId());
        });
    }
    
    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
 
    private void getSessionData(String sessionSlotId) {
        Services.get(WearableService.class).ifPresent(service -> {
            setMouseTransparent(true);
            service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.SESSION_SUMMARY_PATH + "/" + sessionSlotId, 
                    sessionSlotId, 
                    (String s) -> {
                        summary.setText(s);
                        pane.setVvalue(0);
                        setMouseTransparent(false);
                    });
        });
    }
}
