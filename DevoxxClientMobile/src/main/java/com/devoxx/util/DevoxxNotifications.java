/**
 * Copyright (c) 2016, 2017 Gluon Software
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
package com.devoxx.util;

import com.devoxx.DevoxxView;
import com.devoxx.model.Session;
import com.devoxx.service.Service;
import com.devoxx.views.SessionPresenter;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.LocalNotificationsService;
import com.gluonhq.charm.down.plugins.Notification;
import javafx.collections.ListChangeListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.devoxx.util.DevoxxLogging.LOGGING_ENABLED;
import static java.time.temporal.ChronoUnit.SECONDS;

@Singleton
public class DevoxxNotifications {
    
    private static final Logger LOG = Logger.getLogger(DevoxxNotifications.class.getName());

    public static final String GCM_SENDER_ID = "945674729015";
    
    private static final String ID_START = "START_";
    private static final String ID_VOTE = "VOTE_";

    private final static String TITLE_VOTE_SESSION = DevoxxBundle.getString("OTN.VISUALS.VOTE_NOW");
    private final static String TITLE_SESSION_STARTS = DevoxxBundle.getString("OTN.VISUALS.SESSION_STARTING_SOON");
    private final static int SHOW_VOTE_NOTIFICATION = -2; // show vote notification two minutes before session ends
    private final static int SHOW_SESSION_START_NOTIFICATION = -15; // show session start warning 15 minutes before posted start time
    
    private final Map<String, Notification> scheduledSessionNotificationMap = new HashMap<>();
    private final Map<String, Notification> voteSessionNotificationMap = new HashMap<>();

    private ListChangeListener<Session> scheduledSessionslistener;
    
    @Inject
    private Service service;
    
    private final Optional<LocalNotificationsService> notificationsService;
    
    public DevoxxNotifications() {
        notificationsService = Services.get(LocalNotificationsService.class);
    }

    /**
     * For a new Scheduled Session, we create two local notifications:
     * - One notification will be triggered by the device before the session starts
     * - One notification will be triggered by the device right before the session ends
     * 
     * We create the notifications and schedule them on the device, but only for future events. 
     * 
     * @param session The new scheduled session
     */
    public final void addScheduledSessionNotifications(Session session) {
        final String sessionId = session.getTalk().getId();
        
        if (!scheduledSessionNotificationMap.containsKey(sessionId)) {
            createStartNotification(session).ifPresent(notification -> {
                scheduledSessionNotificationMap.put(sessionId, notification);
                notificationsService.ifPresent(n -> n.getNotifications().add(notification));
            });
        }
        
        if (!voteSessionNotificationMap.containsKey(sessionId)) {
            createVoteNotification(session).ifPresent(notification -> {
                voteSessionNotificationMap.put(sessionId, notification);
                notificationsService.ifPresent(n -> n.getNotifications().add(notification));
            });
        }
    }
    
    /**
     * For a given Scheduled session, if the user unschedules it, its two notifications
     * will be cancelled on the device
     * @param session the scheduled session to unschedule
     */
    public final void removeScheduledSessionNotifications(Session session) {
        final Notification startNotification = scheduledSessionNotificationMap.remove(session.getTalk().getId());
        if (startNotification != null) { 
            notificationsService.ifPresent(n -> n.getNotifications().remove(startNotification));
        }
        final Notification voteNotification = voteSessionNotificationMap.remove(session.getTalk().getId());
        if (voteNotification != null) { 
            notificationsService.ifPresent(n -> n.getNotifications().remove(voteNotification));
        }
    }
    
    /**
     * Called when the application starts, allows retrieving the scheduled
     * notifications, and restoring the notifications map
     */
    public void preloadScheduledSessions() {
        if (service.isAuthenticated()) { 
            scheduledSessionslistener = (ListChangeListener.Change<? extends Session> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        for (Session session : c.getAddedSubList()) {
                            if (LOGGING_ENABLED) {
                                LOG.log(Level.INFO, "Adding notification #" + session.getTalk().getId());
                            }
                            addAlreadyScheduledSessionNotifications(session);
                        }
                    }
                }
            };
            service.retrieveFavoredSessions().addListener(scheduledSessionslistener);
        }
    }
    
    /**
     * Called after the application has started and preloading the scheduled sessions
     * ends. At this point, we have all the notifications available, and we can remove
     * the listener (so new notifications are not treated as already scheduled) and 
     * send them to the Local Notifications service at once
     */
    public void preloadingScheduledSessionsDone() {
        if (scheduledSessionslistener != null) {
            service.retrieveFavoredSessions().removeListener(scheduledSessionslistener);
            scheduledSessionslistener = null;
        
            // process notifications at once
            List<Notification> notificationList = new ArrayList<>();
            notificationList.addAll(scheduledSessionNotificationMap.values());
            notificationList.addAll(voteSessionNotificationMap.values());
            
            if (notificationList.size() > 0) {
                LOG.log(Level.INFO, String.format("Adding %d notifications already scheduled", notificationList.size()));
                notificationsService.ifPresent(n -> n.getNotifications().addAll(notificationList));
            }
        }
    }
    
    /**
     * Creates a notification that will be triggered by the device before the session starts
     * @param session the scheduled session
     * @return a notification if the event is in the future
     */
    private Optional<Notification> createStartNotification(Session session) {
        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        
        // Add notification 15 min before session starts
        ZonedDateTime dateTimeStart = session.getStartDate().plusMinutes(SHOW_SESSION_START_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeStart = dateTimeStart.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
        }
        
        // add the notification for new ones if they haven't started yet
        if (dateTimeStart.isAfter(now)) {
            return Optional.of(getStartNotification(session, dateTimeStart));
        }
        return Optional.empty();
    }
    
    /**
     * Creates a notification that will be triggered by the device right before the session ends
     * @param session the scheduled session
     * @return a notification if the session wasn't added yet and it was already scheduled or 
     * the event is in the future
     */
    private Optional<Notification> createVoteNotification(Session session) {
        final ZonedDateTime now = ZonedDateTime.now(service.getConference().getConferenceZoneId());
        
        // Add notification 2 min before session ends
        ZonedDateTime dateTimeVote = session.getEndDate().plusMinutes(SHOW_VOTE_NOTIFICATION);
        if (DevoxxSettings.NOTIFICATION_TESTS) {
            dateTimeVote = dateTimeVote.minus(DevoxxSettings.NOTIFICATION_OFFSET, SECONDS);
        }
        
        // add the notification if the session hasn't finished yet
        if (dateTimeVote.isAfter(now)) {
            return Optional.of(getVoteNotification(session, dateTimeVote));
        }
        return Optional.empty();
    }
    
    /**
     * For an already scheduled session, we create two local notifications.
     * 
     * These notifications are not sent to the Local Notification service yet: 
     * this will be done when calling {@link #preloadingScheduledSessionsDone()}.
     * 
     * We don't schedule them again on the device, but we add these notifications to the 
     * notifications map, so in case they are delivered, their runnable is available.
     * 
     * @param session The already scheduled session
     */
    private void addAlreadyScheduledSessionNotifications(Session session) {
        final String sessionId = session.getTalk().getId();

        if (!scheduledSessionNotificationMap.containsKey(sessionId)) {
            final Notification dummyStartNotification = getStartNotification(session, null);
            // Remove notification to avoid duplicate notification
            notificationsService.ifPresent(ns -> {
                // we need to add the notification first so because no direct method
                // exists on LocalNotificationsService to un-schedule a notification.
                // and un-scheduling is done via the listener attached to notifications observable list
                ns.getNotifications().add(dummyStartNotification);
                ns.getNotifications().remove(dummyStartNotification);
            });

            // Add notification
            createStartNotification(session).ifPresent(n -> scheduledSessionNotificationMap.put(sessionId, n));
        }
        
        if (!voteSessionNotificationMap.containsKey(sessionId)) {
            final Notification dummyVoteNotification = getVoteNotification(session, null);
            notificationsService.ifPresent(ns -> {
                ns.getNotifications().add(dummyVoteNotification);
                ns.getNotifications().remove(dummyVoteNotification);
            });
            
            createVoteNotification(session).ifPresent(n -> voteSessionNotificationMap.put(sessionId, n));
        }
    }
    
    /**
     * Creates a notification that will be triggered by the device before the session starts
     * 
     * @param session the scheduled session
     * @param dateTimeStart the session's start zoned date time. If null, this notification won't be 
     * scheduled on the device
     * @return a local notification
     */
    private Notification getStartNotification(Session session, ZonedDateTime dateTimeStart) {
        return new Notification(
                ID_START + session.getTalk().getId(),
                TITLE_SESSION_STARTS, 
                DevoxxBundle.getString("OTN.VISUALS.IS_ABOUT_TO_START", session.getTitle()),
                DevoxxNotifications.class.getResourceAsStream("/icon.png"),
                dateTimeStart,
                () ->
                    DevoxxView.SESSION.switchView().ifPresent(presenter -> {
                        SessionPresenter sessionPresenter = (SessionPresenter) presenter;
                        sessionPresenter.showSession(session);
                    })
                );
    }
    
    /**
     * Creates a notification that will be triggered by the device right before the session ends
     * @param session the scheduled session
     * @param dateTimeVote the session's end zoned date time. If null, this notification won't be 
     * scheduled on the device
     * @return a local notification
     */
    private Notification getVoteNotification(Session session, ZonedDateTime dateTimeVote) {
        return new Notification(
                ID_VOTE + session.getTalk().getId(),
                TITLE_VOTE_SESSION,
                DevoxxBundle.getString("OTN.VISUALS.CAST_YOUR_VOTE_ON", session.getTitle()),
                DevoxxNotifications.class.getResourceAsStream("/icon.png"),
                dateTimeVote,
                () -> {
                    // first go to the session
                    DevoxxView.SESSION.switchView().ifPresent(presenter -> {
                        SessionPresenter sessionPresenter = (SessionPresenter) presenter;
                        sessionPresenter.showSession(session, SessionPresenter.Pane.VOTE);
                    });
                });
    }
    
}
