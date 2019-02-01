/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package io.narayana.demo.lra.devconf2019.jaxrs;


import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.CompensatorStatus;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.client.LRAClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.narayana.demo.lra.devconf2019.BookingManager;
import io.narayana.demo.lra.devconf2019.FlightManager;
import io.narayana.demo.lra.devconf2019.jpa.Booking;
import io.narayana.demo.lra.devconf2019.jpa.BookingStatus;
import io.narayana.demo.lra.devconf2019.jpa.Flight;

/**
 * <p>
 * Demo REST api showing the usage of <a href="https://github.com/eclipse/microprofile-lra">LRA</a>.
 * <p>
 */
@Path("/book")
public class FlightBookingService {
    private static final Logger log = Logger.getLogger(FlightBookingService.class);

    @Inject
    private BookingManager bookingManager;

    @Inject
    private FlightManager flightManager;

    @Inject
    private LRAClient lraClient;

    @Inject @ConfigProperty(name = "target.call", defaultValue = "")
    private String targetCallConfig;

    /**
     * <p>
     * Creating LRA, making booking by saving it to database and calling
     * a next service if <code>target.call</code> property is defined.
     * <p>
     * Expecting JSON data in format:
     * <code>{"date":"2019-01-27", "name": "Name of passenger", "target.call": "http://ruby-api:4567/fail"}</code>
     */
    @LRA
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response book(String jsonData) {
        // getting json to map
        Map<String,String> jsonMap = parseJson(jsonData);

        // getting a flight that is requested based date provided in json
        Flight matchingFlight = findMatchingFlightForBooking(jsonMap);

        // will we be calling a next service?
        String targetCallFromJson = jsonMap.get("target.call");

        log.infof("Booking with data '%s' as part of LRA id '%s', trying to call at '%s'",
                jsonData, lraClient.getCurrent().toExternalForm(), targetCallFromJson);

        // do database changes
        Booking booking = new Booking()
                .setFlight(matchingFlight)
                .setName(jsonMap.get("name"))
                .setStatus(BookingStatus.IN_PROGRESS)
                .setLraId(lraClient.getCurrent().toExternalForm());
        bookingManager.save(booking);
        log.infof("Booking '%s' was created", booking);

        // calling next service
        if(targetCallFromJson != null && !targetCallFromJson.isEmpty()) {
            Response response = null;
            boolean shouldBeCanceled = false;
            try {
                response = ClientBuilder.newClient().target(targetCallFromJson)
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.text("calling to book a hotel for person: " + booking.getName()));

                String entityBody = response.readEntity(String.class);
                int returnCode = response.getStatus();
                log.infof("Response code from call '%s' was %s, entity: %s", targetCallFromJson, returnCode, entityBody);

                // ruby app (https://github.com/adamruzicka/microservice-ruby-dc2019) returns 200/OK but body contains info
                if(entityBody.contains("rejected")) shouldBeCanceled = true;
            } catch (Exception e) {
                log.errorf(e, "Failed to call '%s': %s", targetCallFromJson, e.getMessage());
                shouldBeCanceled = true;
            }
            if(shouldBeCanceled) lraClient.cancelLRA(lraClient.getCurrent());
        }

        return Response.ok(booking.getId()).build();
    }

    /**
     * Enpoint expecting incoming LRA ID header. It search for existing booking with the LRA id
     * and if there is such it creates new booking.
     */
    @LRA
    @POST
    @Path("/in-chain")
    public Response bookInChain(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        if(lraId == null || lraId.isEmpty())
            throw new WebApplicationException("LRA ID header [" + LRAClient.LRA_HTTP_HEADER + "] is empty", Response.Status.PRECONDITION_FAILED);
        Booking byLraBooking = bookingManager.getFirstByLraId(lraId);
        if(byLraBooking == null) {
            log.warnf("There is no LRA id '%s' in the database. No updates done.", lraId);
            return Response.status(Response.Status.NOT_FOUND).entity(
                    Entity.text("No LRA with id " + lraId + " found for bookings")).build();
        } else {
            Booking booking = new Booking()
                .setFlight(byLraBooking.getFlight())
                .setName(byLraBooking.getName())
                .setStatus(BookingStatus.IN_PROGRESS)
                .setLraId(lraId);
            bookingManager.save(booking);
            log.infof("Booking in-chain '%s' was created", booking);
            return Response.ok(booking.getId()).build();
        }
    }

    /**
     * LRA complete
     */
    @PUT
    @Path("/complete")
    @Produces(MediaType.TEXT_PLAIN)
    @Complete
    public Response completeWork(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException, JsonProcessingException {
        log.info("Completing...");
        boolean wasBooked = confirmBooking(lraId);
        log.infof("LRA ID '%s' was completed", lraId);
        CompensatorStatus completeStatus = wasBooked ? CompensatorStatus.Completed : CompensatorStatus.FailedToComplete;
        return Response.ok(completeStatus.name()).build();
    }

    /**
     * LRA compensate
     */
    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException, JsonProcessingException {
        log.info("Compensating...");
        cancelBooking(lraId);
        log.warnf("LRA ID '%s' was compensated", lraId);
        return Response.ok(CompensatorStatus.Compensated.name()).build();
    }

    /**
     * When somebody want to list all the booking.
     * In fact just listing database by <code>SELECT *</code>.
     */
    @Path("/all")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<Booking> allBookings = bookingManager.getAllBookings();
        if(log.isDebugEnabled()) log.debugf("All flights: %s", allBookings);
        return Response.ok().entity(allBookings).build();
    }


    @SuppressWarnings("unchecked")
    private Map<String,String> parseJson(String jsonData) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String,String> jsonMap = objectMapper.readValue(jsonData, HashMap.class);
            if(log.isDebugEnabled())
                log.debugf("The incoming body '%s' was parsed for JSON format '%s'", jsonData, jsonMap);
            return jsonMap;
        } catch (IOException ioe) {
            log.errorf("Cannot parse the provided body '%s' to JSON format", jsonData);
            lraClient.cancelLRA(lraClient.getCurrent());
            throw new WebApplicationException(ioe, Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(String.format("Cannot parse the provided body '%s' to JSON format", jsonData))
                    .type("text/plain").build());
        }
    }

    private Flight findMatchingFlightForBooking(Map<String,String> jsonMap) {
        if(jsonMap.get("date") == null || jsonMap.get("name") == null) {
            lraClient.cancelLRA(lraClient.getCurrent());
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(String.format("Invalid format of json data '%s' as does not contain fields 'date' and/or 'name'", jsonMap))
                    .type("text/plain").build());
        }

        Date parsedDate = FlightManagementService.parseDate(jsonMap.get("date"));
        List<Flight> foundFlights = flightManager.getByDate(parsedDate);
        if(foundFlights == null || foundFlights.isEmpty()) {
            log.errorf("No flight at date '%s' is available", parsedDate);
            lraClient.cancelLRA(lraClient.getCurrent());
            throw new NotFoundException(String.format("No flight at date '%s' is available", parsedDate));
        }

        Optional<Flight> matchingFlight = foundFlights.stream()
                .filter(f -> f.getNumberOfSeats() > f.getBookedSeats()).findFirst();
        if(!matchingFlight.isPresent()) {
            log.errorf("There is no flight which would not be already occupied at the date '%s'", parsedDate);
            lraClient.cancelLRA(lraClient.getCurrent());
            throw new NotFoundException("There is no flight which would not be already occupied at the date " + parsedDate);
        }

        return matchingFlight.get();
    }

    private boolean confirmBooking(String lraId) {
        boolean wasSuccesful = true;
        List<Booking> byLraBookings = bookingManager.getByLraId(lraId);
        for(Booking booking: byLraBookings) {
            booking.setStatus(BookingStatus.BOOKED);
            int bookedSeats = booking.getFlight().getBookedSeats();
            int availableSeats = booking.getFlight().getNumberOfSeats();

            if(bookedSeats + 1 > availableSeats) {
                log.errorf("Cannot finish booking '%s' for LRA '%s'. The flight '%s' is already full.",
                        booking, lraId, booking.getFlight());
                wasSuccesful = false;
            }

            flightManager.update(booking.getFlight().setBookedSeats(bookedSeats + 1));
            bookingManager.update(booking);
            log.infof("Confirmed booking: '%s'", booking);
        }
        return wasSuccesful;
    }

    private void cancelBooking(String lraId) {
        List<Booking> byLraBookings = bookingManager.getByLraId(lraId);
        for(Booking booking: byLraBookings) {
            booking.setStatus(BookingStatus.CANCELED);
            bookingManager.update(booking);
            log.infof("Undone booking: '%s'", booking);
        }
    }
}
