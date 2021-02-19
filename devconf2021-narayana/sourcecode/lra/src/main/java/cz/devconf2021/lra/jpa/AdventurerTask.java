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

package cz.devconf2021.lra.jpa;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "ADVENTURER_TASK")
@NamedQueries({
    @NamedQuery(name="AdventurerTask.findAll", query="SELECT b FROM AdventurerTask b"),
    @NamedQuery(name="AdventurerTask.findAllByType", query="SELECT b FROM AdventurerTask b WHERE b.type = :type"),
    @NamedQuery(name="AdventurerTask.findByLraId", query="SELECT b FROM AdventurerTask b WHERE b.lraId = :lraId")
})
public class AdventurerTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;
    private TaskType type;
    private TaskStatus status = TaskStatus.IN_PROGRESS;
    private String lraId;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AdventurerTask setName(String name) {
        this.name = name;
        return this;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public AdventurerTask setStatus(TaskStatus status) {
        this.status = status;
        return this;
    }

    public TaskType getType() {
        return type;
    }

    public AdventurerTask setType(TaskType type) {
        this.type = type;
        return this;
    }

    public String getLraId() {
        return lraId;
    }

    public AdventurerTask setLraId(String lraId) {
        this.lraId = lraId;
        return this;
    }

    @Override
    public String toString() {
        return String.format("[%d] adventurer task: %s, status: %s, lra id: %s",
                id, name, status, lraId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdventurerTask adventurerTask = (AdventurerTask) o;
        return id == adventurerTask.id && Objects.equals(name, adventurerTask.name) && status == adventurerTask.status && Objects.equals(lraId, adventurerTask.lraId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status, lraId);
    }
}
