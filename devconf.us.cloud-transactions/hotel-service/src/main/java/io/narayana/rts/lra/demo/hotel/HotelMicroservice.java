/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.rts.lra.demo.hotel;

import io.narayana.rts.lra.demo.model.Booking;
import io.narayana.rts.lra.demo.model.BookingStore;
import io.narayana.rts.lra.demo.model.BookingStores;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.CompensatorStatus;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.client.LRAClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@ApplicationScoped
@Path("/")
public class HotelMicroservice {
    @Context
    private UriInfo context;

    @Inject
    private BookingStores bookingStores;

    // Business logic

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @LRA
    public Booking reserve(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId, @QueryParam("name") String name) {
        traceRequest();
        Booking booking = new Booking(bookingId, name);
        getStore().add(booking);
        return booking;
    }

    @GET
    @Path("/booking/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Booking get(@PathParam("bookingId") String bookingId) {
        traceRequest();
        return getStore().get(bookingId);
    }

    // Participant

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/complete")
    @Complete
    public Booking complete(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) {
        traceRequest();
        return getStore().update(bookingId, Booking.BookingStatus.CONFIRMED);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/compensate")
    @Compensate
    public Booking compensate(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) {
        traceRequest();
        return getStore().update(bookingId, Booking.BookingStatus.CANCELLED);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    public CompensatorStatus getStatus(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) {
        traceRequest();
        Booking booking = getStore().get(bookingId);

        if (booking == null) {
            throw new NotFoundException();
        }

        switch (booking.getStatus()) {
            case CANCELLED:
                return CompensatorStatus.Compensated;
            case CONFIRMED:
                return CompensatorStatus.Completed;
            default:
                return null;
        }
    }

    private void traceRequest() {
        System.out.printf("%s%n", context.getRequestUri());
    }

    @PostConstruct
    private void initController() {
        getStore().restore();
    }

    private BookingStore getStore() {
        return bookingStores.getStore("hotel");
    }
}
