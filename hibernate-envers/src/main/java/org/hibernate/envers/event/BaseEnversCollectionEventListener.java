/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.event;

import java.io.Serializable;
import java.util.List;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntityConfiguration;
import org.hibernate.envers.entities.RelationDescription;
import org.hibernate.envers.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.entities.mapper.id.IdMapper;
import org.hibernate.envers.synchronization.AuditProcess;
import org.hibernate.envers.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.synchronization.work.CollectionChangeWorkUnit;
import org.hibernate.envers.synchronization.work.FakeBidirectionalRelationWorkUnit;
import org.hibernate.envers.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.persister.collection.AbstractCollectionPersister;

/**
 * Base class for Envers' collection event related listeners
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern�n Chanfreau
 * @author Steve Ebersole
 */
public abstract class BaseEnversCollectionEventListener extends BaseEnversEventListener {
	protected BaseEnversCollectionEventListener(AuditConfiguration enversConfiguration) {
		super( enversConfiguration );
	}

    protected final CollectionEntry getCollectionEntry(AbstractCollectionEvent event) {
        return event.getSession().getPersistenceContext().getCollectionEntry(event.getCollection());
    }

    protected final void onCollectionAction(
			AbstractCollectionEvent event,
			PersistentCollection newColl,
			Serializable oldColl,
			CollectionEntry collectionEntry) {
        String entityName = event.getAffectedOwnerEntityName();
        if ( ! getAuditConfiguration().getGlobalCfg().isGenerateRevisionsForCollections() ) {
            return;
        }
        if ( getAuditConfiguration().getEntCfg().isVersioned( entityName ) ) {
            AuditProcess auditProcess = getAuditConfiguration().getSyncManager().get(event.getSession());

            String ownerEntityName = ((AbstractCollectionPersister) collectionEntry.getLoadedPersister()).getOwnerEntityName();
            String referencingPropertyName = collectionEntry.getRole().substring(ownerEntityName.length() + 1);

            // Checking if this is not a "fake" many-to-one bidirectional relation. The relation description may be
            // null in case of collections of non-entities.
            RelationDescription rd = searchForRelationDescription( entityName, referencingPropertyName );
            if ( rd != null && rd.getMappedByPropertyName() != null ) {
                generateFakeBidirecationalRelationWorkUnits(
						auditProcess,
						newColl,
						oldColl,
						entityName,
                        referencingPropertyName,
						event,
						rd
				);
            }
			else {
                PersistentCollectionChangeWorkUnit workUnit = new PersistentCollectionChangeWorkUnit(
						event.getSession(),
						entityName,
						getAuditConfiguration(),
						newColl,
						collectionEntry,
						oldColl,
						event.getAffectedOwnerIdOrNull(),
						referencingPropertyName
				);
				auditProcess.addWorkUnit( workUnit );

                if (workUnit.containsWork()) {
                    // There are some changes: a revision needs also be generated for the collection owner
                    auditProcess.addWorkUnit(
							new CollectionChangeWorkUnit(
									event.getSession(),
									event.getAffectedOwnerEntityName(),
									getAuditConfiguration(),
									event.getAffectedOwnerIdOrNull(),
									event.getAffectedOwnerOrNull()
							)
					);

                    generateBidirectionalCollectionChangeWorkUnits( auditProcess, event, workUnit, rd );
                }
            }
        }
    }

    /**
     * Looks up a relation description corresponding to the given property in the given entity. If no description is
     * found in the given entity, the parent entity is checked (so that inherited relations work).
	 *
     * @param entityName Name of the entity, in which to start looking.
     * @param referencingPropertyName The name of the property.
	 * 
     * @return A found relation description corresponding to the given entity or {@code null}, if no description can
     * be found.
     */
    private RelationDescription searchForRelationDescription(String entityName, String referencingPropertyName) {
        EntityConfiguration configuration = getAuditConfiguration().getEntCfg().get( entityName );
        RelationDescription rd = configuration.getRelationDescription(referencingPropertyName);
        if ( rd == null && configuration.getParentEntityName() != null ) {
            return searchForRelationDescription( configuration.getParentEntityName(), referencingPropertyName );
        }

        return rd;
    }

    private void generateFakeBidirecationalRelationWorkUnits(
			AuditProcess auditProcess,
			PersistentCollection newColl,
			Serializable oldColl,
			String collectionEntityName,
			String referencingPropertyName,
			AbstractCollectionEvent event,
			RelationDescription rd) {
        // First computing the relation changes
        List<PersistentCollectionChangeData> collectionChanges = getAuditConfiguration()
				.getEntCfg()
				.get( collectionEntityName )
				.getPropertyMapper()
                .mapCollectionChanges(event.getSession(), referencingPropertyName, newColl, oldColl, event.getAffectedOwnerIdOrNull() );

        // Getting the id mapper for the related entity, as the work units generated will corrspond to the related
        // entities.
        String relatedEntityName = rd.getToEntityName();
        IdMapper relatedIdMapper = getAuditConfiguration().getEntCfg().get(relatedEntityName).getIdMapper();

        // For each collection change, generating the bidirectional work unit.
        for ( PersistentCollectionChangeData changeData : collectionChanges ) {
            Object relatedObj = changeData.getChangedElement();
            Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity(relatedObj);
            RevisionType revType = (RevisionType) changeData.getData().get(
					getAuditConfiguration().getAuditEntCfg().getRevisionTypePropName()
			);

            // This can be different from relatedEntityName, in case of inheritance (the real entity may be a subclass
            // of relatedEntityName).
            String realRelatedEntityName = event.getSession().bestGuessEntityName(relatedObj);

            // By default, the nested work unit is a collection change work unit.
            AuditWorkUnit nestedWorkUnit = new CollectionChangeWorkUnit(
					event.getSession(),
					realRelatedEntityName,
					getAuditConfiguration(),
                    relatedId,
					relatedObj
			);

            auditProcess.addWorkUnit(
					new FakeBidirectionalRelationWorkUnit(
							event.getSession(),
							realRelatedEntityName,
							getAuditConfiguration(),
							relatedId,
							referencingPropertyName,
							event.getAffectedOwnerOrNull(),
							rd,
							revType,
							changeData.getChangedElementIndex(),
							nestedWorkUnit
					)
			);
        }

        // We also have to generate a collection change work unit for the owning entity.
        auditProcess.addWorkUnit(
				new CollectionChangeWorkUnit(
						event.getSession(),
						collectionEntityName,
						getAuditConfiguration(),
						event.getAffectedOwnerIdOrNull(),
						event.getAffectedOwnerOrNull()
				)
		);
    }

    private void generateBidirectionalCollectionChangeWorkUnits(
			AuditProcess auditProcess,
			AbstractCollectionEvent event,
			PersistentCollectionChangeWorkUnit workUnit,
			RelationDescription rd) {
        // Checking if this is enabled in configuration ...
        if ( ! getAuditConfiguration().getGlobalCfg().isGenerateRevisionsForCollections() ) {
            return;
        }

        // Checking if this is not a bidirectional relation - then, a revision needs also be generated for
        // the other side of the relation.
        // relDesc can be null if this is a collection of simple values (not a relation).
        if ( rd != null && rd.isBidirectional() ) {
            String relatedEntityName = rd.getToEntityName();
            IdMapper relatedIdMapper = getAuditConfiguration().getEntCfg().get( relatedEntityName ).getIdMapper();

            for ( PersistentCollectionChangeData changeData : workUnit.getCollectionChanges() ) {
                Object relatedObj = changeData.getChangedElement();
                Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity( relatedObj );

                auditProcess.addWorkUnit(
						new CollectionChangeWorkUnit(
								event.getSession(),
								event.getSession().bestGuessEntityName(relatedObj),
								getAuditConfiguration(),
								relatedId,
								relatedObj
						)
				);
            }
        }
    }
}
