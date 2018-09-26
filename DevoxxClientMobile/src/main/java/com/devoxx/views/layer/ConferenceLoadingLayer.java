package com.devoxx.views.layer;

import com.devoxx.service.Service;
import com.gluonhq.charm.glisten.application.GlassPane;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.layout.Layer;
import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class ConferenceLoadingLayer extends Layer {
    
    private static Duration TIMEOUT = Duration.seconds(20);

    private final GlassPane glassPane;
    private final ProgressIndicator progressIndicator;
    private final Label conferenceLabel;
    private Region background;

    private ConferenceLoadingLayer(Service service, String conferenceName) {

        this.glassPane = MobileApplication.getInstance().getGlassPane();
        progressIndicator = new ProgressIndicator();

        conferenceLabel = new Label(conferenceName);
        conferenceLabel.setWrapText(true);
        conferenceLabel.setTextAlignment(TextAlignment.CENTER);
        conferenceLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 15));

        background = new Region();
        background.setStyle("-fx-background-color: white;");

        setAutoHide(false);
        getChildren().addAll(background, progressIndicator, conferenceLabel);

        final PauseTransition timeout = new PauseTransition(TIMEOUT);
        final InvalidationListener sessionsListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                if (service.retrieveSessions().size() > 0) {
                    timeout.stop();
                    ConferenceLoadingLayer.this.hide();
                    service.retrieveSessions().removeListener(this);
                }
            }
        };
        timeout.setOnFinished(e -> {
            service.retrieveSessions().removeListener(sessionsListener);
            hide();
        });
        timeout.playFromStart();
        service.retrieveSessions().addListener(sessionsListener);
    }
    
    public static void show(Service service, String conferenceName) {
        new ConferenceLoadingLayer(service, conferenceName).show();
    }

    @Override
    public void layoutChildren() {
        final double glassPaneWidth = snapSize(glassPane.getWidth());
        final double glassPaneHeight = snapSize(glassPane.getHeight());
        background.resizeRelocate(0, 0, glassPaneWidth, glassPaneHeight);

        double radius = Math.min(glassPaneWidth, glassPaneHeight) / 2 - 20;
        progressIndicator.setRadius(radius);
        progressIndicator.resizeRelocate(20, glassPaneHeight / 2 - radius, radius * 2, radius * 2);

        double labelWidth = Math.min(conferenceLabel.prefWidth(-1), radius - 10);
        double labelHeight = conferenceLabel.prefHeight(labelWidth);
        conferenceLabel.resizeRelocate(glassPaneWidth / 2 - labelWidth / 2, glassPaneHeight / 2 - labelHeight / 2, labelWidth, labelHeight);

        super.layoutChildren();
    }
}
