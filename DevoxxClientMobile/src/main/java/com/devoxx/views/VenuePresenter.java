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
package com.devoxx.views;

import com.devoxx.DevoxxApplication;
import com.devoxx.DevoxxView;
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.views.helper.Util;
import com.gluonhq.charm.glisten.afterburner.GluonPresenter;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.gluonhq.maps.demo.PoiLayer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;

public class VenuePresenter extends GluonPresenter<DevoxxApplication> {

    private static double DEFAULT_ZOOM = 15.0;

    @Inject
    private Service service;

    @FXML
    private View venue;

    @FXML
    private MapView mapView;

    @FXML
    private Region imageSpacer;

    @FXML
    private ImageView imageView;

    @FXML
    private Label name;

    @FXML
    private Label address;

    private MapLayer venueMarker;
    private final ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> resizeImages();
    private final ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> resizeImages();
    private FloatingActionButton webActionButton;

    public void initialize() {
        createFloatingActionButtons();

        ReadOnlyObjectProperty<Conference> venueProperty = service.conferenceProperty();
        venueProperty.addListener((observableValue, oldVenue, venue) -> {
            if (venue != null) {
                updateVenueInformation(venue);
            }
        });
        if (venueProperty.get() != null) {
            updateVenueInformation(venueProperty.get());
        }

        venue.setOnShowing(event -> {
            AppBar appBar = getApp().getAppBar();
            appBar.setNavIcon(getApp().getNavMenuButton());
            appBar.setTitleText(DevoxxView.VENUE.getTitle());

            // FixME: The following is a hack to reset zoom value (OTN-254)
            mapView.setZoom(10.0);
            mapView.setZoom(DEFAULT_ZOOM);

            // randomly change image on each showing
            imageView.setImage(Util.getMediaBackgroundImage());
            venue.getScene().widthProperty().addListener(widthListener);
            venue.getScene().heightProperty().addListener(heightListener);

            resizeImages();
        });

        venue.setOnHiding(event -> {
            venue.getScene().widthProperty().removeListener(widthListener);
            venue.getScene().heightProperty().removeListener(heightListener);
        });
    }

    private void updateVenueInformation(Conference conference) {
        name.setText(conference.getVenue());
        address.setText(conference.getAddress());

        MapPoint venuePoint = new MapPoint(Double.valueOf(conference.getLatitude()), Double.valueOf(conference.getLongitude()));
        mapView.setCenter(venuePoint);
        mapView.setZoom(DEFAULT_ZOOM);

        if (venueMarker != null) {
            mapView.removeLayer(venueMarker);
        }
        venueMarker = createVenueMarker(venuePoint);
        mapView.addLayer(venueMarker);

        String url = conference.getWwwURL();
        if (url == null || url.isEmpty()) {
            webActionButton.hide();
        }

        resizeImages();
    }

    private Conference getVenue() {
        return service.getConference();
    }

    private void createFloatingActionButtons() {
        webActionButton = Util.createWebLaunchFAB(() -> getVenue().getWwwURL());
        webActionButton.getStyleClass().add("secondary");
        webActionButton.showOn(venue);
    }

    private MapLayer createVenueMarker(MapPoint venue) {
        PoiLayer answer = new PoiLayer();
        answer.getStyleClass().add("poi-layer");
        Node marker = MaterialDesignIcon.ROOM.graphic();
        marker.getStyleClass().add("marker");
        Group box = new Group(marker);
        box.getStyleClass().add("marker-container");
        // FIXME: Better Solution ?
        // StackPane added because of OTN-320.
        // Avoids Group to translate when zoom in / zoom out events takes place
        answer.addPoint(venue, new StackPane(box));
        return answer;
    }

    private void resizeImages() {
        if (venue == null || venue.getScene() == null) {
            return;
        }
        double newWidth = venue.getScene().getWidth();
        double newHeight = venue.getScene().getHeight() - getApp().getAppBar().getHeight(); // Exclude the AppBar
        // Resize and translate ImageView
        // Resize imageSpacer and stop expanding when a maxHeight is reached.
        Util.resizeImageViewAndImageSpacer(imageSpacer, imageView, newWidth, newHeight / 3.5);
    }
}
