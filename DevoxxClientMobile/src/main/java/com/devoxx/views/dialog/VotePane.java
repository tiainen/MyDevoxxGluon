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
package com.devoxx.views.dialog;

import com.devoxx.DevoxxView;
import com.devoxx.model.Session;
import com.devoxx.model.Vote;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxBundle;
import com.devoxx.views.SessionPresenter;
import com.gluonhq.charm.glisten.control.Rating;
import com.gluonhq.charm.glisten.control.Toast;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class VotePane extends BorderPane {

    private static final int DEFAULT_RATING = 3;
    private static final int MAX_RATING = 5;

    private Rating rating;
    private TextField  delivery, content, other;

    public VotePane(Service service, Session session) {

        Label contentTitle = new Label(DevoxxBundle.getString("OTN.VOTEPANE.RATE_THE_FOLLOWING_SESSION"));
        contentTitle.getStyleClass().add("title");

        rating = new Rating(MAX_RATING, DEFAULT_RATING);
        StackPane stackPane = new StackPane(rating);

        Label deliveryLabel = new Label(DevoxxBundle.getString("OTN.VOTEPANE.DELIVERY"));
        Label contentLabel = new Label(DevoxxBundle.getString("OTN.VOTEPANE.CONTENT"));
        Label otherLabel = new Label(DevoxxBundle.getString("OTN.VOTEPANE.OTHER"));
        deliveryLabel.getStyleClass().add("share-text");
        contentLabel.getStyleClass().add("share-text");
        otherLabel.getStyleClass().add("share-text");

        delivery = new TextField();
        content = new TextField();
        other = new TextField();

        VBox shareContainer = new VBox(deliveryLabel, delivery, contentLabel, content, otherLabel, other);
        shareContainer.getStyleClass().add("share-container");

        Button submit = new Button(DevoxxBundle.getString("OTN.BUTTON.SUBMIT_CAPS"));
        submit.setOnAction(event -> {
            // Submit Vote to Backend
            service.voteTalk(createVote(session.getTalk().getId()));
            // Switch to INFO Pane
            DevoxxView.SESSION.switchView().ifPresent(presenter -> {
                SessionPresenter sessionPresenter = (SessionPresenter) presenter;
                sessionPresenter.showSession(session, SessionPresenter.Pane.INFO);
            });
            // Show Toast
            Toast toast = new Toast(DevoxxBundle.getString("OTN.VOTEPANE.SUBMIT_VOTE"));
            toast.show();
        });

        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        HBox titleBox = new HBox(contentTitle, region, submit);
        titleBox.getStyleClass().add("title-box");

        VBox contentBox = new VBox();
        contentBox.getStyleClass().add("content");
        contentBox.getChildren().addAll(stackPane, shareContainer, titleBox);

        setTop(titleBox);
        setCenter(contentBox);
        getStyleClass().add("vote-pane");
    }

    private Vote createVote(String talkId) {
        Vote vote = new Vote(talkId);
        vote.setDelivery(delivery.getText());
        vote.setContent(content.getText());
        vote.setOther(other.getText());
        vote.setValue((int) rating.getRating());
        return vote;
    }

}
