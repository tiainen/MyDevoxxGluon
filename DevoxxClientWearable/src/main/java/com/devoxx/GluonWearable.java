/**
 * Copyright (c) 2017, Gluon Software
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

import com.devoxx.views.ConfSelectorView;
import com.devoxx.views.ScheduleView;
import com.devoxx.views.AmbientView;
import com.devoxx.views.OpenMobileView;
import com.devoxx.views.SessionView;
import com.devoxx.views.SessionsView;
import com.devoxx.views.SpeakersView;
import com.devoxx.views.SummaryView;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.LifecycleEvent;
import com.gluonhq.charm.down.plugins.LifecycleService;
import com.gluonhq.charm.down.plugins.WearableService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import static com.gluonhq.charm.glisten.application.MobileApplication.HOME_VIEW;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class GluonWearable extends MobileApplication {

    public static final String CONF_SELECTOR_VIEW = HOME_VIEW;
    public static final String SCHEDULE_VIEW = "Schedule_View";
    public static final String SESSIONS_VIEW = "Sessions_View";
    public static final String SESSION_VIEW = "Session_View";
    public static final String SPEAKERS_VIEW = "Speakers_View";
    public static final String SUMMARY_VIEW = "Summary_View";
    public static final String AMBIENT_VIEW = "Ambient_View";
    public static final String OPEN_MOBILE_VIEW = "Open_Mobile_View";
    
    private AmbientView ambientView;

    @Override
    public void init() {
        addViewFactory(CONF_SELECTOR_VIEW, () -> new ConfSelectorView(CONF_SELECTOR_VIEW));
        addViewFactory(SCHEDULE_VIEW, () -> new ScheduleView(SCHEDULE_VIEW));
        addViewFactory(SESSIONS_VIEW, () -> new SessionsView(SESSIONS_VIEW));
        addViewFactory(SESSION_VIEW, () -> new SessionView(SESSION_VIEW));
        addViewFactory(SPEAKERS_VIEW, () -> new SpeakersView(SPEAKERS_VIEW));
        addViewFactory(SUMMARY_VIEW, () -> new SummaryView(SUMMARY_VIEW));
        addViewFactory(OPEN_MOBILE_VIEW, () -> new OpenMobileView(OPEN_MOBILE_VIEW));
        addViewFactory(AMBIENT_VIEW, () -> {
            ambientView = new AmbientView(AMBIENT_VIEW);
            return ambientView;
        });
    }

    @Override
    public void postInit(Scene scene) {
        Swatch.BLUE.assignTo(scene);
        
        if (Platform.isDesktop()) {
            ((Stage) scene.getWindow()).setWidth(230);
            ((Stage) scene.getWindow()).setHeight(252);
        }

        ((Stage) scene.getWindow()).getIcons().add(new Image(GluonWearable.class.getResourceAsStream("/icon.png")));
        scene.getStylesheets().add(GluonWearable.class.getResource("wearable.css").toExternalForm());
        if (Platform.isDesktop()) {
            scene.getStylesheets().add(GluonWearable.class.getResource("/desktop.css").toExternalForm());
        }
        Services.get(LifecycleService.class).ifPresent(service -> {
            service.addListener(LifecycleEvent.PAUSE, () -> {
                Services.get(WearableService.class).ifPresent(s -> s.keepScreenOn(false));
            });
            service.addListener(LifecycleEvent.RESUME, () -> {
                Services.get(WearableService.class).ifPresent(s -> s.keepScreenOn(true));
            });
        });
        
        // TODO: Investigate why the device enters ambient mode too soon (less than 5 seconds)
        Services.get(WearableService.class).ifPresent(service -> {
            // This is required to prevent the device from going to ambient mode too soon
            service.keepScreenOn(true);
            // with screen on this is not required
            service.setAmbientHandler(() -> switchView(AMBIENT_VIEW), 
                    () -> ambientView.updateView(), 
                    () -> switchToPreviousView());
        });
        
    }

}
