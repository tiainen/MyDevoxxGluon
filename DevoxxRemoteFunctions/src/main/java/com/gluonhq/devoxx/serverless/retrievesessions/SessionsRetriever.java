package com.gluonhq.devoxx.serverless.retrievesessions;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SessionsRetriever {

    private static final Logger LOGGER = Logger.getLogger(SessionsRetriever.class.getName());

    private static final Client client = ClientBuilder.newClient();

    public String retrieve(String cfpEndpoint, String conferenceId) throws IOException {
        Response schedules = client.target(cfpEndpoint).path("conferences").path(conferenceId).path("schedules/")
                .request().get();
        if (schedules.getStatus() == Response.Status.OK.getStatusCode()) {
            try (JsonReader schedulesReader = Json.createReader(new StringReader(schedules.readEntity(String.class)))) {
                List<String> dayLinks = schedulesReader.readObject().getJsonArray("links").getValuesAs(JsonObject.class).stream()
                        .filter(schedule -> schedule.containsKey("href"))
                        .map(schedule -> schedule.getString("href").replaceFirst("http://", "https://"))
                        .collect(Collectors.toList());

                JsonArrayBuilder sessions = Json.createArrayBuilder();
                for (String dayLink : dayLinks) {
                    Response slots = client.target(dayLink).request().get();
                    if (slots.getStatus() == Response.Status.OK.getStatusCode()) {
                        try (JsonReader slotsReader = Json.createReader(new StringReader(slots.readEntity(String.class)))) {
                            slotsReader.readObject().getJsonArray("slots").getValuesAs(JsonObject.class).stream()
                                    .filter(slot -> slot.containsKey("talk") && slot.get("talk").getValueType() == JsonValue.ValueType.OBJECT)
                                    .forEach(sessions::add);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Failed processing link " + dayLink + ": " + slots.readEntity(String.class));
                    }
                }
                return sessions.build().toString();
            }
        } else {
            throw new IOException(new WebApplicationException(schedules));
        }
    }
}
