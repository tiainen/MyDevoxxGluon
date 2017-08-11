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

package com.devoxx.filter;

import com.devoxx.model.Session;
import com.devoxx.util.DevoxxBundle;

import java.time.ZonedDateTime;

public enum TimePeriod {
    MORE_THAN_ONE_HOUR_AGO("OTN.FILTER.TIME_PERIOD.MORE_THAN_ONE_HOUR", 1),
    MORE_THAN_TWO_HOURS_AGO("OTN.FILTER.TIME_PERIOD.MORE_THAN_TWO_HOURS", 2),
    MORE_THAN_FOUR_HOURS_AGO("OTN.FILTER.TIME_PERIOD.MORE_THAN_FOUR_HOURS", 4),
    ALL("OTN.FILTER.TIME_PERIOD.ALL", 4);

    private String i18nKey;
    private long hours;

    TimePeriod(String i18nKey, long hours) {
        this.i18nKey = i18nKey;
        this.hours = hours;
    }

    @Override
    public String toString() {
        return DevoxxBundle.getBundle().getString(i18nKey);
    }

    public static boolean isSessionWithinPeriod(Session session, TimePeriod timePeriod) {
        return timePeriod == ALL ||
               ZonedDateTime.now().minusHours(timePeriod.hours).isBefore(session.getEndDate());
    }
}
