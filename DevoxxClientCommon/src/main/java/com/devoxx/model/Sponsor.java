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
package com.devoxx.model;

import com.devoxx.util.SponsorCategory;

import java.util.Locale;
import java.util.Objects;

public class Sponsor extends Searchable implements Mergeable<Sponsor> {
    
    private String id;
    private String name;
    private String slug;
    private String imageURL;
    private SponsorCategory level;

    public Sponsor() {

    }

    public Sponsor(String id, String name, String slug, String imageURL, String level) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.imageURL = imageURL;
        setLevel(level);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public SponsorCategory getLevel() {
        return level;
    }

    public void setLevel(String level) {
        for (SponsorCategory category : SponsorCategory.values()) {
            if (category.getShortName().equalsIgnoreCase(level)) {
                this.level = category;
                break;
            }
        }
    }

    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append(safeStr(getId()))
            .append(",").append(safeStr(getName()))
            .append(",").append(safeStr(getSlug()))
            .append(",").append(safeStr(getImageURL()))
            .append(",").append(safeStr(getLevel().getShortName()));
        return csv.toString();
    }

    public static Sponsor fromCSV(String csv) {
        Sponsor sponsor = null;
        if (csv == null || csv.isEmpty()) return null;
        final String[] split = csv.split(",");
        if (split.length == 5) {
            sponsor = new Sponsor();
            sponsor.setId(split[0]);
            sponsor.setName(split[1]);
            sponsor.setSlug(split[2]);
            sponsor.setImageURL(split[3]);
            sponsor.setLevel(split[4]);
        }
        return sponsor;
    }

    private static String safeStr(String s) {
        return s == null? "": s.trim();
    }

    @Override
    public boolean merge(Sponsor other) {
        boolean changed = false;
        if (!Objects.equals(other.id, this.id)) {
            changed = true;
            this.id = other.id;
        }
        if (!Objects.equals(other.name, this.name)) {
            changed = true;
            this.name = other.name;
        }
        if (!Objects.equals(other.slug, this.slug)) {
            changed = true;
            this.slug = other.slug;
        }
        if (!Objects.equals(other.imageURL, this.imageURL)) {
            changed = true;
            this.imageURL = other.imageURL;
        }
        if (!Objects.equals(other.level, this.level)) {
            changed = true;
            this.level = other.level;
        }
        return changed;
    }

    @Override
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsKeyword(getName(), lowerKeyword)        ||
                containsKeyword(getSlug(), lowerKeyword) ||
                containsKeyword(getLevel(), lowerKeyword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sponsor sponsor = (Sponsor) o;
        return Objects.equals(id, sponsor.id) &&
                Objects.equals(name, sponsor.name) &&
                Objects.equals(slug, sponsor.slug) &&
                Objects.equals(imageURL, sponsor.imageURL) &&
                level == sponsor.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug, imageURL, level);
    }
}
