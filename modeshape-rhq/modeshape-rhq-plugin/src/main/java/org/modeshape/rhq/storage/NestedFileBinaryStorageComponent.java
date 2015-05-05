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

import org.modeshape.rhq.PluginI18n;
import org.rhq.modules.plugins.jbossas7.json.Address;

/**
 * The ModeShape RHQ AS 7 nested file binary storage component.
 */
public final class NestedFileBinaryStorageComponent extends ModeShapeStorageComponent {

    /**
     * The nested file binary storage component type.
     */
    public static final String TYPE = "nested-storage-type-file";

    /**
     * Constructs a nested file binary storage component.
     */
    public NestedFileBinaryStorageComponent() {
        super(TYPE, PluginI18n.fileBinaryStorageDisplayName, PluginI18n.fileBinaryStorageDescription, StorageType.BINARY);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.storage.ModeShapeStorageComponent#address()
     */
    @Override
    protected Address address() {
        final CompositeBinaryStorageComponent composite = (CompositeBinaryStorageComponent)context().getParentResourceComponent();
        final Address addr = composite.createBinaryStorageAddress();
        addr.add(NestedFileBinaryStorageComponent.TYPE, deploymentName());

        return addr;
    }

}
