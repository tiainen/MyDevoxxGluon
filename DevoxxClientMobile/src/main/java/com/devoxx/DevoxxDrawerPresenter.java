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
package com.devoxx;

import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.afterburner.AppView;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;

import java.util.Optional;

import static com.gluonhq.charm.glisten.application.MobileApplication.HOME_VIEW;

import com.devoxx.util.DevoxxBundle;
import com.devoxx.util.DevoxxSettings;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.gluonhq.charm.glisten.control.Toast;
import javafx.scene.image.Image;

@Singleton
public class DevoxxDrawerPresenter extends GluonPresenter<DevoxxApplication> {


    private final NavigationDrawer drawer;
    private final Header header;
    private final NavigationDrawer.Item logOut;
    
    @Inject
    private Service service;
    
    public DevoxxDrawerPresenter() {
        drawer = new NavigationDrawer();
        header = new Header();
        
        drawer.setHeader(header);

        for (AppView view : DevoxxView.REGISTRY.getViews()) {
            if (view.isShownInDrawer()) {
                drawer.getItems().add(view.getMenuItem());
            }
        }

        logOut = new NavigationDrawer.Item(DevoxxBundle.getString("OTN.DRAWER.LOG_OUT"), MaterialDesignIcon.CANCEL.graphic());
        logOut.managedProperty().bind(logOut.visibleProperty());
        logOut.selectedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                if (service.logOut()) {
                    Toast toast = new Toast(DevoxxBundle.getString("OTN.LOGGED_OUT_MESSAGE"));
                    toast.show();

                    // switch to home view
                    getApp().switchView(HOME_VIEW);
                }
            }
        });
        drawer.getItems().add(logOut);
        
        drawer.addEventHandler(NavigationDrawer.ITEM_SELECTED, e -> getApp().hideLayer(DevoxxApplication.MENU_LAYER));
        
        getApp().viewProperty().addListener((obs, oldView, newView) -> {
            Optional.ofNullable(oldView)
                    .flatMap(v -> DevoxxView.REGISTRY.getView(oldView))
                    .ifPresent(otnView -> otnView.getMenuItem().setSelected(false));
            updateDrawer(newView);
        });
        updateDrawer(getApp().getView());
    }

    @PostConstruct
    public void postConstruct() {
        header.text.setText(DevoxxBundle.getString("OTN.DRAWER.CONF_NAME", getConferenceShortName(service.getConference())));
        service.conferenceProperty().addListener((obs, ov, nv) -> {
            header.text.setText(DevoxxBundle.getString("OTN.DRAWER.CONF_NAME", getConferenceShortName(nv)));
        });
    }

    private void updateDrawer(View view) {
        DevoxxView.REGISTRY.getView(view).ifPresent(otnView -> {
            drawer.setSelectedItem(otnView.getMenuItem());
            otnView.selectMenuItem();
        });
    }

    private String getConferenceShortName(Conference conference) {
        if (conference != null) {
            String conferenceShortName = conference.getShortName();
            if (conferenceShortName != null) {
                return conferenceShortName;
            }
        }
        return "";
    }

    public final void setSidePopupView(SidePopupView sidePopupView) {
        sidePopupView.showingProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                logOut.setVisible(service.isAuthenticated() && !DevoxxSettings.AUTO_AUTHENTICATION);
            }
        });
        header.setSidePopupView(sidePopupView);
    }
    
    public final NavigationDrawer getDrawer() {
        return drawer;
    }

    private class Header extends Region {

        private final ImageView background;
//        private final Button profileButton;
        private final Label text;
        private final double aspectRatio;

        public Header() {
            // background image
            background = Util.getMediaBackgroundImageView();
            final Image image = background.getImage();
            aspectRatio = image == null ? 0.3 : image.getHeight() / image.getWidth();
            background.setFitWidth(getWidth());
            // text
            text = new Label();
            text.getStyleClass().add("primary-title");

//            // profile button
//            profileButton  = MaterialDesignIcon.SETTINGS.button(e -> {
//                getApp().hideLayer(MENU_LAYER);
//                DevoxxView.PROFILE.switchView();
//            });
//            // the profile button is only visible when the user is logged in
//            profileButton.setVisible(false);
//            profileButton.managedProperty().bind(profileButton.visibleProperty());
            
            getChildren().addAll(background, text/*, profileButton*/);
        }

        protected void setSidePopupView(SidePopupView sidePopupView) {
//            sidePopupView.showingProperty().addListener((obs, ov, nv) -> {
//                if (nv) {
//                    // the profile button is only visible when the user is logged in,
//                    // and we check every time the drawer is shown
//                    profileButton.setVisible(service.isAuthenticated());
//                }
//            });
        }
        
        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();

            background.setFitWidth(w);

            // Position text in bottom-left
            final double labelWidth = text.prefWidth(-1);
            final double labelHeight = text.prefHeight(-1);
            text.resizeRelocate(0, h - labelHeight, labelWidth, labelHeight);

            // put profile down bottom-right
//            final double profileBtnWidth = profileButton.prefWidth(-1);
//            final double profileBtnHeight = profileButton.prefHeight(-1);
//            profileButton.resizeRelocate(x - profileBtnWidth, h - profileBtnHeight - padding, profileBtnWidth, profileBtnHeight);
        }

        @Override
        protected double computePrefHeight(double width) {
            return width * aspectRatio;
        }
        
    }
}