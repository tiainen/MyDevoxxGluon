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

import static com.devoxx.GluonWearable.SESSIONS_VIEW;
import com.devoxx.model.Conference;
import com.devoxx.model.WearableModel;
import com.devoxx.util.DevoxxSettings;
import com.devoxx.views.helper.WearUtils;
import com.gluonhq.charm.glisten.animation.FadeInRightBigTransition;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ScheduleView extends View {

    private final ListView<ZonedDateTime> listView;
    
    public ScheduleView(String name) {
        super(name);
        setShowTransitionFactory(FadeInRightBigTransition::new);
        
        Label title = new Label();
        title.getStyleClass().add("green");
        title.setWrapText(true);
        
        listView = new ListView<>();
        listView.setPlaceholder(new Label("No days yet"));
        listView.setCellFactory(p -> new ListCell<ZonedDateTime>() {
            
            private final VBox box;
            private final Label dayOfWeek, dayFormatted;
            private ZonedDateTime day;
            {
                dayOfWeek = new Label();
                dayOfWeek.getStyleClass().add("white");
                dayFormatted = new Label();
                dayFormatted.getStyleClass().add("white");
                box = new VBox(0, dayOfWeek, dayFormatted);
                box.setPadding(new Insets(5));
                box.setAlignment(Pos.CENTER);
                box.setOnMouseClicked(e -> {
                    WearableModel.getInstance().setSelectedDay(WearableModel.getInstance().getSelectedConference().getConferenceDayIndex(day));
                    MobileApplication.getInstance().switchView(SESSIONS_VIEW);
                });
                setText(null);
            }
            @Override
            protected void updateItem(ZonedDateTime item, boolean empty) {
                super.updateItem(item, empty);
                day = item;
                if (item != null && !empty) {
                    dayOfWeek.setText(item.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
                    dayFormatted.setText(item.format(DevoxxSettings.WEARABLE_DATE_FORMATTER));
                    setGraphic(box);
                } else {
                    setGraphic(null);
                }
            }
            
        });
        
        VBox controls = new VBox(10.0, 
                title,
                listView);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(20, 20, 30, 20));
        
        if (com.gluonhq.charm.down.Platform.isDesktop()) {
            setCenter(new StackPane(controls, WearUtils.getFrame()));
        } else {
            setCenter(controls);
        }
        
        setOnShowing(e -> {
            final Conference selectedConference = WearableModel.getInstance().getSelectedConference();
            if (selectedConference != null && selectedConference.getDays() != null) {
                title.setText(selectedConference.getCountry());
                listView.setItems(FXCollections.observableArrayList(selectedConference.getDays()));
            }
        });
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setVisible(false);
    }
    
}
