package com.devoxx.views.layer;

import com.devoxx.DevoxxView;
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.glisten.application.GlassPane;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.ProgressIndicator;
import com.gluonhq.charm.glisten.layout.Layer;
import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import static com.devoxx.views.helper.Util.hidePastConferenceMessage;
import static com.devoxx.views.helper.Util.isConferenceFromPast;
import static com.devoxx.views.helper.Util.showPastConferenceMessage;

public class ConferenceLoadingLayer extends Layer {

    private static final PseudoClass PSEUDO_CLASS_VOXXED = PseudoClass.getPseudoClass("voxxed");
    private static Duration TIMEOUT = Duration.seconds(20);

    private final Service service;
    private final GlassPane glassPane;
    private final DisplayService displayService;

    private final Label conferenceLabel;
    private final Region background;
    private final ProgressIndicator progressIndicator;

    private ConferenceLoadingLayer(Service service, Conference conference) {
        this.service = service;
        this.glassPane = MobileApplication.getInstance().getGlassPane();

        progressIndicator = new ProgressIndicator();
        progressIndicator.pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, Conference.Type.VOXXED == conference.getEventType());

        conferenceLabel = new Label(conference.getName());

        background = new Region();
        background.getStyleClass().add("background");

        setAutoHide(false);
        getStyleClass().add("conf-layer");
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

        displayService = Services.get(DisplayService.class).orElse(null);
    }
    
    public static void show(Service service, Conference conference) {
        new ConferenceLoadingLayer(service, conference).show();
    }

    @Override
    public void hide() {
        DevoxxView.SESSIONS.switchView();
        if (isConferenceFromPast(service.getConference())) {
            showPastConferenceMessage();
        } else {
            hidePastConferenceMessage();
        }
        super.hide();
    }

    @Override
    public void layoutChildren() {
        final double glassPaneWidth = snapSize(glassPane.getWidth());
        final double glassPaneHeight = snapSize(glassPane.getHeight());
        background.resizeRelocate(0, 0, glassPaneWidth, glassPaneHeight);

        double radius = Math.min(glassPaneWidth, glassPaneHeight) / 2 - 20;
        if (displayService != null && displayService.isTablet()) {
            radius = Math.min(glassPaneWidth, glassPaneHeight) / 4;
        }
        progressIndicator.setRadius(radius);
        progressIndicator.resizeRelocate(glassPaneWidth / 2 - radius, glassPaneHeight / 2 - radius, radius * 2, radius * 2);

        double labelWidth = Math.min(conferenceLabel.prefWidth(-1), radius - 10);
        double labelHeight = conferenceLabel.prefHeight(labelWidth);
        conferenceLabel.resizeRelocate(glassPaneWidth / 2 - labelWidth / 2, glassPaneHeight / 2 - labelHeight / 2, labelWidth, labelHeight);

        super.layoutChildren();
    }
}
