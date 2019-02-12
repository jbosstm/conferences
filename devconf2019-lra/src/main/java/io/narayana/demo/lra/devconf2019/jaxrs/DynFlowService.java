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

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.narayana.demo.lra.devconf2019.BookingManager;
import io.narayana.demo.lra.devconf2019.FlightManager;
import io.narayana.demo.lra.devconf2019.jpa.Booking;
import io.narayana.demo.lra.devconf2019.jpa.BookingStatus;
import io.narayana.demo.lra.devconf2019.jpa.Flight;

/**
 * <p>
 * This is a resource used by <a href="http://dynflow.github.io/">DynFlow</a> executor machinery.
 * This is used during presentation on <a href="https://sched.co/JcgU">DevConf.cz 2019</a>.
 * <p>
 * This resource provides two endpoints that DynFlow uses. First pretends the work to be done (<code>/book</code>).
 * The second is called when work fails and compensation should be run (<code>/{id}/compensate</code>).
 */
@Path("/dynflow")
public class DynFlowService {
    private static final Logger log = Logger.getLogger(DynFlowService.class);
    private static volatile int nameCounter = 1;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private FlightManager flightManager;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response book() {
        // finding a flight that exists in this project database, for this to work we expect the 'init.csv'
        //   is used and data from 'flight.data' is loaded during startup
        Flight flight = flightManager.getByDate(FlightManagementService.parseDate("2019-01-27")).get(0);
        Booking booking = new Booking()
                .setFlight(flight)
                .setStatus(BookingStatus.BOOKED)
                .setName("The great guy " + (nameCounter++));
        bookingManager.save(booking);
        log.infof("Created booking: '%s'", booking);

        return Response.ok(booking).build();
    }

    @POST
    @Path("/{id}/compensate")
    @Produces(MediaType.TEXT_PLAIN)
    public Response compensate(@PathParam("id") String bookingId) {
        int id = Integer.parseInt(bookingId);
        Booking booking = bookingManager.get(id);
        booking.setStatus(BookingStatus.CANCELED);
        log.infof("Compensating booking with id '%s' of id '%d'", booking, id);
        bookingManager.update(booking);
        return Response.ok("compensated").build();
    }
}
