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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.devoxx.DevoxxView;
import com.gluonhq.cloudlink.client.media.MediaClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AboutPresenter extends GluonPresenter<DevoxxApplication> {

    private static final Logger LOG = Logger.getLogger(AboutPresenter.class.getName());

    private MediaClient mediaClient;

    @FXML
    private View aboutView;

    @FXML
    private Separator separator;

    @FXML
    private ImageView devoxxImage;

    @FXML
    private ImageView gluonLogo;

    @FXML
    private Label devoxxLabel;

    @FXML
    private Label gluonLabel;

    public void initialize() {
        mediaClient = new MediaClient();

        try {
            devoxxImage.setImage(mediaClient.loadImage("aboutDevoxx"));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load media image 'aboutDevoxx'.", e);
        }

        aboutView.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.ABOUT.getTitle());

//            devoxxImage.setFitWidth(((Region) separator.getParent()).getWidth());
        });

        gluonLogo.setImage(new Image(getClass().getResource("gluon_logo.png").toExternalForm()));

        devoxxImage.setFitWidth(290.0);
        gluonLogo.setFitWidth(250);

        devoxxLabel.setText(DevoxxBundle.getString("OTN.ABOUT.LABEL.DEVOXX", DevoxxSettings.BUILD_NUMBER));
        gluonLabel.setText(DevoxxBundle.getString("OTN.ABOUT.LABEL.GLUON"));

    }
}
