package com.devoxx.views.cell;

import com.devoxx.DevoxxView;
import com.devoxx.model.Conference;
import com.devoxx.service.Service;
import com.devoxx.util.DevoxxSettings;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import com.gluonhq.charm.down.plugins.StorageService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.CharmListCell;
import com.gluonhq.connect.provider.RestClient;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConferenceCell extends CharmListCell<Conference> {

    private static final Logger LOG = Logger.getLogger(ConferenceCell.class.getName());

    private static final PseudoClass PSEUDO_CLASS_VOXXED = PseudoClass.getPseudoClass("voxxed");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    
    private final Service service;
    private final Label name;
    private final Label eventType;
    private final Label dateLabel;

    private final ImageView background;
    private final VBox top;
    private final BorderPane content;
    private final StackPane root;
    private final DoubleBinding widthProperty;

    public ConferenceCell(Service service) {
        this.service = service;

        widthProperty = MobileApplication.getInstance().getGlassPane().widthProperty().subtract(15);

        name = new Label();
        name.getStyleClass().add("name");
        
        eventType = new Label();
        eventType.getStyleClass().add("type");
        
        dateLabel = new Label();
        dateLabel.getStyleClass().add("date");
        
        background = new ImageView();
        background.setPreserveRatio(true);
        
        top = new VBox(eventType, name);
        top.getStyleClass().add("top");
        

        content = new BorderPane();
        content.getStyleClass().add("content");
        content.setTop(top);
        content.setBottom(dateLabel);
        
        root = new StackPane(background, content);
        getStyleClass().add("conference-cell"); 
    }

    @Override
    public void updateItem(Conference item, boolean empty) {
        super.updateItem(item, empty);
        
        if (item != null && !empty) {
            eventType.setText(item.getEventType().name());
            if (item.getEventType() == Conference.Type.VOXXED) {
                pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, true);
            } else {
                pseudoClassStateChanged(PSEUDO_CLASS_VOXXED, false);
            }
            
            name.setText(item.getName());
            dateLabel.setText(LocalDate.parse(item.getFromDate()).getDayOfMonth() + " - " + 
                    LocalDate.parse(item.getEndDate()).format(DATE_TIME_FORMATTER));

            final Task<InputStream> inputStreamTask = loadBackgroundImage(item);
            inputStreamTask.setOnSucceeded(e -> {
                Image image = new Image(inputStreamTask.getValue());
                background.setImage(image);
            });
            inputStreamTask.exceptionProperty().addListener((o, ov, nv) -> {
                LOG.log(Level.SEVERE, nv.getMessage());
            });
            new Thread(inputStreamTask).start();
            
            background.fitWidthProperty().bind(widthProperty.subtract(2));
            
            content.setOnMouseReleased(e -> {
                if (!item.equals(service.getConference())) {
                    service.retrieveConference(item.getId());
                    Services.get(SettingsService.class).ifPresent(settingsService -> {
                        settingsService.store(DevoxxSettings.SAVED_CONFERENCE_TYPE, item.getEventType().name());
                        settingsService.store(DevoxxSettings.SAVED_CONFERENCE_ID, String.valueOf(item.getId()));
                    });
                }
                DevoxxView.SESSIONS.switchView();
            });
            
            setGraphic(root);
        } else {
            setGraphic(null);
        }
    }

    private Task<InputStream> loadBackgroundImage(Conference conference) {

        return new Task<InputStream>() {

            @Override
            protected InputStream call() throws Exception {
                final String eventImageFileName = conference.getId() + "_event_background_image.jpeg";
                final Optional<File> optionalRootDir = Services.get(StorageService.class).flatMap(StorageService::getPrivateStorage);
                if (optionalRootDir.isPresent()) {
                    final File rootDir = optionalRootDir.get();
                    File eventImageFile = new File(rootDir, eventImageFileName);
                    if (eventImageFile.exists()) {
                        return new FileInputStream(eventImageFile);
                    }
                }
                
                // File not found on local device, download from internet
                RestClient restClient = RestClient.create()
                        .method("GET")
                        .contentType("image/jpeg")
                        .host(conference.getImageURL())
                        .readTimeout(30000)
                        .connectTimeout(60000);
                final InputStream inputStream = restClient.createRestDataSource().getInputStream();
                
                // Write to file
                if (optionalRootDir.isPresent()) {
                    final File rootDir = optionalRootDir.get();
                    File eventImageFile = new File(rootDir, eventImageFileName);
                    writeToFile(inputStream, eventImageFile);
                    return new FileInputStream(eventImageFile);
                }
                
                // If no setting service is present, return the InputStream directly
                return inputStream;
            }
        };
    }

    private void writeToFile(InputStream inputStream, File eventImageFile) throws IOException {
        FileOutputStream writer = new FileOutputStream(eventImageFile);
        byte[] buffer = new byte[2048];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            writer.write(buffer, 0, len);
        }
    }
}
