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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityManager;

import org.hibernate.annotations.CollectionId;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity3;
import org.hibernate.envers.test.entities.components.Component4;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks if embedded component lists mapped with a {@link CollectionId} are being audited
 * @author thiagolrc
 *
 */
public class EmbeddableList3 extends AbstractEntityTest {
	private Integer ecle0_id;
	private Integer ecle1_id;
	
	private final Component4 c40 = new Component4("c40", "c40_value", null);//description is not audited
    private final Component4 c41 = new Component4("c41", "c41_value", null);//description is not audited
    private final Component4 c42 = new Component4("c42", "c42_value", null);//description is not audited
    private final Component4 c43 = new Component4("c43", "c43_value", null);//description is not audited
    private final Component4 c44 = new Component4("c44", "c44_value", null);//description is not audited
    
    private Number revision1;
    private Number revision2;
    private Number revision3;
    private Number revision4;
    private Number revision5;
    private Number revision6;
    private Number revision7;
    private Number revision8;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbeddableListEntity3.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        EmbeddableListEntity3 ecle0 = new EmbeddableListEntity3();

        // Revision 1 (ecle0: initially 2 identical elements in the collections)
        em.getTransaction().begin();
        
        ecle0.getComponentList().add(c40);
        ecle0.getComponentList().add(c40);

        em.persist(ecle0);
        
        em.getTransaction().commit();
        revision1 = getCurrentDateRevision();
        
        ecle0_id = ecle0.getId();
        
        //Revision 1_1 (ecle0: adding another c40)
        em.getTransaction().begin();
        
        ecle0.getComponentList().add(c40);
        
        em.persist(ecle0);
        
        em.getTransaction().commit();
        revision1 = getCurrentDateRevision();
        
        ecle0_id = ecle0.getId();

        EmbeddableListEntity3 ecle1 = new EmbeddableListEntity3();

        // Revision 2 (ecle1: initially 2 elements in the collections)
        em.getTransaction().begin();
        
        ecle1.getComponentList().add(c41);
        ecle1.getComponentList().add(c42);

        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision2 = getCurrentDateRevision();

        ecle1_id = ecle1.getId();

        // Revision 3 (ecle1: adding another element)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);

        ecle1.getComponentList().add(c43);
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision3 = getCurrentDateRevision();

        // Revision 4 (ecle1: changing some elements)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);
        
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(c41)).setValue("c41_val");
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(c42)).setValue("c42_val");
        
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision4 = getCurrentDateRevision();

        // Revision 5 (ecle1: removing an element)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);
        
        ecle1.getComponentList().remove(c43);
        
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision5 = getCurrentDateRevision();

        // Revision 6 (ecle1: adding, editing and removing elements)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);
        
        ecle1.getComponentList().add(c44);
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(c41)).setValue("c41_new_val");
        ecle1.getComponentList().remove(c42);
        
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision6 = getCurrentDateRevision();
        
        // Revision 7 (ecle1: removing all elements)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);
        
        ecle1.getComponentList().remove(c41);
        ecle1.getComponentList().remove(c44);
        
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision7 = getCurrentDateRevision();
        
        // Revision 8 (ecle1: removing all)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity3.class, ecle1_id);
        
        em.remove(ecle1);
        
        em.getTransaction().commit();
        revision8 = getCurrentDateRevision();
        
    }
    
	private Number getCurrentDateRevision() {
		return getAuditReader().getRevisionNumberForDate(new Date());
	}

    @Test
    public void testRevisionsCounts() {
    	assertEquals(getAuditReader().getRevisions(EmbeddableListEntity3.class, ecle0_id), Arrays.asList(revision1));
        assertEquals(getAuditReader().getRevisions(EmbeddableListEntity3.class, ecle1_id), Arrays.asList(revision2, revision3, revision4, revision5, revision6, revision7, revision8));
    }

    @Test
    public void testHistoryOfEcle() {
        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle0_id, revision1).getComponentList().containsAll( 
        		Arrays.asList(new Component4("c40", "c40_value", null), new Component4("c40", "c40_value", null))));
        
        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision2).getComponentList().containsAll(  
        		Arrays.asList(new Component4("c41", "c41_value", null), new Component4("c42", "c42_value", null))));

        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision3).getComponentList().containsAll(  
        		Arrays.asList(new Component4("c41", "c41_value", null), new Component4("c42", "c42_value", null),
        				new Component4("c43", "c43_value", null))));

        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision4).getComponentList().containsAll(  
        		Arrays.asList(new Component4("c41", "c41_val", null),new Component4("c42", "c42_val", null),
        				new Component4("c43", "c43_value", null))));

        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision5).getComponentList().containsAll(  
        		Arrays.asList(new Component4("c41", "c41_val", null), new Component4("c42", "c42_val", null))));

        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision6).getComponentList().containsAll(  
        		Arrays.asList(new Component4("c41", "c41_new_val", null), new Component4("c44", "c44_value", null))));

        Assert.assertTrue(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision7).getComponentList().containsAll(  
        		new ArrayList<Component4>()));

        Assert.assertNull(getAuditReader().find(EmbeddableListEntity3.class, ecle1_id, revision8));
        
    }
}