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

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.annotations.CollectionId;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity3;
import org.hibernate.envers.test.entities.components.Component4;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks if embedded component lists mapped with a {@link CollectionId} are being audited
 * @author thiagolrc
 *
 */
public class EmbeddableList3 extends AbstractEntityTest {
	private Integer ecle1_id;
    private final Component4 c4 = new Component4("c4", "c4_value", "c4_description");
    
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbeddableListEntity3.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        EmbeddableListEntity3 ecle1 = new EmbeddableListEntity3();

        // Revision 1 (ecle1: initially 1 element in the collections)
        em.getTransaction().begin();
        
        ecle1.getComponentList().add(c4);

        em.persist(ecle1);

        em.getTransaction().commit();

    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(getAuditReader().getRevisions(EmbeddableListEntity3.class, ecle1_id), Arrays.asList(1));
    }

    @Test
    public void testHistoryOfEcle1() {
        EmbeddableListEntity3 rev1 = getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, 1);
        
        assertEquals(rev1.getComponentList(), Collections.singletonList(c4));
    }
}