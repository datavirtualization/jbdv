/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.rhq;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.modeshape.rhq.util.I18n;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ModeShape RHQ AS 7 external source (connector) component.
 */
public final class ConnectorComponent extends ModeShapeComponent {

    /**
     * The connector component type.
     */
    public static final String TYPE = "source";

    /**
     * Constructs a connector component.
     */
    public ConnectorComponent() {
        super(TYPE, PluginI18n.connectorDisplayName, PluginI18n.connectorDescription);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected void load() throws Exception {
        final ModeShapeComponent repository = context().getParentResourceComponent();
        final Address addr = new Address(repository.getAddress());
        addr.add(TYPE, deploymentName());

        final Result result = getASConnection().execute(Operation.Util.createReadResourceOperation(addr, true));
        boolean success = true;

        if (result.isSuccess()) {
            final Object tempResult = result.getResult();

            if ((tempResult != null) && (tempResult instanceof Map<?, ?>)) {
                final Map<?, ?> propMap = (Map<?, ?>)tempResult;
                ModeShapePlugin.LOG.debug("Loading " + propMap.size() + " properties for connector '" + deploymentName() + '\'');
                loadProperties(propMap);
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        if (!success) {
            throw new Exception(I18n.bind(PluginI18n.errorLoadingProperties,
                                          deploymentName(),
                                          getClass().getSimpleName(),
                                          repository.deploymentName()));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.util.Collection)
     */
    @Override
    protected void loadProperty( final String name,
                                 final Collection<?> valueList ) throws Exception {
        if (RhqId.PROPERTIES.equals(name)) {
            final PropertyList values = new PropertyList(name);

            for (final Object obj : valueList) {
                final PropertyMap map = new PropertyMap(RhqId.PROPERTY);
                final Map<?, ?> propMap = (Map<?, ?>)obj;
                final Entry<?, ?> prop = propMap.entrySet().iterator().next(); // only one entry per map
                map.put(new PropertySimple(RhqId.NAME, prop.getKey()));
                map.put(new PropertySimple(RhqId.VALUE, prop.getValue()));
                values.add(map);
            }

            resourceConfiguration().put(values);
        } else if (RhqId.PROJECTIONS.equals(name)) {
            final PropertyList values = new PropertyList(name);

            for (final Object obj : valueList) {
                final Property prop = new PropertySimple(RhqId.PROJECTION, obj);
                values.add(prop);
            }

            resourceConfiguration().put(values);
        } else {
            throw new Exception(I18n.bind(PluginI18n.unknownProperty, name, deploymentName(), type()));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.lang.String)
     */
    @Override
    protected void loadProperty( final String name,
                                 final String value ) throws Exception {
        // list/map properties without values must still be loaded as list/map
        if (RhqId.PROJECTIONS.equals(name)) {
            final PropertyList values = new PropertyList(RhqId.PROJECTION); // no values so empty list
            resourceConfiguration().put(values);
        } else if (RhqId.PROPERTIES.equals(name)) {
            final PropertyList values = new PropertyList(RhqId.PROPERTIES);
            final PropertyMap map = new PropertyMap(RhqId.PROPERTY); // no values so empty map
            values.add(map);
            resourceConfiguration().put(values);
        } else {
            super.loadProperty(name, value);
        }
    }

    /**
     * Identifiers that match the <code>rhq-plugin.xml</code> identifiers and pertain to list and map properties.
     */
    private interface RhqId {
        String NAME = "name";
        String PROJECTION = "projection";
        String PROJECTIONS = "projections";
        String PROPERTIES = "properties";
        String PROPERTY = "property";
        String VALUE = "value";
    }

}
