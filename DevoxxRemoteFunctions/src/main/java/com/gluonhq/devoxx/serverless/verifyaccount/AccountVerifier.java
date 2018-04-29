/**
 * Copyright (c) 2018, Gluon Software
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
package com.gluonhq.devoxx.serverless.verifyaccount;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountVerifier {

    private static final Logger LOGGER = Logger.getLogger(AccountVerifier.class.getName());

    public String verify(String cfpEndpoint, String email, String password) throws IOException {
        if (cfpEndpoint.contains("cfp.devoxx.fr")) {
            return verifyDevoxxFr(email, password);
        } else {
            return verifyRegularCfp(cfpEndpoint, email, password);
        }
    }

    private String verifyDevoxxFr(String email, String password) throws IOException {
        String entity = Json.createObjectBuilder()
                .add("email", email)
                .add("password", password)
                .add("rememberMe", false)
                .build().toString();

        Response accountVerification = buildClient().target("https://my.devoxx.fr").path("pwa").path("login")
                .request()
                .post(Entity.json(entity));
        String response = accountVerification.readEntity(String.class);
        LOGGER.log(Level.INFO, "Account Verification Response: {0}", response);
        if (accountVerification.getStatus() == Response.Status.OK.getStatusCode()) {
            try (JsonReader reader = Json.createReader(new StringReader(response))) {
                JsonObject token = reader.readObject();
                return fetchUserProfileDevoxxFr(token.getString("token"));
            }
        } else if (accountVerification.getStatus() == Response.Status.BAD_REQUEST.getStatusCode() ||
                accountVerification.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new IOException("Invalid credentials");
        } else {
            throw new IOException(new WebApplicationException(accountVerification));
        }
    }

    private String fetchUserProfileDevoxxFr(String token) throws IOException {
        Response fetchUserProfile = buildClient().target("https://my.devoxx.fr").path("pwa").path("user")
                .request()
                .header("X-Auth-Token", token)
                .get();
        String response = fetchUserProfile.readEntity(String.class);
        LOGGER.log(Level.INFO, "FetchUserProfile Response: {0}", response);
        if (fetchUserProfile.getStatus() == Response.Status.OK.getStatusCode()) {
            try (JsonReader reader = Json.createReader(new StringReader(response))) {
                JsonObject user = reader.readObject();
                return Json.createObjectBuilder()
                        .add("identifier", user.getString("userID"))
                        .add("name", user.getString("firstName", "") + " " + user.getString("lastName", ""))
                        .add("picture", user.getString("avatarURL", ""))
                        .build().toString();
            }
        } else {
            throw new IOException("Invalid credentials");
        }
    }

    private String verifyRegularCfp(String cfpEndpoint, String email, String password) throws IOException {
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        formDataMultiPart.field("email", email);
        formDataMultiPart.field("password", password);

        Response accountVerification = buildClientRegularCfp().target(cfpEndpoint).path("account").path("credentials")
                .request()
                .header("X-Gluon", System.getenv("DEVOXX_CFP_X_GLUON_HEADER"))
                .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE));
        String response = accountVerification.readEntity(String.class);
        LOGGER.log(Level.INFO, "Account Verification Response: {0}", response);
        if (accountVerification.getStatus() == Response.Status.OK.getStatusCode()) {
            return "{\"identifier\":\"" + response + "\"}";
        } else if (accountVerification.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            throw new IOException("Invalid credentials");
        } else {
            throw new IOException(new WebApplicationException(accountVerification));
        }
    }

    private Client buildClient() {
        return ClientBuilder.newClient()
                .register(new LoggingFeature(LOGGER, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 4096));
    }

    private Client buildClientRegularCfp() {
        return buildClient()
                .register(MultiPartFeature.class)
                .register(HttpAuthenticationFeature.basic(System.getenv("DEVOXX_CFP_USERNAME"),
                        System.getenv("DEVOXX_CFP_PASSWORD")));
    }
}
