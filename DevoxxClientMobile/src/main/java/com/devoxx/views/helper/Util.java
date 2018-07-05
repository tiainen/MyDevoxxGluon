/*
 * Copyright (c) 2016, 2018 Gluon Software
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
package com.devoxx.views.helper;

import com.devoxx.model.Speaker;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.ImageCache;
import com.devoxx.views.ExhibitionMapPresenter;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.BrowserService;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.media.MediaClient;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class Util {

    // speaker avatars
    private static final Image DEFAULT_IMAGE = new Image(SpeakerCard.class.getResource("speaker.jpeg").toString());
    
    // drawer's header default image
    private static final Image DEFAULT_BACKGROUND_IMAGE = new Image(Util.class.getResource("backgroundImage.png").toString());

    // TODO: Provide default image
    public static final Image DEFAULT_EXHIBITION_MAP = new Image(ExhibitionMapPresenter.class.getResource("circle.png").toString());

    private static MediaClient mediaClient = new MediaClient();

    private Util() {}

    public static ImageView getMediaBackgroundImageView() {
        final ImageView imageView = new ImageView(getMediaBackgroundImage());
        imageView.setPreserveRatio(true);
         return imageView;
    }

    public static Image getMediaBackgroundImage() {
        Image loadImage = null;
        try {
            loadImage = mediaClient.loadImage("backgroundImage");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (loadImage == null || loadImage.getHeight() == 0) {
            return DEFAULT_BACKGROUND_IMAGE;
        }
        return loadImage;
    }

    public static Avatar getSpeakerAvatar(Speaker speaker, String... newStyleClasses) {
        Avatar avatar = new Avatar();
        avatar.getStyleClass().addAll(newStyleClasses);

        Image image = ImageCache.get(speaker.getAvatarURL(), () -> DEFAULT_IMAGE, downloadedImage -> avatar.setImage(downloadedImage));
        avatar.setImage(image);

        avatar.setCache(true);

        return avatar;
    }

    public static FloatingActionButton createFAB(MaterialDesignIcon icon, EventHandler<ActionEvent> onAction ) {
        FloatingActionButton floatingActionButton = new FloatingActionButton(icon.text, onAction);
        floatingActionButton.setFloatingActionButtonHandler(FloatingActionButton.BOTTOM_RIGHT);
        return floatingActionButton;
    }

    public static FloatingActionButton createWebLaunchFAB(Supplier<String> urlSupplier) {
        return createFAB(MaterialDesignIcon.LAUNCH, e -> {
            Services.get(BrowserService.class).ifPresent(b -> {
                try {
                    String url = urlSupplier.get();
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://".concat(url);
                    }
                    b.launchExternalBrowser(url);
                } catch (IOException | URISyntaxException ex) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.VISUALS.CONNECTION_FAILED"));
                    toast.show();
                }
            });
        });
    }
    
    public static void requestRating() {
        String url = null;
        if(Platform.isAndroid()) {
            String packageName = "com.devoxx";
            url = "market://details?id=" + packageName;
        } else if (Platform.isIOS()) {
            String appId = "1094805620";
            url = "itms-apps://itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews?id=" + appId +"&amp;onlyLatestVersion=true&amp;pageNumber=0&amp;sortOrdering=1&amp;type=Purple+Software";
        }
        String finalUrl = url;
        Services.get(BrowserService.class).ifPresent(b -> {
            try {
                if (finalUrl != null) {
                    b.launchExternalBrowser(finalUrl);
                }
            } catch (IOException | URISyntaxException e1) {
                Toast toast = new Toast("Failed to launch Store");
                toast.show();
            }
        });
    }

    public static void resizeImageView(ImageView imageView, double availableWidth, double maxHeight) {
        Image image = imageView.getImage();

        if(image == null || image.getWidth() == 0.0 || image.getHeight() == 0.0) {
            imageView.setFitWidth(0.0);
            imageView.setFitHeight(0.0);
            return;
        }

        double aspectRatio = image.getWidth() / image.getHeight();
        double estimatedHeight = availableWidth / aspectRatio;
        if(estimatedHeight <= maxHeight) {
            imageView.setFitWidth(availableWidth);
            imageView.setFitHeight(estimatedHeight);
        } else {
            imageView.setFitWidth(maxHeight * aspectRatio);
            imageView.setFitHeight(maxHeight);
        }
    }

    public static void resizeImageViewAndImageSpacer(Region imageSpacer, ImageView imageView, double maxWidth, double maxHeight) {
        Image image = imageView.getImage();
        if(image == null || image.getHeight() == 0.0) return;

        // Both ImageView and ImageSpacer should take up available width
        imageView.setFitWidth(maxWidth);
        imageSpacer.setPrefWidth(maxWidth);

        double aspectRatio = image.getWidth() / image.getHeight();
        double prefHeight = maxWidth / aspectRatio;

        imageSpacer.setMinHeight(prefHeight);
        imageSpacer.setPrefHeight(prefHeight);
        imageSpacer.setMaxHeight(maxHeight);

        if(prefHeight > maxHeight) {
            imageSpacer.setMinHeight(maxHeight);
            imageView.setTranslateY(maxHeight - imageView.getLayoutBounds().getHeight());
        } else {
            imageView.setTranslateY(0);
        }
    }

}
