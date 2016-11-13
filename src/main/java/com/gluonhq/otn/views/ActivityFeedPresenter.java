/**
 * Copyright (c) 2016, Gluon Software
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
package com.gluonhq.otn.views;

import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.otn.OTNApplication;
import com.gluonhq.otn.OTNView;
import com.gluonhq.otn.control.CircularSelector;
import com.gluonhq.otn.model.Conference;
import com.gluonhq.otn.model.Service;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import javax.inject.Inject;

public class ActivityFeedPresenter extends GluonPresenter<OTNApplication> {

    private static final String CONF_CIRCLE_NAME = "circle.png";

    @FXML
    private View activityFeedView;

    private CircularSelector<Conference> selector = new CircularSelector<>(item -> {

        String imageFileName = item == null ? CONF_CIRCLE_NAME : item.getImageFileName();
        ImageView iv = new ImageView(ActivityFeedPresenter.class.getResource(imageFileName).toExternalForm());

        if (item != null) {
            double size = 30 * 2; // TODO: base it on the component size
            iv.setPreserveRatio(true);
            iv.setFitWidth(size);
        }

        return iv;
    });

    @Inject
    private Service service;

    private boolean firstTime = true;
    private Button countryButton = new Button("Select a countryButton");


    public void initialize() {

        BorderPane content = new BorderPane();
        content.setStyle("-fx-padding: 70 0 70 0;");

        countryButton.setOnAction(e -> {
            service.setConference(selector.getSelectedItem());
            OTNView.SESSIONS.switchView();
        });
        selector.getItems().addAll(Conference.values());
        selector.selectedItemProperty().addListener(e -> {
            Conference c = selector.getSelectedItem();
            countryButton.setText("Go to " + c.getName());
        });
        content.setCenter(selector);
        content.setBottom(countryButton);
        content.setAlignment(countryButton, Pos.CENTER);
        activityFeedView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(OTNView.ACTIVITY_FEED.getTitle());
            appBar.getActionItems().add(getApp().getSearchButton());
            activityFeedView.setCenter(content);

            //TODO move to external css
            activityFeedView.setStyle("-fx-background-color: #363C5A");

        });

    }
}
