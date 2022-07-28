package io.xstefank.client;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletionStage;

@RegisterRestClient(baseUri = "http://localhost:8082")
@Path("/airline")
public interface AirlineClient {

    @POST
    @Path("/book")
    void bookFlight(String id);
}
