package com.gluonhq.otn.model;

public enum Conference {

    DEVOXX_US("usa", "USA"),
    DEVOXX_BE("be", "Belgium"),
    DEVOXX_MA("ma", "Morocco"),
    DEVOXX_FR("fr", "France"),
    DEVOXX_UK("uk", "United Kingdom"),
    DEVOXX_PL("pl", "Poland");

    private final String id;
    private final String name;

    Conference(String id, String name) { // TODO: define more conference attributes, such as url etc
        this.id = id;
        this.name = name;
    }

    public String getImageFileName() {
        return "splash_btn_" + id + ".png";
    }

    public String getName() {
        return name;
    }

}
