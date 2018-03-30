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
    
    private String name;
    private String slug;
    private SponsorCategory level;

    public Sponsor() {

    }

    public Sponsor(String name, String slug, String level) {
        this.name = name;
        this.slug = slug;
        setLevel(level);
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

    public SponsorCategory getLevel() {
        return level;
    }

    public void setLevel(String level) {
        for (SponsorCategory category : SponsorCategory.values()) {
            if (category.getSlug().equals(level)) {
                this.level = category;
                break;
            }
        }
    }

    @Override
    public boolean merge(Sponsor other) {
        boolean changed = false;
        if (!Objects.equals(other.name, this.name)) {
            changed = true;
            this.name = other.name;
        }
        if (!Objects.equals(other.slug, this.slug)) {
            changed = true;
            this.slug = other.slug;
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
}
