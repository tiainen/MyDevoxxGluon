package com.devoxx.util;


public final class Strings {

    public static boolean isNullOrEmpty( String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String nullToEmpty(String string) {
        return (string == null) ? "" : string;
    }

    public static String emptyToNull( String string) {
        return isNullOrEmpty(string) ? null : string;
    }
}
