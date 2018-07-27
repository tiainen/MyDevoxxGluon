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
import com.devoxx.model.Feedback;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.TextArea;
import com.gluonhq.charm.glisten.control.TextField;
import com.gluonhq.charm.glisten.control.TextInput;
import com.gluonhq.charm.glisten.control.Toast;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.cloudlink.client.user.User;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javax.inject.Inject;

public class FeedbackPresenter extends GluonPresenter<DevoxxApplication> {

    private static final PseudoClass PSEUDO_CLASS_ERROR = PseudoClass.getPseudoClass("error");

    @FXML
    private View feedback;

    @FXML
    private TextField name;
    
    @FXML
    private TextField email;
    
    @FXML
    private TextArea message;
    
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
                final String nameText = name.getText();
                final String emailText = email.getText();
                final String messageText = message.getText();
                if (validateInput(name, email, message)) {
                    sendFeedback(nameText, emailText, messageText);
                }
            });
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(close);
            appBar.setTitleText(DevoxxView.FEEDBACK.getTitle());
            appBar.getActionItems().add(send);
        });
        
        feedback.setOnShown(e -> feedback.requestFocus());
        addValidator(name, email, message);
    }

    private void sendFeedback(String nameText, String emailText, String messageText) {
        final Feedback feedback = new Feedback(nameText, emailText, messageText);
        service.sendFeedback(feedback);
        final Toast toast = new Toast(DevoxxBundle.getString("OTN.FEEDBACK.MSG.SUCCESS"));
        toast.show();
        DevoxxView.SESSIONS.switchView();
    }

    private boolean validateInput(TextInput... textInputs) {
        boolean status = true;
        for (TextInput textInput : textInputs) {
            if (textInput.getText().isEmpty()) {
                textInput.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, true);
                status = false;
            } else {
                textInput.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, false);
            }    
        }
        return status;
    }

    private void addValidator(TextInput... textInputs) {
        for (TextInput textInput : textInputs) {
            if (textInput.getText().isEmpty()) {
                textInput.setErrorValidator(s -> {
                    if (s.isEmpty()) {
                        return DevoxxBundle.getString("OTN.FEEDBACK.MSG.EMPTY");
                    }
                    return "";
                });
            }
        }
    }
}
