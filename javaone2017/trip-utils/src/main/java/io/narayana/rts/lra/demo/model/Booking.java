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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Booking {
    public enum BookingStatus {
        PROVISIONAL, CONFIRMED, CANCELLED
    }

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("status")
    private BookingStatus status;
    @JsonProperty("details")
    private Booking[] details;

    /**
     * @param id       ID
     * @param bookings Nested Bookings
     */
    public Booking(String id, Booking... bookings) {
        this(id, "Aggregate Booking", BookingStatus.PROVISIONAL, bookings);
    }

    /**
     * Individual booking
     *
     * @param id   ID
     * @param name Name of resource
     */
    public Booking(String id, String name) {
        this(id, name, BookingStatus.PROVISIONAL, null);
    }

    @JsonCreator
    public Booking(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("status") BookingStatus status,
                   @JsonProperty("details") Booking[] details) {

        init(id, name, status, details);
    }

    Booking(Booking booking) {
        this.init(booking.getId(), booking.getName(), booking.getStatus(), null);

        details = new Booking[booking.getDetails().length];

        IntStream.range(0, details.length).forEach(i -> details[i] = new Booking(booking.getDetails()[i]));
    }

    private void init(String id, String name, BookingStatus status, Booking[] details) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.status = status;
        this.details = details == null ? new Booking[0] : removeNullEnElements(details);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] removeNullEnElements(T[] a) {
        List<T> list = new ArrayList<T>(Arrays.asList(a));
        list.removeAll(Collections.singleton(null));
        return list.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), list.size()));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Booking[] getDetails() {
        return details;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String toString() {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"status\":\"%s\"}",
                id, name, status);
    }

    public boolean merge(Booking booking) {
        if (!id.equals(booking.getId()))
            return false; // or throw an exception

        name = booking.getName();
        status = booking.getStatus();

        return true;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Booking booking = (Booking) o;

        if (!getId().equals(booking.getId())) return false;
        if (!getName().equals(booking.getName())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getName().hashCode();
        return result;
    }

    public void merge(String responseData) throws URISyntaxException, IOException {
        responseData = responseData.replaceAll("\"", "");
        responseData = responseData.replaceAll("\\\\", "\"");
        List<Booking> bookingDetails = Arrays.asList(new ObjectMapper().readValue(responseData, Booking[].class));

        Map<String, Booking> bookings = bookingDetails.stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));

        // update tripBooking with bookings returned in the data returned from the trip setConfirmed request
        Arrays.stream(getDetails()) // the array of bookings in this trip booking
                .filter(b -> bookings.containsKey(b.getId())) // pick out bookings for which we have updated data
                .forEach(b -> b.merge(bookings.get(b.getId()))); // merge in the changes (returned from the setConfirmed request)
    }
}
