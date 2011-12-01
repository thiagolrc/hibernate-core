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
import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.collection.EmbeddableListEntity2;
import org.hibernate.envers.test.entities.components.relations.ManyToOneComponent;
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
    private ManyToOneComponent manyToOneComponent1 = new ManyToOneComponent(entity1,"dataComponent1");
    private ManyToOneComponent manyToOneComponent2 = new ManyToOneComponent(entity2,"dataComponent2");
    
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
        em.persist(ecle1);
        
        em.getTransaction().commit();
        ecl1 = ecle1.getId();

        // Revision 2 (ecle1: changing the component)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());

        ecle1.getComponentList().clear();
        ecle1.getComponentList().add(manyToOneComponent2);
        
        em.getTransaction().commit();
        
        //Revision 3 (ecle1: putting back the manyToOneComponent1 to the list)
        em.getTransaction().begin();
        ecle1 = em.find(EmbeddableListEntity2.class, ecle1.getId());

        ecle1.getComponentList().add(manyToOneComponent1);
        
        em.getTransaction().commit();
        
        
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(getAuditReader().getRevisions(EmbeddableListEntity2.class, ecl1), Arrays.asList(1,2,3));
    }

    @Test
    public void testManyToOneComponentList(){
    	//Rev1: manyToOneComponent1 in the list 
    	EmbeddableListEntity2 rev1 = getAuditReader().find(EmbeddableListEntity2.class, ecl1, 1);
    	assertNotNull(rev1,"Revision not found");
    	assertTrue(rev1.getComponentList().size()>0,"The component collection was not audited");
    	assertEquals(rev1.getComponentList().get(0).getData(),manyToOneComponent1.getData(),"The component primitive property was not audited");
    	assertEquals(rev1.getComponentList().get(0).getEntity(),entity1,"The component manyToOne reference was not audited");
    }
    
    @Test
    public void testHistoryOfEcle1() {
    	//Rev1: manyToOneComponent1 in the list 
    	EmbeddableListEntity2 rev1 = getAuditReader().find(EmbeddableListEntity2.class, ecl1, 1);
    	assertEquals(rev1.getComponentList(),Collections.singletonList(manyToOneComponent1));
    	
    	//Rev2: manyToOneComponent2 in the list
    	EmbeddableListEntity2 rev2 = getAuditReader().find(EmbeddableListEntity2.class, ecl1, 2);
    	assertEquals(rev2.getComponentList(),Collections.singletonList(manyToOneComponent2));
        
    	//Rev3: manyToOneComponent2 and manyToOneComponent1 in the list in the list
    	EmbeddableListEntity2 rev3 = getAuditReader().find(EmbeddableListEntity2.class, ecl1, 3);
    	assertEquals(Arrays.asList(manyToOneComponent2, manyToOneComponent1),rev3.getComponentList());
    	
    }
}