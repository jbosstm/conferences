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

package io.narayana.demo.lra.devconf2019.jpa;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.narayana.demo.lra.devconf2019.StartUp;

/**
 * <p>
 * An entity saving data about flights. The business logic checks if there is enough
 * seats to book a new one on the flight.
 * <p>
 * Some base set of the entities are created during statup by {@link StartUp}.
 */
@Entity
@Table(name = "FLIGHTS")
@NamedQueries({
    @NamedQuery(name="Flight.findAll", query="SELECT f FROM Flight f"),
    @NamedQuery(name="Flight.findByDate", query="SELECT f FROM Flight f WHERE f.date = :date"),
})
public class Flight implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "flight")
    private List<Booking> bookings = new ArrayList<>();

    private Date date;
    private int numberOfSeats, bookedSeats;

    public int getId() {
        return id;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public Flight setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
        return this;
    }

    public int getBookedSeats() {
        return bookedSeats;
    }

    public Flight setBookedSeats(int bookedSeats) {
        this.bookedSeats = bookedSeats;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public String getDateFormated() {
        return new SimpleDateFormat(DATE_FORMAT).format(getDate());
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return String.format("[%s] date: %s, seats: %d, booked: %d",
                id, getDateFormated(), numberOfSeats, bookedSeats);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bookedSeats;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + id;
        result = prime * result + numberOfSeats;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Flight other = (Flight) obj;
        if (bookedSeats != other.bookedSeats)
            return false;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (id != other.id)
            return false;
        if (numberOfSeats != other.numberOfSeats)
            return false;
        return true;
    }
}
