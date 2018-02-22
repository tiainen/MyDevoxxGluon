package com.gluonhq.devoxx.serverless.retrievesessions;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("sessions")
public class SessionsHandler {

    @POST
    @Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
    public Response handleResponse(@FormParam("cfpEndpoint") String cfpEndpoint,
            @FormParam("conferenceId") String conferenceId) throws IOException {
        return Response.ok(new SessionsRetriever().retrieve(cfpEndpoint, conferenceId)).build();
    }
}
