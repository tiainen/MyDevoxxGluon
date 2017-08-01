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

import static com.devoxx.GluonWearable.SESSION_VIEW;
import com.devoxx.model.Session;
import com.devoxx.model.WearableModel;
import com.devoxx.util.WearableConstants;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.animation.FadeInRightBigTransition;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.charm.glisten.control.CharmListView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class SessionsView extends View {

    private final CharmListView<Session, String> listView;
    private final Label day;
    private final int max = 5;
    private int count = 0;
    
    public SessionsView(String name) {
        super(name);
        setShowTransitionFactory(FadeInRightBigTransition::new);
        
        final double maxWidth = WearUtils.getMaxWidth();
        
        day = new Label();
        day.getStyleClass().add("white");
        
        listView = new CharmListView<>();
        listView.setPlaceholder(new Label("No sessions yet"));
        listView.setCellFactory(p -> new CharmListCell<Session>() {
            
            private final VBox box;
            private final Label room, time, title;
            private Session session;
            {
                room = new Label();
                room.setWrapText(true);
                room.setMaxWidth((maxWidth - 50)/2);
                room.getStyleClass().add("red");
                time = new Label();
                time.getStyleClass().add("green");
                time.setMaxWidth(maxWidth);
                Pane gap = new Pane();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox hBox = new HBox(room, gap, time);
                hBox.setAlignment(Pos.CENTER);
                hBox.setMaxWidth(maxWidth - 58);
                
                title = new Label();
                title.getStyleClass().add("white");
                title.setWrapText(true);
                title.setMaxWidth(maxWidth - 58);
                title.setTextAlignment(TextAlignment.CENTER);
                box = new VBox(0, hBox, title);
                box.setAlignment(Pos.CENTER);
                box.setMaxWidth(maxWidth - 58);
                box.setPadding(new Insets(0, 0, 5, 0));
                
                box.setOnMouseClicked(e -> {
                    WearableModel.getInstance().setSelectedSession(session);
                    MobileApplication.getInstance().switchView(SESSION_VIEW);
                });
                setText(null);
            }
            
            @Override
            public void updateItem(Session item, boolean empty) {
                super.updateItem(item, empty);
                session = item;
                if (item != null && !empty) {
                    room.setText(item.getRoomName());
                    time.setText(item.getFromTime() + "-" + item.getToTime());
                    title.setText(item.getTitle());
                    setGraphic(box);
                } else {
                    setGraphic(null);
                }
            }
            
        });
        
        VBox controls = new VBox(0, 
                MaterialDesignIcon.CHEVRON_LEFT.button(e -> MobileApplication.getInstance().switchToPreviousView()), 
                day, listView);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 25, 30, 25));
        
        if (com.gluonhq.charm.down.Platform.isDesktop()) {
            setCenter(new StackPane(controls, WearUtils.getFrame()));
        } else {
            setCenter(controls);
        }
        
        setOnShowing(e -> {
            Services.get(WearableService.class).ifPresent(service -> {
                service.errorProperty().addListener((obs, ov, nv) -> {
                    if (nv) {
                        if (count++ < max) { 
                            PauseTransition pause = new PauseTransition(Duration.seconds(1));
                            pause.setOnFinished(t -> retrieveSessions());
                            pause.playFromStart();
                        }
                    }
                });
                retrieveSessions();
            });
        });
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
    
    private void retrieveSessions() {
        final int selectedDay = WearableModel.getInstance().getSelectedDay();
        ZonedDateTime zonedDay = WearableModel.getInstance().getSelectedConference().getDays()[selectedDay -1];
        day.setText(zonedDay.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        Services.get(WearableService.class).ifPresent(service -> {
            setMouseTransparent(true);
            service.sendMessage(WearableConstants.CHANNEL_ID + WearableConstants.SESSIONS_PATH + "/" + selectedDay, 
                    String.valueOf(selectedDay), 
                    (List<Session> p) -> {
                        WearableModel.getInstance().setSessions(p);
                        listView.setItems(FXCollections.observableArrayList(p));
                        Platform.runLater(() -> listView.scrollTo(p.get(0)));
                        setMouseTransparent(false);
            });
        });
    }
    
}
