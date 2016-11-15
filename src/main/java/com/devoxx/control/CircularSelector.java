/**
 * Copyright (c) 2016, Gluon Software
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
package com.devoxx.control;

import com.devoxx.control.skin.CircularSelectorSkin;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.util.Duration;

import java.util.Objects;
import java.util.function.Function;

public class CircularSelector<T> extends Control {

    private ObservableList<T> items = FXCollections.observableArrayList();
    private Function<T,Node> cellFactory = item -> null;

    public CircularSelector(Function<T,Node> cellFactory) {
        getStyleClass().add("circular-selector");
        this.cellFactory = Objects.requireNonNull(cellFactory);
    }

    @Override
    public String getUserAgentStylesheet() {
        return CircularSelector.class.getResource("circular-selector.css").toExternalForm();
    }

    @Override
    public Skin<CircularSelector<T>> createDefaultSkin() {
        return new CircularSelectorSkin<T>(this);
    }

    public final Function<T,Node> getCellFactory() {
        return cellFactory;
    }

    // selectedItemProperty
    private final ObjectProperty<T> selectedItemProperty = new SimpleObjectProperty<>(this, "selectedItem");
    public final ObjectProperty<T> selectedItemProperty() {
       return selectedItemProperty;
    }
    public final T getSelectedItem() {
       return selectedItemProperty.get();
    }
    public final void setSelectedItem(T value) {
        selectedItemProperty.set(value);
    }

    // mainCircleRadiusProperty
    private final DoubleProperty mainCircleRadiusProperty =
            new SimpleDoubleProperty(this, "mainCircleRadius", 130);
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
            new SimpleDoubleProperty(this, "selectorCircleRadius", 30);
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
    public ObservableList<T> getItems() {
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

}