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
package io.narayana.rts.lra.demo.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//@ApplicationScoped
public class BookingStore {
    // A simple data store for bookings
    private Map<String, Booking> bookings = new HashMap<>();
    private Properties properties = new Properties();
    private String storeName;

    public BookingStore(String storeName) {
        this.storeName = storeName;
    }

    public void add(Booking booking) {
        trace("adding booking", booking, false);

        bookings.put(booking.getId(), booking);
        save();
    }

    public Booking get(String bookingId) {
        return bookings.get(bookingId);
    }

    public Booking update(String bookingId, Booking.BookingStatus status) {
        if (!bookings.containsKey(bookingId)) {
            trace("update booking: id not found", null, true);
            throw new RuntimeException("Booking id not found: " + bookingId);
        }

        Booking booking = bookings.get(bookingId);
        booking.setStatus(status);

        return booking;
    }

    private void save() {
        properties.clear();

        for (Map.Entry<String,Booking> entry : bookings.entrySet()) {
            properties.put(entry.getKey(), encode(entry.getValue()));
        }

        try {
            properties.store(new FileOutputStream(storeName + ".bookings"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        trace("saved bookings", null, true);
    }

    public void restore() {
        File f = new File(storeName + ".bookings");

        if (f.isFile() && f.canRead()) {
            try {
                properties.load(new FileInputStream(f));

                for (String key : properties.stringPropertyNames()) {
                    System.out.printf("Store %s: restoring %s: %s%n", storeName, key, properties.get(key));
                    bookings.put(key, decode(properties.get(key).toString()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        printBookings();
    }

    private String encode(Booking booking) {
        return String.format("%s,%s,%s", booking.getId(), booking.getName(), booking.getStatus());
    }

    private Booking decode(String encoding) {
        String[] fields = encoding.split(",");

        return new Booking(fields[0], fields[1], Booking.BookingStatus.valueOf(fields[2]), null);
    }

    private void printBookings() {
        bookings.forEach((k, v) -> System.out.printf("\t%s (%s)%n", k, v));
    }

    private void trace(String msg, Booking booking, boolean showBookings) {
        String id = booking != null ? booking.getId() : "null";

        System.out.printf("%s: %s %s (%s)%n",
                storeName, msg,  id, booking);

        if (showBookings) {
            printBookings();
        }
    }
}
