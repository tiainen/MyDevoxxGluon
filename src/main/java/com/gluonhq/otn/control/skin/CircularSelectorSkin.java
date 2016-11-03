package com.gluonhq.otn.control.skin;


import com.gluonhq.otn.control.CircularSelector;
import javafx.animation.RotateTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.concurrent.atomic.AtomicReference;

public class CircularSelectorSkin<T> extends SkinBase<CircularSelector<T>> {

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

        Node centerDecoration = getSkinnable().getCellFactory().apply(null);
        if ( centerDecoration != null ) {

            Bounds b = centerDecoration.getBoundsInParent();
            Translate t = new Translate();
            t.xProperty().bind(mainCircle.centerXProperty().subtract(b.getWidth()/2));
            t.yProperty().bind(mainCircle.centerYProperty().subtract(b.getHeight()/2));
            centerDecoration.getTransforms().add(t);
            group.getChildren().add(centerDecoration);

        }

        int itemCount = getSkinnable().getItems().size();
        if (itemCount < 1) return; // nothing to do

        double angle = 360 / itemCount;
        AtomicReference<Double> absoluteAngle = new AtomicReference<>(0d);

        final double selectorRadius = getSkinnable().getSelectorCircleRadius();

        for (int i = 0; i < itemCount; i++) {

            T item = getSkinnable().getItems().get(i);
            final Node itemGraphic = getSkinnable().getCellFactory().apply(item);
            if (itemGraphic == null) continue;

            Label itemLabel = new Label( null, itemGraphic);
            itemLabel.setUserData(item);

            Translate t = new Translate(-selectorRadius, -getSkinnable().getMainCircleRadius()-selectorRadius);
            Rotate r = new Rotate(i * angle);
            r.setPivotX(selectorRadius);
            r.pivotYProperty().bind( mainCircle.radiusProperty().add(selectorRadius));

            r.axisProperty().setValue(Rotate.Z_AXIS);

            itemLabel.getTransforms().addAll(t, r);

            final double cangle = i * angle;

            itemLabel.setOnMouseClicked(e -> {
                RotateTransition transition = new RotateTransition(getSkinnable().getTransitionDuration(), group);
                transition.setByAngle((-absoluteAngle.get() - cangle) % 360);
                absoluteAngle.set(-cangle);
                transition.setOnFinished(ae -> getSkinnable().setSelectedItem((T) itemLabel.getUserData()));
                transition.play();
            });

            group.getChildren().add(itemLabel);

        }

    }

}