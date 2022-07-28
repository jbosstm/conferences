package io.xstefank;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/car")
@ApplicationScoped
public class CarResource {

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @POST
    @Path("/book")
    public Response bookCarRental(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Booking car rental for " + lraId);

        return Response.ok().build();
//        return Response.status(500).build();
    }

    @Compensate
    @PUT
    @Path("/compensate")
    public Response compensateCarRental(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Compensate for car rental " + lraId);

        return Response.ok().build();
    }

    @Complete
    @PUT
    @Path("/complete")
    public Response completeCarRental(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        logNicely("Complete for car rental " + lraId);

        return Response.ok().build();
    }

    private void logNicely(String value) {
        System.out.println("===============");
        System.out.println("value = " + value);
        System.out.println("===============");
        System.out.println();
    }
}
