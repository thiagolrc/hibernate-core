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
package org.hibernate.envers.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.entities.mapper.CompositeMapperBuilder;
import org.hibernate.envers.entities.mapper.MultiPropertyMapper;
import org.hibernate.envers.entities.mapper.PropertyMapper;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.tools.query.Parameters;
import org.hibernate.util.ReflectHelper;

/**
 * A {@link MiddleComponentMapper} implementation for embeddables / components,
 * assembling and disassembling the component via a {@link MultiPropertyMapper}.
 * 
 * <p>
 *  All methods except {@link #addMiddleEqualToQuery(Parameters, String, String, String, String)} delegate to a {@link MultiPropertyMapper}.
 * </p>
 * 
 * <p>
 *  {@link #addMiddleEqualToQuery(Parameters, String, String, String, String)} adds all properties
 *  to the where clause.
 * </p>
 * 
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
public final class MiddleEmbeddableComponentMapper implements MiddleComponentMapper, CompositeMapperBuilder {
    private final MultiPropertyMapper delegate;
    private final String componentClassName;

    public MiddleEmbeddableComponentMapper(final MultiPropertyMapper delegate, final String componentClassName) {
      this.delegate = delegate;
      this.componentClassName = componentClassName;
    }

    public void add(PropertyData propertyData) {
      delegate.add(propertyData);
    }

    public CompositeMapperBuilder addComponent(final PropertyData propertyData, final String componentClassName) {
      return delegate.addComponent(propertyData, componentClassName);
    }

    public void addComposite(final PropertyData propertyData, final PropertyMapper propertyMapper) {
      delegate.addComposite(propertyData, propertyMapper);
    }

    public Object mapToObjectFromFullMap(final EntityInstantiator entityInstantiator,
            final Map<String, Object> data, final Object dataObject, final Number revision) {
      try {
        final Object componentInstance = (dataObject != null ? dataObject : ReflectHelper.getDefaultConstructor(Thread.currentThread().getContextClassLoader().loadClass(componentClassName)).newInstance());
        this.delegate.mapToEntityFromMap(entityInstantiator.getAuditConfiguration(), componentInstance, data, null, entityInstantiator.getAuditReaderImplementor(), revision);
        return componentInstance;
      } catch(Exception e) {
        throw new AuditException(e);
      }
    }

    public void mapToMapFromObject(final SessionImplementor session, final Map<String, Object> idData, final Map<String, Object> data, final Object obj) {
      delegate.mapToMapFromEntity(session, data, obj, obj);
    }

    public void addMiddleEqualToQuery(final Parameters parameters, final String idPrefix1, final String prefix1, final String idPrefix2, final String prefix2) {
        addMiddleEqualToQuery(delegate, parameters, idPrefix1, prefix1, idPrefix2, prefix2);
    }

    protected void addMiddleEqualToQuery(final CompositeMapperBuilder compositeMapper, final Parameters parameters, final String idPrefix1, final String prefix1, final String idPrefix2, final String prefix2) {
        for(final Map.Entry<PropertyData, PropertyMapper> entry : compositeMapper.getProperties().entrySet()) {
          final String propertyName = entry.getKey().getName();
          final PropertyMapper nestedMapper = entry.getValue();
          
          if(nestedMapper instanceof CompositeMapperBuilder) {
              addMiddleEqualToQuery((CompositeMapperBuilder) nestedMapper, parameters, idPrefix1, prefix1, idPrefix2, prefix2);
          } else {
              parameters.addWhere(prefix1 + '.' + propertyName, false, "=", prefix2 + '.' + propertyName, false);
          }
        }
    }

    public boolean needsDataComparision() {
        return true;
    }

    public Map<PropertyData, PropertyMapper> getProperties() {
        return delegate.getProperties();
    }
}
