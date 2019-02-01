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

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import io.narayana.demo.lra.devconf2019.jpa.Booking;

/**
 * Data manipulation for {@link Booking} entity.
 */
@Dependent
@Transactional
public class BookingManager {
    @PersistenceContext
    private EntityManager em;

    public void save(Booking booking) {
        em.persist(booking);
    }

    public void update(Booking booking) {
        em.merge(booking);
    }

    public Booking get(int id) {
        return em.find(Booking.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Booking> getAllBookings() {
        return em.createNamedQuery("Booking.findAll").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Booking> getByLraId(String lraId) {
        return em.createNamedQuery("Booking.findByLraId")
            .setParameter("lraId", lraId)
            .getResultList();
    }

    public Booking getFirstByLraId(String lraId) {
        List<Booking> bookings = getByLraId(lraId);
        if(bookings != null && !bookings.isEmpty()) return bookings.get(0);
        return null;
    }
}
