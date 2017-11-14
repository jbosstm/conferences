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
package io.narayana.rts.lra.demo.tripcontroller;

import io.narayana.lra.annotation.LRA;
import io.narayana.lra.client.LRAClient;
import io.narayana.lra.client.LRAClientAPI;
import io.narayana.rts.lra.demo.model.Booking;
import io.narayana.rts.lra.demo.model.BookingStore;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

@RequestScoped
@Path("/")
public class TripMicroservice {
    @Inject
    private BookingStore bookingStore;
    private Client hotelClient, flightClient;
    private WebTarget hotelTarget, flightTarget;

    @Inject
    private LRAClientAPI lraClientAPI;

    // Business logic

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(delayClose = true, join = false)
    public Booking reserve(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String bookingId) throws UnsupportedEncodingException {
        Booking theGrand = reserve(false, "TheGrand");
        Booking firstClass = reserve(true, "firstClass");
        Booking economy = reserve(true, "economy");
        Booking trip = new Booking(bookingId, theGrand, firstClass, economy);
        bookingStore.add(trip);

        Booking updatedBooking = cancel(firstClass);
        firstClass.merge(updatedBooking);
        return trip;
    }

    @PUT
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Booking confirm(@PathParam("bookingId") String bookingId) throws IOException, URISyntaxException {
        String responseData = lraClientAPI.closeLRA(new URL(bookingId));
        Booking booking = bookingStore.update(bookingId, Booking.BookingStatus.CONFIRMED);
        booking.merge(responseData);
        return booking;
    }

    @DELETE
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Booking cancel(@PathParam("bookingId") String bookingId) throws IOException, URISyntaxException {
        String responseData = lraClientAPI.cancelLRA(new URL(bookingId));
        Booking booking = bookingStore.update(bookingId, Booking.BookingStatus.CANCELLED);
        booking.merge(responseData);
        return booking;
    }

    // JAX-RX interaction

    @PostConstruct
    private void initController() throws MalformedURLException {
        hotelClient = ClientBuilder.newClient();
        flightClient = ClientBuilder.newClient();
        hotelTarget = hotelClient.target(new URL("http://" + System.getProperty("hotel.service.http.host", "localhost") + ":" + Integer.getInteger("hotel.service.http.port", 8082)).toExternalForm());
        flightTarget = flightClient.target(new URL("http://" + System.getProperty("flight.service.http.host", "localhost") + ":" + Integer.getInteger("flight.service.http.port", 8083)).toExternalForm());
    }

    @PreDestroy
    private void finiController() {
        hotelClient.close();
        flightClient.close();
    }

    private Booking reserve(boolean flight, String name) {
        WebTarget webTarget = (flight ? flightTarget : hotelTarget).path("/").queryParam("name", name);
        Response response = webTarget.request().post(Entity.text(""));
        Booking booking = response.readEntity(Booking.class);
        return booking;
    }

    private Booking cancel(Booking booking) throws UnsupportedEncodingException {
        WebTarget webTarget = flightTarget.path("/" + URLEncoder.encode(booking.getId(), "UTF-8"));
        Response response = webTarget.request().put(Entity.text(""));
        booking = response.readEntity(Booking.class);
        return booking;
    }
}

