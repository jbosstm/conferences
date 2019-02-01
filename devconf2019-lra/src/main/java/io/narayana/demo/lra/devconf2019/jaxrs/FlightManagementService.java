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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.narayana.demo.lra.devconf2019.FlightManager;
import io.narayana.demo.lra.devconf2019.jpa.Flight;

/**
 * A REST API to manipulate with flights ({@link Flight} entity).
 */
@Path("/flights")
public class FlightManagementService {
    private static final Logger log = Logger.getLogger(FlightManager.class);

    @Inject
    private FlightManager management;

    /**
     * Parsing date of format <code>YYYY-MM-DD</code> for a {@link Date}
     * that could be used in the application.
     *
     * @param stringDate  date in format {@value Flight#DATE_FORMAT}
     * @return {@link Date} object of parsed string of stringDate parameter
     */
    public static Date parseDate(String stringDate) {
        try {
            return new SimpleDateFormat(Flight.DATE_FORMAT).parse(stringDate);
        } catch (ParseException pe) {
            throw new WebApplicationException(pe, Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity(String.format("Cannot parse parameter date '%s' for the date format '" + Flight.DATE_FORMAT + "'", stringDate))
                    .type("text/plain").build());
        }
    }

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        List<Flight> flights = management.getAllFlights();
        if(log.isDebugEnabled()) log.debugf("All flights: %s", flights);
        return Response.ok().entity(flights).build();
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlight(@PathParam("id") String flightId) throws NotFoundException {
        int id = getFlightId(flightId);
        Flight flight = management.find(id);
        if(flight == null) {
            throw new NotFoundException(String.format("Cannot find a flight with id '%d'", id));
        }
        log.debugf("On requested id '%s' found a flight '%s'", id, flight);
        return Response.ok().entity(flight).build();
    }

    @Path("/date/{date}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlightByDate(@PathParam("date") String date) {
        Date toFindDate = parseDate(date);
        List<Flight> byDateflights = management.getByDate(toFindDate);
        log.debugf("On requested date '%s' found flights '%s'", date, byDateflights);
        return Response.ok().entity(byDateflights).build();
    }

    @Path("/add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addFlight(Flight flight) throws NotFoundException {
        log.debug("Adding flight: " + flight);
        management.save(flight);
        return Response.ok().entity(flight.getId()).build();
    }

    @Path("/{id}")
    @DELETE
    public Response deleteFlight(@PathParam("id") String flightId) throws NotFoundException {
        int id = getFlightId(flightId);
        Flight flight = management.find(id);
        if(flight == null) {
            throw new NotFoundException(
                    String.format("Cannot delete the flight with id '%d' as the id does not exist.", id));
        }
        log.debug("Adding flight: " + flight);
        management.delete(flight);
        return Response.ok().entity(id).build();
    }

    private int getFlightId(String flightId) {
        try {
            return Integer.parseInt(flightId);
        } catch (NumberFormatException nfe) {
            throw new WebApplicationException(nfe, Response.status(Response.Status.PRECONDITION_FAILED)
                .entity(String.format("The path parameter '%s' of flight id can't be converted to number",flightId))
                .type("text/plain").build());
        }
    }
}
