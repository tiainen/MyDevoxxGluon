/**
 * Copyright (c) 2016, 2017, Gluon Software
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

import com.devoxx.util.DevoxxBundle;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.VideoService;
import com.gluonhq.charm.glisten.mvc.SplashView;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class DevoxxSplash extends SplashView { 

    public DevoxxSplash() {
        final BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("video-view");

        Pane pane = new Pane();
        HBox.setHgrow(pane, Priority.ALWAYS);

        Button skip = new Button(DevoxxBundle.getString("OTN.VIDEO.SKIP_VIDEO"));
        skip.setOnAction(e -> Services.get(VideoService.class).ifPresent(VideoService::hide));
        HBox hBox = new HBox(pane, skip);
        hBox.getStyleClass().add("container");

        borderPane.setBottom(hBox);
        setCenter(borderPane);

        setOnShown(e -> {
            Services.get(VideoService.class).ifPresent(video -> {
                video.setPosition(Pos.CENTER, 0, 10, 0, 10);
                video.statusProperty().addListener((obs, ov, nv) -> {
                    if (nv == MediaPlayer.Status.DISPOSED) {
                        hideSplashView();
                    }
                });
                video.getPlaylist().add("MyDevoxxIntro.mp4");
                // set enough time to have the view ready before playing:
                PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
                delay.setOnFinished(d -> video.play());
                delay.play();
            });
            skip.requestFocus();
        });
    }
}
