package com.devoxx.views.helper;

import javafx.css.PseudoClass;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionTrack {

    private static Map<String, PseudoClass> trackPseudoClassMap = new HashMap<>();
    private static List<String> classes = Arrays.asList("track-color0",
            "track-color1", "track-color2", "track-color3",
            "track-color4", "track-color5", "track-color6",
            "track-color7", "track-color8", "track-color9"
    );
    private static int index = 0;

    public static PseudoClass fetchPseudoClassForTrack(String trackId) {
        PseudoClass pseudoClass = trackPseudoClassMap.get(trackId);
        if (pseudoClass == null) {
            if (index > classes.size() - 1) {
                index = 0; // exhausted all colors, re-use
            }
            pseudoClass = PseudoClass.getPseudoClass(classes.get(index++));
            trackPseudoClassMap.put(trackId, pseudoClass);
        }
        return pseudoClass;
    }

    public static String fetchStyleClassForTrack(String trackId) {
        return fetchPseudoClassForTrack(trackId).getPseudoClassName();
    }
}
