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
package org.modeshape.rhq.storage;

import java.util.Collection;
import org.modeshape.rhq.Operation;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.RepositoryComponent;
import org.modeshape.rhq.util.I18n;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.modules.plugins.jbossas7.json.Address;

/**
 * The ModeShape RHQ AS 7 composite binary storage component.
 */
public final class CompositeBinaryStorageComponent extends ModeShapeStorageComponent {

    /**
     * The composite binary storage component type.
     */
    public static final String TYPE = "composite-binary-storage";

    /**
     * @return an operation that can be used to obtain repository nested cache binary storage child components (never
     *         <code>null</code>)
     */
    public static Operation createGetNestedCacheBinaryStorages() {
        return new Operation("read-children-resources", "child-type", NestedCacheBinaryStorageComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository nested custom binary storage child components (never
     *         <code>null</code>)
     */
    public static Operation createGetNestedCustomBinaryStorages() {
        return new Operation("read-children-resources", "child-type", NestedCustomBinaryStorageComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository nested database binary storage child components (never
     *         <code>null</code>)
     */
    public static Operation createGetNestedDbBinaryStorages() {
        return new Operation("read-children-resources", "child-type", NestedDbBinaryStorageComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository nested file binary storage child components (never
     *         <code>null</code>)
     */
    public static Operation createGetNestedFileBinaryStorages() {
        return new Operation("read-children-resources", "child-type", NestedFileBinaryStorageComponent.TYPE);
    }

    /**
     * Constructs a composite binary storage component.
     */
    public CompositeBinaryStorageComponent() {
        super(TYPE, PluginI18n.compositeBinaryStorageDisplayName, PluginI18n.compositeBinaryStorageDescription,
              StorageType.BINARY);
    }

    /**
     * @return the requested binary storage address (never <code>null</code>)
     */
    public Address createBinaryStorageAddress() {
        final Address addr = repository().createBinaryStorageAddress();
        addr.add(RepositoryComponent.STORAGE_TYPE, CompositeBinaryStorageComponent.TYPE);
        return addr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.util.Collection)
     */
    @Override
    protected void loadProperty( final String name,
                                 final Collection<?> valueList ) throws Exception {
        if (RhqId.NESTED_STORES.equals(name)) {
            final PropertyList values = new PropertyList(RhqId.NESTED_STORES);

            for (final Object obj : valueList) {
                final Property prop = new PropertySimple(RhqId.STORE_NAME, obj);
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
        if (RhqId.NESTED_STORES.equals(name)) {
            final PropertyList values = new PropertyList(RhqId.NESTED_STORES); // no values so empty list
            resourceConfiguration().put(values);
        } else {
            super.loadProperty(name, value);
        }
    }

    /**
     * Identifiers that match the <code>rhq-plugin.xml</code> identifiers and pertain to list and map properties.
     */
    private interface RhqId {
        String NESTED_STORES = "nested-stores";
        String STORE_NAME = "store-name";
    }

}
