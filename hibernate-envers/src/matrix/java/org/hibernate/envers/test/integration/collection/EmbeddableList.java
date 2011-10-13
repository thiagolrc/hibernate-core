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
package org.hibernate.envers.test.integration.collection;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.entities.components.Component4;
import org.junit.Test;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
public class EmbeddableList extends AbstractEntityTest {
    private Integer ecle1_id;
    private final Component4 c4_1 = new Component4("c41", "c41_value", "c41_description");
    private final Component4 c4_2 = new Component4("c42", "c42_value2", "c42_description");
    private final Component3 c3_1 = new Component3("c31", c4_1, c4_2);
    private final Component3 c3_2 = new Component3("c32", c4_1, c4_2);

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbeddableListEntity.class);
    }

    @Test
    public void initData() {
        EntityManager em = getEntityManager();

        EmbeddableListEntity ecle1 = new EmbeddableListEntity();

        // Revision 1 (ecle1: initially 1 element in both collections)
        em.getTransaction().begin();

        ecle1.getComponentList().add(c3_1);

        em.persist(ecle1);

        em.getTransaction().commit();

        // Revision (still 1) (ecle1: removing non-existing element)
        em.getTransaction().begin();

        ecle1 = em.find(EmbeddableListEntity.class, ecle1.getId());

        ecle1.getComponentList().remove(c3_2);

        em.getTransaction().commit();

        // Revision 2 (ecle1: adding one element)
        em.getTransaction().begin();

        ecle1 = em.find(EmbeddableListEntity.class, ecle1.getId());

        ecle1.getComponentList().add(c3_2);

        em.getTransaction().commit();

        // Revision 3 (ecle1: adding one existing element)
        em.getTransaction().begin();

        ecle1 = em.find(EmbeddableListEntity.class, ecle1.getId());

        ecle1.getComponentList().add(c3_1);

        em.getTransaction().commit();

        // Revision 4 (ecle1: removing one existing element)
        em.getTransaction().begin();

        ecle1 = em.find(EmbeddableListEntity.class, ecle1.getId());

        ecle1.getComponentList().remove(c3_2);

        em.getTransaction().commit();

        ecle1_id = ecle1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(getAuditReader().getRevisions(EmbeddableListEntity.class, ecle1_id), Arrays.asList(1, 2, 3, 4));
    }

    @Test
    public void testHistoryOfEcle1() {
        EmbeddableListEntity rev1 = getAuditReader().find(EmbeddableListEntity.class, ecle1_id, 1);
        EmbeddableListEntity rev2 = getAuditReader().find(EmbeddableListEntity.class, ecle1_id, 2);
        EmbeddableListEntity rev3 = getAuditReader().find(EmbeddableListEntity.class, ecle1_id, 3);
        EmbeddableListEntity rev4 = getAuditReader().find(EmbeddableListEntity.class, ecle1_id, 4);

        assertEquals(rev1.getComponentList(), Collections.singletonList(c3_1));
        assertEquals(rev2.getComponentList(), Arrays.asList(c3_1, c3_2));
        assertEquals(rev3.getComponentList(), Arrays.asList(c3_1, c3_2, c3_1));
        assertEquals(rev4.getComponentList(), Arrays.asList(c3_1, c3_1));
    }
}