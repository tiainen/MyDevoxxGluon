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

import javax.xml.bind.annotation.XmlElement;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoUnit.DAYS;

public class Conference {

    private static final Logger LOG = Logger.getLogger(Conference.class.getName());

    private static final ZoneId DEFAULT_CONFERENCE_ZONE_ID = ZoneId.of("Europe/Brussels");

    private static final Map<String, String> conferenceShortNames = new HashMap<>();
    static {
        conferenceShortNames.put("France", "FR");
        conferenceShortNames.put("UK", "UK");
        conferenceShortNames.put("Poland", "PL");
        conferenceShortNames.put("Belgium", "BE");
        conferenceShortNames.put("Morocco", "MA");
        conferenceShortNames.put("United States", "US");
    }

    private String id;
    private String confType;
    private String confDescription;
    private String confIcon;
    private String venue;
    private String address;
    private String country;
    private String latitude;
    private String longitude;
    private List<Floor> floors;
    private String capacity;
    private String sessions;
    private String hashtag;
    private String splashImgURL;
    private String fromDate;
    private ZonedDateTime startDate;
    private String toDate;
    private ZonedDateTime endDate;
    private ZonedDateTime[] days;
    private String wwwURL;
    private String regURL;
    private String cfpURL;
    private String talkURL;
    private String votingURL;
    private String votingEnabled;
    private String votingImageName;
    private String cfpEndpoint;
    private String cfpVersion;
    private String youTubeURL;
    private String integrationId;
    private String pushEnabled;
    private String timezone;
    private ZoneId timezoneId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortName() {
        return conferenceShortNames.get(country);
    }

    public String getConfType() {
        return confType;
    }

    public void setConfType(String confType) {
        this.confType = confType;
    }

    public String getConfDescription() {
        return confDescription;
    }

    public void setConfDescription(String confDescription) {
        this.confDescription = confDescription;
    }

    public String getConfIcon() {
        return confIcon;
    }

    public void setConfIcon(String confIcon) {
        this.confIcon = confIcon;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public List<Floor> getFloors() {
        return floors;
    }

    public void setFloors(List<Floor> floors) {
        this.floors = floors;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getSessions() {
        return sessions;
    }

    public void setSessions(String sessions) {
        this.sessions = sessions;
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String hashtag) {
        this.hashtag = hashtag;
    }

    public String getSplashImgURL() {
        return splashImgURL;
    }

    public void setSplashImgURL(String splashImgURL) {
        this.splashImgURL = splashImgURL;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;

        if (this.fromDate != null && this.toDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public long getDaysUntilStart() {
        LocalDate today = LocalDate.now(getConferenceZoneId());
        return today.until(getStartDate(), ChronoUnit.DAYS);
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;

        if (this.fromDate != null && this.toDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    public ZonedDateTime getEndDate() {
        return endDate;
    }

    public long getDaysUntilEnd() {
        LocalDate today = LocalDate.now(getConferenceZoneId());
        return today.until(getEndDate(), ChronoUnit.DAYS);
    }

    private void calculateConferenceDays() {
        this.startDate = LocalDateTime.parse(fromDate, DateTimeFormatter.ofPattern("yyyy-M-d'T'HH:mm:ss.SSSX")).atZone(getConferenceZoneId());
        this.endDate = LocalDateTime.parse(toDate, DateTimeFormatter.ofPattern("yyyy-M-d'T'HH:mm:ss.SSSX")).atZone(getConferenceZoneId());
        long numberOfDays = DAYS.between(startDate, endDate) + 1;
        days = new ZonedDateTime[(int) numberOfDays];
        days[0] = dayOnly(startDate, getConferenceZoneId());
        for (int day = 1; day < numberOfDays; day++) {
            days[day] = days[0].plusDays(day);
        }
    }

    public ZonedDateTime[] getDays() {
        return days;
    }

    private static ZonedDateTime dayOnly(ZonedDateTime dateTime, ZoneId zoneId) {
        return ZonedDateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), 0, 0, 0, 0, zoneId);
    }

    public int getConferenceDayIndex(ZonedDateTime date) {
        return Arrays.binarySearch(days, dayOnly(date, getConferenceZoneId())) + 1;
    }

    public long getNumberOfDays() {
        return days.length;
    }

    public ZoneId getConferenceZoneId() {
        return timezoneId;
    }

    public String getWwwURL() {
        return wwwURL;
    }

    public void setWwwURL(String wwwURL) {
        this.wwwURL = wwwURL;
    }

    public String getRegURL() {
        return regURL;
    }

    public void setRegURL(String regURL) {
        this.regURL = regURL;
    }

    public String getCfpURL() {
        return cfpURL;
    }

    public void setCfpURL(String cfpURL) {
        this.cfpURL = cfpURL;
    }

    public String getTalkURL() {
        return talkURL;
    }

    public void setTalkURL(String talkURL) {
        this.talkURL = talkURL;
    }

    public String getVotingURL() {
        return votingURL;
    }

    public void setVotingURL(String votingURL) {
        this.votingURL = votingURL;
    }

    public String getVotingEnabled() {
        return votingEnabled;
    }

    public void setVotingEnabled(String votingEnabled) {
        this.votingEnabled = votingEnabled;
    }

    public String getVotingImageName() {
        return votingImageName;
    }

    public void setVotingImageName(String votingImageName) {
        this.votingImageName = votingImageName;
    }

    public String getCfpEndpoint() {
        return cfpEndpoint;
    }

    public void setCfpEndpoint(String cfpEndpoint) {
        this.cfpEndpoint = cfpEndpoint;
    }

    public String getCfpVersion() {
        return cfpVersion;
    }

    public void setCfpVersion(String cfpVersion) {
        this.cfpVersion = cfpVersion;
    }

    public String getYouTubeURL() {
        return youTubeURL;
    }

    public void setYouTubeURL(String youTubeURL) {
        this.youTubeURL = youTubeURL;
    }

    @XmlElement(name = "integration_id")
    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    public String getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(String pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        try {
            this.timezoneId = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            LOG.log(Level.WARNING, "Failed to convert timezone: " + timezone + ", using default timezone (Europe/Brussels) instead.");
            this.timezoneId = DEFAULT_CONFERENCE_ZONE_ID;
        }

        if (this.fromDate != null && this.toDate != null && this.timezoneId != null) {
            calculateConferenceDays();
        }
    }

    @Override
    public String toString() {
        return "Conference{" +
                "id='" + id + '\'' +
                ", confDescription='" + confDescription + '\'' +
                ", country='" + country + '\'' +
                ", cfpEndpoint='" + cfpEndpoint + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Conference that = (Conference) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
