package com.gluonhq.otn.control;

import com.gluonhq.otn.control.skin.CircularSelectorSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.util.Duration;

public class CircularSelector extends Control {

    private ObservableList<Item> items = FXCollections.observableArrayList();

    public CircularSelector() {
        getStyleClass().add("timeline-view");
    }

    @Override
    public String getUserAgentStylesheet() {
        return CircularSelector.class.getResource("circular-selector.css").toExternalForm();
    }

    @Override
    public Skin<CircularSelector> createDefaultSkin() {
        return new CircularSelectorSkin(this);
    }

    // mainCircleRadiusProperty
    private final DoubleProperty mainCircleRadiusProperty =
            new SimpleDoubleProperty(this, "mainCircleRadius", 150);
    public final DoubleProperty mainCircleRadiusProperty() {
        return mainCircleRadiusProperty;
    }
    public final double getMainCircleRadius() {
        return mainCircleRadiusProperty.get();
    }
    public final void setMainCircleRadius(double value) {
        mainCircleRadiusProperty.set(value);
    }

    // selectorCircleRadiusProperty
    private final DoubleProperty selectorCircleRadiusProperty =
            new SimpleDoubleProperty(this, "selectorCircleRadius", 40);
    public final DoubleProperty selectorCircleRadiusProperty() {
        return selectorCircleRadiusProperty;
    }
    public final double getSelectorCircleRadius() {
        return selectorCircleRadiusProperty.get();
    }
    public final void setSelectorCircleRadius(double value) {
        selectorCircleRadiusProperty.set(value);
    }

    // selector items
    public ObservableList<Item> getItems() {
        return items;
    }

    // transitionDurationProperty
    private final ObjectProperty<Duration> transitionDurationProperty =
            new SimpleObjectProperty<>(this, "transitionDuration", Duration.millis(1000));
    public final ObjectProperty<Duration> transitionDurationProperty() {
        return transitionDurationProperty;
    }
    public final Duration getTransitionDuration() {
        return transitionDurationProperty.get();
    }
    public final void setTransitionDuration(Duration value) {
        transitionDurationProperty.set(value);
    }

    public static class Item {
        // add picture, url and more properties
    }

}