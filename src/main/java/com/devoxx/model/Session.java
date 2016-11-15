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
package com.devoxx.model;



import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class Session implements Searchable {
    //TODO Eventually we have to replace it with the data coming from the API
    private static final ZoneId CONFERENCE_ZONE_ID = ZoneId.of("Europe/Brussels");
    private static final int CONFERENCE_DAYS_NUMBER = 5;
    private static final ZonedDateTime[] CONFERENCE_DATES = new ZonedDateTime[CONFERENCE_DAYS_NUMBER];
    // 2016 September 18th
    public static final ZonedDateTime CONFERENCE_START_DATE = ZonedDateTime.of(2016, 11, 7, 0, 0, 0, 0, CONFERENCE_ZONE_ID);

    private String slotId;
    private String roomId;
    private String roomName;
    private String day;
    private String fromTime;
    private long fromTimeMillis;
    private String toTime;
    private long toTimeMillis;
    private Break aBreak;
    private Talk talk;

    static {
        for (int i = 0; i < CONFERENCE_DAYS_NUMBER; ++i) {
            CONFERENCE_DATES[i] = CONFERENCE_START_DATE.plusDays(i);
        }
    }

    public Session() {}

    public Session(String uuid, String roomId, String roomName, String day, String fromTime, long fromTimeMillis, String toTime, long toTimeMillis, Break aBreak, Talk talk) {
        this.slotId = uuid;
        this.roomId = roomId;
        this.roomName = roomName;
        this.day = day;
        this.fromTime = fromTime;
        this.fromTimeMillis = fromTimeMillis;
        this.toTime = toTime;
        this.toTimeMillis = toTimeMillis;
        this.aBreak = aBreak;
        this.talk = talk;
    }

    public String getUuid() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getFromTime() {
        return fromTime;
    }

    public void setFromTime(String fromTime) {
        this.fromTime = fromTime;
    }

    public long getFromTimeMillis() {
        return fromTimeMillis;
    }

    public void setFromTimeMillis(long fromTimeMillis) {
        this.fromTimeMillis = fromTimeMillis;
    }

    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public long getToTimeMillis() {
        return toTimeMillis;
    }

    public void setToTimeMillis(long toTimeMillis) {
        this.toTimeMillis = toTimeMillis;
    }

    public Break getaBreak() {
        return aBreak;
    }

    public void setaBreak(Break aBreak) {
        this.aBreak = aBreak;
    }

    public Talk getTalk() {
        return talk;
    }

    public void setTalk(Talk talk) {
        this.talk = talk;
    }

    public static ZoneId getConferenceZoneId() {
        return CONFERENCE_ZONE_ID;
    }

    private static ZonedDateTime dayOnly(ZonedDateTime dateTime) {
        return ZonedDateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), 0, 0, 0, 0, CONFERENCE_ZONE_ID);
    }

    public static int getConferenceDayIndex(ZonedDateTime dateTime) {
        return Arrays.binarySearch(CONFERENCE_DATES, dayOnly(dateTime)) + 1;
    }

    public ZonedDateTime getStartDate() {
        return timeToZonedDateTime(getFromTimeMillis());
    }

    public ZonedDateTime getEndDate() {
        return timeToZonedDateTime(getToTimeMillis());
    }

    public int getConferenceDayIndex() {
        return getConferenceDayIndex(getStartDate());
    }

    private static ZonedDateTime timeToZonedDateTime( long time ) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), CONFERENCE_ZONE_ID);
    }

    public boolean isOverlappingWith(Session otherSession ) {
        if (otherSession == null || this.equals(otherSession)) return false;

        return dateInRange( otherSession.getEndDate(),   getStartDate(), getEndDate() ) ||
                dateInRange( otherSession.getStartDate(), getStartDate(), getEndDate() );
    }

    private static boolean dateInRange( ZonedDateTime dateTime, ZonedDateTime rangeStart, ZonedDateTime rangeEnd ) {
        return dateTime.compareTo(rangeStart) >= 0 && dateTime.compareTo(rangeEnd) <= 0;
    }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        } 
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return ((getRoomName() != null && getRoomName().toLowerCase(Locale.ROOT).contains(lowerKeyword)));
    }

    public String getTitle() {
        if (talk != null) return talk.getTitle();
        return "session without a talk";
    }

    public String getSummary() {
        if (talk != null) return talk.getSummary();
        return "session without a talk";
    }
    
    public String getLocation() {
        return getRoomName();
    }
}
