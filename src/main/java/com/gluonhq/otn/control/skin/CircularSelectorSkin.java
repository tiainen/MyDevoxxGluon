package com.gluonhq.otn.control.skin;


import com.gluonhq.otn.control.CircularSelector;
import javafx.animation.RotateTransition;
import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.control.SkinBase;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.concurrent.atomic.AtomicReference;

public class CircularSelectorSkin extends SkinBase<CircularSelector> {

    private Group group = new Group();
    private Circle mainCircle = new Circle();

    public CircularSelectorSkin(CircularSelector control) {
        super(control);

        mainCircle.radiusProperty().bind(getSkinnable().mainCircleRadiusProperty());
        mainCircle.getStyleClass().add("main-circle");

        getChildren().add(group);

        rebuild();

        getSkinnable().selectorCircleRadiusProperty().addListener((o, ov, nv) -> rebuild());
        getSkinnable().getItems().addListener((InvalidationListener) o -> rebuild());
        getSkinnable().transitionDurationProperty().addListener((o, ov, nv) -> rebuild());

    }

    private void rebuild() {

        group.getChildren().clear();
        group.getChildren().add(mainCircle);

        int confCount = getSkinnable().getItems().size();

        if (confCount < 1) return; // nothing to do

        double angle = 360 / confCount;
        AtomicReference<Double> absoluteAngle = new AtomicReference<>(0d);

        for (int i = 0; i < confCount; i++) {

            Circle c = new Circle();
            c.getStyleClass().add("selector-circle");
            c.setRadius(getSkinnable().getSelectorCircleRadius());

            Translate t = new Translate(0, -getSkinnable().getMainCircleRadius());

            Rotate r = new Rotate(i * angle);
            r.pivotYProperty().bind(mainCircle.layoutXProperty().add(mainCircle.radiusProperty()));
            r.pivotYProperty().bind(mainCircle.layoutYProperty().add(mainCircle.radiusProperty()));
            r.axisProperty().setValue(Rotate.Z_AXIS);

            c.getTransforms().addAll(t, r);

            final double cangle = i * angle;

            c.setOnMouseClicked(e -> {
//                System.out.println(absoluteAngle.get());
                RotateTransition transition = new RotateTransition(getSkinnable().getTransitionDuration(), group);
                transition.setByAngle((-absoluteAngle.get() - cangle) % 360);
                absoluteAngle.set(-cangle);

                transition.play();
            });

            group.getChildren().add(c);

        }

    }

}
