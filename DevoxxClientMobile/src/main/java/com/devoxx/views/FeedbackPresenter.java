/*
 * Copyright (c) 2018, Gluon Software
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
import com.devoxx.DevoxxView;
import com.devoxx.service.Service;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.TextArea;
import com.gluonhq.charm.glisten.control.TextField;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javax.inject.Inject;

public class FeedbackPresenter extends GluonPresenter<DevoxxApplication> {
    
    @FXML
    private View feedback;

    @FXML
    private TextField name;
    
    @FXML
    private TextField email;
    
    @FXML
    private TextArea textArea;
    
    @Inject
    private Service service;
    
    public void initialize() {
        
        feedback.setOnShowing( event -> {
            final User authenticatedUser = service.getAuthenticatedUser();
            if (authenticatedUser != null) {
                name.setText(authenticatedUser.getName());
                email.setText(authenticatedUser.getEmail());
            }
            
            final Button close = MaterialDesignIcon.CLEAR.button(e -> DevoxxView.SESSIONS.switchView());
            final Button send = MaterialDesignIcon.SEND.button(e -> {
                // SEND
            });
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(close);
            appBar.setTitleText(DevoxxView.FEEDBACK.getTitle());
            appBar.getActionItems().add(send);
        });
        
        feedback.setOnShown(e -> feedback.requestFocus());
    }
}
