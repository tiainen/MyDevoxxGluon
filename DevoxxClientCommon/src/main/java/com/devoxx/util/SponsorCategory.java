/*
 * Copyright (c) 2016, 2018 Gluon Software
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

public enum SponsorCategory {
    
    PLATINUM     ("OTN.SPONSOR.PLATINUM",     "platinum",     0),
    GOLD         ("OTN.SPONSOR.GOLD",         "gold",         1),
    SILVER       ("OTN.SPONSOR.SILVER",       "silver",       2),
    BRONZE       ("OTN.SPONSOR.BRONZE",       "bronze",       3),
    MG           ("OTN.SPONSOR.MG",           "mg",           4),
    STARTUP      ("OTN.SPONSOR.STARTUP",      "startup",      5),
    ARCADE       ("OTN.SPONSOR.ARCADE",       "arcade",       6),
    SPEAKERDINER ("OTN.SPONSOR.SPEAKERDINER", "speakerdiner", 7),
    UNIVERSITY   ("OTN.SPONSOR.UNIVERSITY",   "university",   8),
    LAB          ("OTN.SPONSOR.LAB",          "lab",          9),
    MEETGREET    ("OTN.SPONSOR.MEETGREET",    "MeetGreet",    10),
    HACKERGARTEN ("OTN.SPONSOR.HACKERGARTEN", "Hackergarten", 11),
    NA           ("OTN.SPONSOR.NA",           "na",           12);
 
    private final String name;
    private final String category;
    private final int categoryPrecedence;

    SponsorCategory(String name, String category, int i) {
        this.name = DevoxxBundle.getString(name);
        this.category = category;
        categoryPrecedence = i;
    }

    public int getValue() {
        return categoryPrecedence;
    }

    public String getShortName() {
        return category;
    }

    @Override
    public String toString() {
        return name;
    }
}
