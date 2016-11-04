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
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.CardCell;
import com.gluonhq.charm.glisten.control.CardPane2;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.otn.OTNApplication;
import com.gluonhq.otn.OTNView;
import com.gluonhq.otn.control.CircularSelector;
import com.gluonhq.otn.model.News;
import com.gluonhq.otn.model.Service;
import com.gluonhq.otn.util.EulaManager;
import com.gluonhq.otn.util.ImageCache;
import com.gluonhq.otn.util.OTNBundle;
import com.gluonhq.otn.util.OTNSettings;
import com.gluonhq.otn.views.helper.Util;
import com.gluonhq.otn.views.layer.ImageViewLayer;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.inject.Inject;

public class ActivityFeedPresenter extends GluonPresenter<OTNApplication> {

    private static final String PLACEHOLDER_MESSAGE = OTNBundle.getString("OTN.ACTIVITY.PLACEHOLDER_MESSAGE");

    enum Conference {

        DEVOXX_US("usa"),
        DEVOXX_BE("be"),
        DEVOXX_MA("ma"),
        DEVOXX_FR("fr"),
        DEVOXX_UK("uk"),
        DEVOXX_PL("pl");

        private final String id;

        Conference(String id) { // TODO: define more conference attributes, such as url etc
            this.id = id;
        }

        public String getImageFileName() {
            return "splash_btn_" + id + ".png";
        }

    }

    private static final String CONF_CIRCLE_NAME = "circle.png";

    @FXML
    private View activityFeedView;

    private CircularSelector<Conference> selector = new CircularSelector<>( item -> {

        String imageFileName = item ==  null?  CONF_CIRCLE_NAME: item.getImageFileName();
        ImageView iv = new ImageView(ActivityFeedPresenter.class.getResource(imageFileName).toExternalForm());

        if ( item != null ) {
            double size = 30 * 2; // TODO: base it on the component size
            iv.setPreserveRatio(true);
            iv.setFitWidth(size);
        }

        return iv;
    });

    @Inject
    private Service service;

    private boolean firstTime = true;

    public void initialize() {

        selector.getItems().addAll(Conference.values());

        activityFeedView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(OTNView.ACTIVITY_FEED.getTitle());
            appBar.getActionItems().add(getApp().getSearchButton());

            activityFeedView.setCenter(selector);

            //TODO move to external css
            activityFeedView.setStyle("-fx-background-color: #363C5A");

        });

    }
}