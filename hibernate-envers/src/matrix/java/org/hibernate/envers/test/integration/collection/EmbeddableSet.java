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
import java.util.HashSet;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.collection.EmbeddableSetEntity;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.entities.components.Component4;
import org.junit.Test;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
public class EmbeddableSet extends AbstractEntityTest {
    private Integer ecse1_id;
    private final Component4 c4_1 = new Component4("c41", "c41_value", "c41_description");
    private final Component4 c4_2 = new Component4("c42", "c42_value2", "c42_description");
    private final Component3 c3_1 = new Component3("c31", c4_1, c4_2);
    private final Component3 c3_2 = new Component3("c32", c4_1, c4_2);

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbeddableSetEntity.class);
    }

    @Test
    public void initData() {
        EntityManager em = getEntityManager();

        EmbeddableSetEntity ecse1 = new EmbeddableSetEntity();

        // Revision 1 (ecse1: initially 1 element in both collections)
        em.getTransaction().begin();

        ecse1.getComponentSet().add(c3_1);

        em.persist(ecse1);

        em.getTransaction().commit();

        // Revision (still 1) (ecse1: removing non-existing element)
        em.getTransaction().begin();

        ecse1 = em.find(EmbeddableSetEntity.class, ecse1.getId());

        ecse1.getComponentSet().remove(c3_2);

        em.getTransaction().commit();

        // Revision 2 (ecse1: adding one element)
        em.getTransaction().begin();

        ecse1 = em.find(EmbeddableSetEntity.class, ecse1.getId());

        ecse1.getComponentSet().add(c3_2);

        em.getTransaction().commit();

        // Revision 3 (ecse1: adding one existing element)
        em.getTransaction().begin();

        ecse1 = em.find(EmbeddableSetEntity.class, ecse1.getId());

        ecse1.getComponentSet().add(c3_1);

        em.getTransaction().commit();

        // Revision 4 (ecse1: removing one existing element)
        em.getTransaction().begin();

        ecse1 = em.find(EmbeddableSetEntity.class, ecse1.getId());

        ecse1.getComponentSet().remove(c3_2);

        em.getTransaction().commit();

        ecse1_id = ecse1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(getAuditReader().getRevisions(EmbeddableSetEntity.class, ecse1_id), Arrays.asList(1, 2, 3));
    }

    @Test
    public void testHistoryOfEcse1() {
        EmbeddableSetEntity rev1 = getAuditReader().find(EmbeddableSetEntity.class, ecse1_id, 1);
        EmbeddableSetEntity rev2 = getAuditReader().find(EmbeddableSetEntity.class, ecse1_id, 2);
        EmbeddableSetEntity rev3 = getAuditReader().find(EmbeddableSetEntity.class, ecse1_id, 3);

        assertEquals(rev1.getComponentSet(), Collections.singleton(c3_1));
        assertEquals(rev2.getComponentSet(), new HashSet<Component3>(Arrays.asList(c3_1, c3_2)));
        assertEquals(rev3.getComponentSet(), new HashSet<Component3>(Arrays.asList(c3_1)));
    }
}