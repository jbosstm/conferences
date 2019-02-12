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

package io.narayana.demo.lra.devconf2019;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.narayana.demo.lra.devconf2019.jpa.Flight;

/**
 * <p>
 * Running at application boot-up.
 * <p>
 * This gets csv file from file defined with <code>init.csv</code> property.
 * The csv file is defined of having three columns
 * <p>
 * <ul>
 *   <li>flight date in format <code>YYYY-MM-DD</code></li>
 *   <li>number of seats at flight</li>
 *   <li>number of seats already reserved at the flight</li>
 * </ul>
 * <p>
 * See the example at file <code>flight.data</code>.
 */
@ApplicationScoped
public class StartUp {
    private static final Logger log = Logger.getLogger(StartUp.class);

    @Inject @ConfigProperty(name = "init.csv")
    private String pathToCsvInitFile;

    @Inject
    private FlightManager flighthManagement;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.debug("startup routine was succesfully initialized");
        if(pathToCsvInitFile != null && !pathToCsvInitFile.isEmpty()) {
            log.infof("Going to load data from CSV file at '%s'", pathToCsvInitFile);
            loadCsv(pathToCsvInitFile);
        }
    }



    public void loadCsv(String pathString) {
        Path pathToCsv = Paths.get(pathString);
        if(!pathToCsv.toFile().isFile()) {
            log.warnf("Path '%s' is not correct path to a file. No data to load.", pathString);
            return;
        }
        try {
            Files.lines(pathToCsv).forEach(line -> {
                String[] lineSplit = line.split(";");
                if(lineSplit.length != 3) {
                    log.warnf("Cannot parse line '%s' from file '%s' as expecting to have"
                            + " 3 values to be loaded to DB", line, pathString);
                    return;
                }
                Flight flight = new Flight();
                try {
                    flight.setDate(new SimpleDateFormat(Flight.DATE_FORMAT).parse(lineSplit[0]));
                } catch (ParseException pe) {
                    log.warnf("Cannot parse line '%s' from file '%s' as parsing of date '%s' failed",
                        line, pathString, lineSplit[0]);
                    return;
                }
                try {
                    flight.setNumberOfSeats(Integer.parseInt(lineSplit[1]));
                    flight.setBookedSeats(Integer.parseInt(lineSplit[2]));
                } catch (NumberFormatException nfe) {
                    log.warnf("Cannot parse line '%s' from file '%s' as parsing of one from numbers '%s', '%s' failed",
                            line, pathString, lineSplit[1], lineSplit[2]);
                    return;
                }
                // save loaded csv data through jpa
                flighthManagement.save(flight);
                log.infof("Saved flight: %s", flight);
            });
        } catch(IOException ioe) {
            log.errorf(ioe, "Cannot load and process CSV file at '%s'", pathString);
            return;
        }
    }
}
