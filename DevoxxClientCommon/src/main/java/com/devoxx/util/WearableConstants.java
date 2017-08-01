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
package com.devoxx.util;

public class WearableConstants {

    // Wearable-Mobile communication
    public static final String CHANNEL_ID = "/000001";
    public static final String HEADER_PATH = "/header";
    public static final String OBJECT_PATH = "/object";
    public static final String LIST_PATH = "/list";
    public static final String ERROR_PATH = "/error";
    public static final String DATAMAP_TIMESTAMP = "timestamp";
    public static final String DATAMAP_ERROR = "error";
    
    // conference selector
    public static final String CONFERENCES_PATH = "/conferences";
    public static final String DATAMAP_COUNTRY = "country";
    public static final String DATAMAP_COUNTRY_SELECTED = "country_selected";
    public static final String DATAMAP_COUNTRY_TO_DAY = "country_to_day";
    public static final String DATAMAP_COUNTRY_TIMEZONE = "country_timezone";
    public static final String DATAMAP_COUNTRY_FROM_DAY = "country_from_day";
    public static final String DATAMAP_COUNTRY_ID = "country_id";
    
    // sessions
    public static final String SESSIONS_PATH = "/sessions";
    public static final String DATAMAP_SESSION_DAY = "session_day";
    public static final String DATAMAP_SESSION_ROOM = "session_room";
    public static final String DATAMAP_SESSION_SLOT_ID = "session_slot_id";
    public static final String DATAMAP_SESSION_SUMMARY = "session_summary";
    public static final String DATAMAP_SESSION_FROM_TIME = "session_from_time";
    public static final String DATAMAP_SESSION_TO_TIME = "session_to_time";
    public static final String DATAMAP_SESSION_TITLE = "session_title";
    
    // session
    public static final String SESSION_FAV_PATH = "/session_fav";
    public static final String DATAMAP_NO_AUTH = "user_no_auth";
    public static final String DATAMAP_SESSION_IS_FAV = "session_is_fav";
    
    public static final String SESSION_SET_FAV_PATH = "/session_set_fav";
    public static final String SESSION_SUMMARY_PATH = "/session_summary";
    
    // speakers
    public static final String SPEAKERS_PATH = "/speakers";
    public static final String DATAMAP_SPEAKER_FIRST_NAME = "speaker_first_name";
    public static final String DATAMAP_SPEAKER_LAST_NAME = "speaker_last_name";
    public static final String DATAMAP_SPEAKER_IMAGE = "speaker_image";
    public static final String DATAMAP_SPEAKER_TWITTER = "speaker_twitter";
    
    // twitter
    public static final String TWITTER_PATH = "/twitter";
    
    // mobile
    public static final String OPEN_MOBILE_PATH = "/open_mobile";
    public static final String DATAMAP_OPEN_MOBILE_SELECT = "open_mobile_select";
    public static final String DATAMAP_OPEN_MOBILE_AUTH = "open_mobile_auth";
}
