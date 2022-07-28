package io.xstefank;

import io.xstefank.client.HotelClient;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/airline")
@ApplicationScoped
public class AirlineResource {

    @Inject
    @RestClient
    HotelClient hotelClient;

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @POST
    @Path("/book")
    public Response bookFlight(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Booking flight for " + lraId);

        // propagate the saga to the hotel after the flight is booked
        try {
            hotelClient.bookHotel(lraId);
        } catch (Throwable throwable) {
            // intentionally empty
        }

        return Response.ok().build();
    }

    @Compensate
    @PUT
    @Path("/compensate")
    public Response compensateFlight(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Compensate for flight " + lraId);

        return Response.ok().build();
    }

    @Complete
    @PUT
    @Path("/complete")
    public Response completeFlight(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Complete for flight " + lraId);

        return Response.ok().build();
    }

    private void logNicely(String value) {
        System.out.println("===============");
        System.out.println("value = " + value);
        System.out.println("===============");
        System.out.println();
    }
}
