package com.gluonhq.devoxx.serverless.retrievesessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class SessionsLambda implements RequestStreamHandler {

    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        try (JsonReader reader = Json.createReader(input)) {
            JsonObject jsonInput = reader.readObject();
            String cfpEndpoint = jsonInput.getString("cfpEndpoint");
            String conferenceId = jsonInput.getString("conferenceId");
            String jsonOutput = new SessionsRetriever().retrieve(cfpEndpoint, conferenceId);
            try (Writer writer = new OutputStreamWriter(output)) {
                writer.write(jsonOutput);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        InputStream input = new ByteArrayInputStream("{\"cfpEndpoint\":\"https://cfp.devoxx.be/api\",\"conferenceId\":\"DVBE17\"}".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new SessionsLambda().handleRequest(input, output, null);
        System.out.println("output = " + new String(output.toByteArray(), StandardCharsets.UTF_8));
    }
}
