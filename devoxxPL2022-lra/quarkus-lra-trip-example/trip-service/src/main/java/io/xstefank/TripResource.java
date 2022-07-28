package io.xstefank;

import io.xstefank.client.AirlineClient;
import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/trip")
@ApplicationScoped
public class TripResource {

    @Inject
    @RestClient
    AirlineClient airlineClient;

    @LRA
    @GET
    @Path("/book")
    public String bookTrip(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Booking new trip " + lraId);

        try {
            airlineClient.bookFlight(lraId);
        } catch (Throwable throwable) {
            // intentionally empty
        }

        return "Booking will be processed";
    }

    @LRA(end = true)
    @GET
    @Path("/end-trip")
    public Response endTripBooking(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Ending booking for " + lraId);

        return Response.ok().build();
    }

    @AfterLRA
    @PUT
    @Path("/bookingProcessed")
    public Response bookingProcessed(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) String lraId, LRAStatus status) {
        logNicely("Booking processed " + lraId + ", " + status);
        return Response.ok().build();
    }

    private void logNicely(String value) {
        System.out.println("===============");
        System.out.println("value = " + value);
        System.out.println("===============");
        System.out.println();
    }
}
