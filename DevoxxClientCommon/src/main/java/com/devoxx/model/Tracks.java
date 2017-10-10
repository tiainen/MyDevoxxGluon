package com.devoxx.model;

import java.util.List;

public class Tracks {

    private String content;
    private List<Track> tracks;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }
}
