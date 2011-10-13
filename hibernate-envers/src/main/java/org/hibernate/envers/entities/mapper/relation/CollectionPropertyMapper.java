package org.hibernate.envers.entities.mapper.relation;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.strategy.ValidityAuditStrategy;

/**
 * An extension to the {@link PropertyMapper} interface
 * used for controlling the revision query creation
 * used by the {@link ValidityAuditStrategy}.
 *  
 * @author Kristoffer Lundberg
 * 
 * @see ValidityAuditStrategy
 */
public interface CollectionPropertyMapper extends PropertyMapper {
    /**
     * Checks if the query needs to compare data
     * outside of the primary key.
     * 
     * This is only the case for {@link ElementCollection}s
     * of {@link Embeddable}s.
     */
    boolean needsDataComparision();
}
