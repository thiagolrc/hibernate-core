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
package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.entities.mapper.relation.lazy.initializor.BasicCollectionInitializor;
import org.hibernate.envers.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.reader.AuditReaderImplementor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableBiMap;

/**
 * 
 * @author thiagolrc
 *
 */
public final class IdBagCollectionMapper extends AbstractCollectionMapper<List>
		implements PropertyMapper {
	private final MiddleComponentData elementComponentData;
	private final String idColumnName;

	public IdBagCollectionMapper(
			String idColumnName,
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends List> collectionClass,
			Class<? extends List> proxyClass,
			MiddleComponentData elementComponentData, boolean ordinalInId,
			boolean revisionTypeInId) {
		super(commonCollectionMapperData, collectionClass, proxyClass,
				ordinalInId, revisionTypeInId);
		this.elementComponentData = elementComponentData;
		this.idColumnName = idColumnName;
	}

	protected Initializor<List> getInitializor(AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader, Object primaryKey,
			Number revision) {
		return new BasicCollectionInitializor<List>(verCfg, versionsReader,
				commonCollectionMapperData.getQueryGenerator(), primaryKey,
				revision, collectionClass, elementComponentData);
	}

	protected Collection getNewCollectionContent(
			PersistentCollection newCollection) {
		return (Collection) newCollection;
	}

	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if (oldCollection == null) {
			return null;
		} else {
			return ((HashMap)oldCollection).values();
		} 
	}
	
	protected void mapToMapFromObject(SessionImplementor session,
			Map<String, Object> idData, Map<String, Object> data, Object changed) {
		elementComponentData.getComponentMapper().mapToMapFromObject(session,
				idData, data, changed);
	}

	public boolean needsDataComparision() {
		return elementComponentData.getComponentMapper().needsDataComparision();
	}
	
	@Override
    protected Map<String, Object> createIdMap(int ordinal) {
          final HashMap<String, Object> idMap = new HashMap<String, Object>();
          return idMap;
    }


	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(SessionImplementor session, 
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
		final List<PersistentCollectionChangeData> collectionChanges = super.mapCollectionChanges(session, referencingPropertyName, newColl, oldColl, id);
		
		//now we map the identifiers into the audited data
		
		//PS:
		//a) the oldColl is a map<key,Object> and it can have different keys which map
		//to same objects like: <1,Object1>, <2,Object1> 
		//b) the keys are the collection's identifiers.
		//c) as the auditing of component collections always deletes the whole collection and then recreates it, 
		//the exactly id which maps to the object doesn't really matter, 
		//so if we have duplicate elements we're not going to keep the exact mapping
		
		ArrayListMultimap<Object, Object> componentKeysMap = ArrayListMultimap.create();
		if (oldColl != null) {
			HashMap<Object, Object> oc = (HashMap<Object, Object>)oldColl;
			for(Object key : oc.keySet()) {
				componentKeysMap.put(oc.get(key), key);
			}
		}
		
		for (int i=0; i<collectionChanges.size();i++){
			PersistentCollectionChangeData ccd = collectionChanges.get(i);
			HashMap<String, Object> originalIdMap = (HashMap<String, Object>)ccd.getData().get(this.commonCollectionMapperData.getVerEntCfg().getOriginalIdPropName());
			if (originalIdMap.get(this.commonCollectionMapperData.getVerEntCfg().getRevisionTypePropName()).equals(RevisionType.ADD)){
				originalIdMap.put(this.idColumnName, ((org.hibernate.collection.PersistentIdentifierBag)newColl).getIdentifier(((List)newColl).get(i),i));
			}
			if (originalIdMap.get(this.commonCollectionMapperData.getVerEntCfg().getRevisionTypePropName()).equals(RevisionType.DEL)){
				//the same component could appear several times with different keys, let's just use any of the keys at a time... 
				originalIdMap.put(this.idColumnName, componentKeysMap.get(ccd.getChangedElement()).remove(0));
			}
		}
		

		return collectionChanges;
	}
}