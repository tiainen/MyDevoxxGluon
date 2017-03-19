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

import com.devoxx.model.Vote;
import com.devoxx.util.DevoxxBundle;
import com.gluonhq.charm.glisten.control.Dialog;
import com.gluonhq.charm.glisten.control.Rating;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class VoteDialog extends Dialog<Vote> {

    private Vote vote;
    private Rating rating;
    private TextField  delivery, content, other;

    public VoteDialog(String title) {
        this.rootNode.getStyleClass().add("vote-dialog");

//        setTitleText(DevoxxBundle.getString("OTN.VOTEDIALOG.VOTE"));
        Label titleLabel = new Label(DevoxxBundle.getString("OTN.VOTEDIALOG.VOTE"));
        VBox contentBox = new VBox();
        Label contentTitle = new Label(DevoxxBundle.getString("OTN.VOTEDIALOG.RATE_THE_FOLLOWING_SESSION"));
        Label sessionTitleLabel = new Label(title);
        StackPane stackPane = new StackPane();
        rating = new Rating();
        stackPane.getChildren().add(rating);

        Label deliveryLabel = new Label(DevoxxBundle.getString("OTN.VOTEDIALOG.DELIVERY"));
        Label contentLabel = new Label(DevoxxBundle.getString("OTN.VOTEDIALOG.CONTENT"));
        Label otherLabel = new Label(DevoxxBundle.getString("OTN.VOTEDIALOG.OTHER"));
        delivery = new TextField();
        content = new TextField();
        other = new TextField();
        VBox shareContainer = new VBox();
        shareContainer.getChildren().addAll(deliveryLabel, delivery, contentLabel, content, otherLabel, other);

        contentBox.getChildren().addAll(contentTitle, sessionTitleLabel, stackPane, shareContainer);

        setContent(contentBox);

        Button cancel = new Button(DevoxxBundle.getString("OTN.BUTTON.CANCEL"));
        Button submit = new Button(DevoxxBundle.getString("OTN.BUTTON.SUBMIT_CAPS"));
        //getButtons().addAll(cancel, submit);

        cancel.getStyleClass().addAll("flat", "light");
        submit.getStyleClass().addAll("flat", "light");
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        HBox titleBox = new HBox(titleLabel, region, cancel, submit);
        setTitle(titleBox);

        cancel.setOnAction(event -> {
            hide();
        });
        submit.setOnAction(event -> {
            vote.setDelivery(delivery.getText());
            vote.setContent(content.getText());
            vote.setOther(other.getText());
            vote.setValue((int) rating.getRating());
            setResult(vote);
            hide();
        });

        contentBox.getStyleClass().add("content");
        contentTitle.getStyleClass().add("title");
        sessionTitleLabel.getStyleClass().add("session-title");
        deliveryLabel.getStyleClass().add("share-text");
        contentLabel.getStyleClass().add("share-text");
        otherLabel.getStyleClass().add("share-text");
        shareContainer.getStyleClass().add("share-container");

    }

    public void setVote(Vote vote) {
        this.vote = vote;
        rating.setRating(vote.getValue());
        delivery.setText(vote.getDelivery());
        content.setText(vote.getContent());
        other.setText(vote.getOther());
    }


}
