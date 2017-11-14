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
package io.narayana.rts.lra.demo.flight;

import io.narayana.lra.annotation.Compensate;
import io.narayana.lra.annotation.Complete;
import io.narayana.lra.annotation.LRA;
import io.narayana.lra.annotation.NestedLRA;
import io.narayana.lra.client.LRAClient;
import io.narayana.lra.client.LRAClientAPI;
import io.narayana.rts.lra.demo.model.Booking;
import io.narayana.rts.lra.demo.model.BookingStore;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;

@RequestScoped
@Path("/")
public class FlightMicroservice {
    @Inject
    private BookingStore bookingStore;

    @Inject
    private LRAClientAPI lraClientAPI;

    // Business logic

    @PUT
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Booking cancel(@PathParam("bookingId") String bookingId) throws MalformedURLException {
        lraClientAPI.cancelLRA(new URL(bookingId));
        return bookingStore.get(bookingId);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @LRA
    @NestedLRA
    public Booking reserve(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId, @QueryParam("name") String name) {
        Booking booking = new Booking(bookingId, name);
        bookingStore.add(booking);
        return booking;
    }

    // Participant

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/complete")
    @Complete
    public Booking complete(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) {
        return bookingStore.update(bookingId, Booking.BookingStatus.CONFIRMED);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/compensate")
    @Compensate
    public Booking compensate(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) {
        return bookingStore.update(bookingId, Booking.BookingStatus.CANCELLED);
    }
}
