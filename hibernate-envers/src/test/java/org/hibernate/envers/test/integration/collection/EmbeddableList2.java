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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity2;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Checks if manyToOne relations inside a embedded component list are being audited 
 * @author thiagolrc
 *
 */
public class EmbeddableList2 extends AbstractEntityTest {
    private Integer ecl1;
    
    private StrTestEntity entity1 = new StrTestEntity("strTestEntity1");
    private StrTestEntity entity2 = new StrTestEntity("strTestEntity2");
    private StrTestEntity entity3 = new StrTestEntity("strTestEntity3");
    private StrTestEntity entity4 = new StrTestEntity("strTestEntity3");
    private StrTestEntity entity4Copy;

    private ManyToOneComponent manyToOneComponent1 = new ManyToOneComponent(entity1,"dataComponent1");
    private ManyToOneComponent manyToOneComponent2 = new ManyToOneComponent(entity2,"dataComponent2");
    private ManyToOneComponent manyToOneComponent4 = new ManyToOneComponent(entity4,"dataComponent4");
    
    private Number revision1;
    private Number revision2;
    private Number revision3;
    private Number revision4;
    private Number revision5;
    private Number revision6;
    private Number revision7;
    private Number revision8;
       
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbeddableListEntity2.class);
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        EmbeddableListEntity2 ecle1 = new EmbeddableListEntity2();

        // Revision 1 (ecl1: saving a list with 1 manyToOneComponent)
        em.getTransaction().begin();
        
        em.persist(entity1);//persisting the entities referenced by the components
        em.persist(entity2);
        
        ecle1.getComponentList().add(manyToOneComponent1);
        //ecle1.getComponentList().add(new ManyToOneComponent(entity1,"dataComponent1"));
        em.persist(ecle1);
        
        em.getTransaction().commit();
        revision1 = getCurrentDateRevision();
        
        ecl1 = ecle1.getId();

        // Revision 2 (ecle1: changing the component)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());

        ecle1.getComponentList().clear();
        ecle1.getComponentList().add(manyToOneComponent2);
        
        em.getTransaction().commit();
        revision2 = getCurrentDateRevision();
        
        //Revision 3 (ecle1: putting back the manyToOneComponent1 to the list)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());

        ecle1.getComponentList().add(manyToOneComponent1);
        
        em.getTransaction().commit();
        revision3 = getCurrentDateRevision();
        

        // Revision 4 (ecle1: changing the component's entity)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());
      
        em.persist(entity3);
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(manyToOneComponent2)).setEntity(entity3);
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(manyToOneComponent2)).setData("dataComponent3");
        
        em.getTransaction().commit();
        revision4 = getCurrentDateRevision();
        
        // Revision 5 (ecle1: adding a new manyToOneComponent)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());
      
        em.persist(entity4);
        entity4Copy = new StrTestEntity(entity4.getStr(), entity4.getId());

        ecle1.getComponentList().add(manyToOneComponent4);
        
        em.getTransaction().commit();
        revision5 = getCurrentDateRevision();

        // Revision 6 (ecle1: changing the component's entity properties)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());
      
        ecle1.getComponentList().get(ecle1.getComponentList().indexOf(manyToOneComponent4)).getEntity().setStr("sat4");
        
        em.getTransaction().commit();
        revision6 = getCurrentDateRevision();
        
        // Revision 7 (ecle1: removing component)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());
      
        ecle1.getComponentList().remove(ecle1.getComponentList().indexOf(manyToOneComponent4));
        
        em.getTransaction().commit();
        revision7 = getCurrentDateRevision();
        
        // Revision 8 (ecle1: removing all)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());
      
        em.remove(ecle1);
        em.getTransaction().commit();
        
        revision8 = getCurrentDateRevision();
        
    }

	private Number getCurrentDateRevision() {
		return getAuditReader().getRevisionNumberForDate(new Date());
	}

    @Test
    public void testRevisionsCounts() {
        assertEquals(getAuditReader().getRevisions(EmbeddableListEntity2.class, ecl1), Arrays.asList(revision1, revision2, revision3, revision4, revision5, revision7, revision8));
        assertEquals(getAuditReader().getRevisions(StrTestEntity.class, entity1.getId()), Arrays.asList(revision1));
        assertEquals(getAuditReader().getRevisions(StrTestEntity.class, entity2.getId()), Arrays.asList(revision1));
        assertEquals(getAuditReader().getRevisions(StrTestEntity.class, entity3.getId()), Arrays.asList(revision4));
        assertEquals(getAuditReader().getRevisions(StrTestEntity.class, entity4.getId()), Arrays.asList(revision5, revision6));
    }

    @Test
    public void testManyToOneComponentList(){
    	//Rev1: manyToOneComponent1 in the list 
    	EmbeddableListEntity2 rev1 = getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision1);
    	assertNotNull(rev1, "Revision not found");
    	assertTrue(rev1.getComponentList().size() > 0, "The component collection was not audited");
    	assertEquals(rev1.getComponentList().get(0).getData(), "dataComponent1", "The component primitive property was not audited");
    	assertEquals(rev1.getComponentList().get(0).getEntity(), entity1, "The component manyToOne reference was not audited");
    }
    
    @Test
    public void testHistoryOfEcle1() {
    	//Rev1: manyToOneComponent1 in the list 
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision1).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity1, "dataComponent1")))));
    	
    	//Rev2: manyToOneComponent2 in the list
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision2).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity2, "dataComponent2")))));
        
    	//Rev3: manyToOneComponent2 and manyToOneComponent1 in the list
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision3).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity2, "dataComponent2"), new ManyToOneComponent(entity1, "dataComponent1")))));

    	//Rev4: manyToOneComponent2 edited and manyToOneComponent1 in the list
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision4).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity3, "dataComponent3"), new ManyToOneComponent(entity1, "dataComponent1")))));

    	//Rev5: manyToOneComponent4 added in the list
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision5).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity3, "dataComponent3"), new ManyToOneComponent(entity1, "dataComponent1"), new ManyToOneComponent(entity4Copy, "dataComponent4")))));

    	//Rev6: changing the manyToOneComponent4's entity4 property
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision6).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity3, "dataComponent3"), new ManyToOneComponent(entity1, "dataComponent1"), new ManyToOneComponent(entity4, "dataComponent4")))));

    	//Rev6: removing manyToOneComponent4
    	 Assert.assertTrue((getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision7).getComponentList().containsAll(
    			Arrays.asList(new ManyToOneComponent(entity3, "dataComponent3"), new ManyToOneComponent(entity1, "dataComponent1")))));
    	
        Assert.assertNull(getAuditReader().find(EmbeddableListEntity2.class, ecl1, revision8));

    }
}