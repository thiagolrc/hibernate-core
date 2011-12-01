/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.collection;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
/**
 * EmbeddableList with components with a manyToOne relation (referencing some entity)
 * @author T.Lourenconi
 *
 */
@Entity
@Audited
public class EmbeddableListEntity2 {
    @Id
    @GeneratedValue
    private Integer id;
    
    @ElementCollection
    @OrderColumn
    private List<ManyToOneComponent> componentList = new ArrayList<ManyToOneComponent>();

    public EmbeddableListEntity2() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<ManyToOneComponent> getComponentList()
    {
      return componentList;
    }

    public void setComponentList(List<ManyToOneComponent> componentList)
    {
      this.componentList = componentList;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddableListEntity2)) return false;

        EmbeddableListEntity2 that = (EmbeddableListEntity2) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "ECLE(id = " + id + ", componentList = " + componentList + ')';
    }
}