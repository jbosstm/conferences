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

import java.util.Date;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import io.narayana.demo.lra.devconf2019.jpa.Flight;

/**
 * Data manipulation for {@link Flight} entity.
 */
@Dependent
@Transactional
public class FlightManager {
    @PersistenceContext
    private EntityManager em;

    public void save(Flight flight) {
        em.persist(flight);
    }

    public void update(Flight flight) {
        em.merge(flight);
    }

    public void delete(Flight flight) {
        flight = em.merge(flight);
        em.remove(flight);
    }

    public Flight find(int id) {
        return em.find(Flight.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Flight> getAllFlights() {
        return em.createNamedQuery("Flight.findAll").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Flight> getByDate(Date date) {
        return em.createNamedQuery("Flight.findByDate")
            .setParameter("date", date)
            .getResultList();
    }
}
