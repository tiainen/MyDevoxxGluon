/**
 * Copyright (c) 2017, 2018 Gluon Software
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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Locale;
import java.util.Objects;

public class Badge extends Searchable {

    @SuppressWarnings("unused")
    public Badge() {
    }

    public Badge(String qr) {
        if (qr != null && ! qr.isEmpty() && qr.split("::").length == 5) {
            String[] split = qr.split("::");
            badgeId.set(split[0]);
            lastName.set(split[1]);
            firstName.set(split[2]);
            company.set(split[3]);
            email.set(split[4]);
        }
    }

    private StringProperty badgeId = new SimpleStringProperty();
    public StringProperty badgeIdProperty() { return badgeId; }
    public String getBadgeId() { return badgeId.get(); }
    public void setBadgeId(String badgeId) { this.badgeId.set(badgeId); }

    private StringProperty firstName = new SimpleStringProperty();
    public StringProperty firstNameProperty() { return firstName; }
    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String firstName) { this.firstName.set(firstName); }

    private StringProperty lastName = new SimpleStringProperty();
    public StringProperty lastNameProperty() { return lastName; }
    public String getLastName() { return lastName.get(); }
    public void setLastName(String lastName) { this.lastName.set(lastName); }

    private StringProperty company = new SimpleStringProperty();
    public StringProperty companyProperty() { return company; }
    public String getCompany() { return company.get(); }
    public void setCompany(String company) { this.company.set(company); }

    private StringProperty email = new SimpleStringProperty();
    public StringProperty emailProperty() { return email; }
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }

    private StringProperty details = new SimpleStringProperty();
    public StringProperty detailsProperty() { return details; }
    public String getDetails() { return details.get(); }
    public void setDetails(String details) { this.details.set(details); }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        } 
        final String lowerKeyword = keyword.toLowerCase(Locale.ROOT);

        return containsKeyword(getFirstName(), lowerKeyword) ||
               containsKeyword(getLastName(), lowerKeyword)  ||
               containsKeyword(getCompany(), lowerKeyword)   ||
               containsKeyword(getEmail(), lowerKeyword)     ||
               containsKeyword(getDetails(), lowerKeyword);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Badge badge = (Badge) o;
        return Objects.equals(badgeId, badge.badgeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(badgeId);
    }

    protected String safeStr(String s) {
        return s == null? "": s.trim();
    }

    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append(safeStr(getBadgeId()));
        csv.append(",").append(safeStr(getFirstName()));
        csv.append(",").append(safeStr(getLastName()));
        csv.append(",").append(safeStr(getCompany()));
        csv.append(",").append(safeStr(getEmail()));
        csv.append(",").append(safeStr(getDetails()));
        return csv.toString();
    }
    
}
