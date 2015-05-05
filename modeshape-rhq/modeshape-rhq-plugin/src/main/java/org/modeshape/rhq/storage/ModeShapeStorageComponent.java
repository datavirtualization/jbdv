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

import java.util.Map;
import org.modeshape.rhq.ModeShapeComponent;
import org.modeshape.rhq.ModeShapePlugin;
import org.modeshape.rhq.Operation;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.RepositoryComponent;
import org.modeshape.rhq.util.I18n;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ModeShape RHQ AS 7 resource component base class for binary and index storage.
 */
public abstract class ModeShapeStorageComponent extends ModeShapeComponent {

    private final StorageType storageType;

    protected ModeShapeStorageComponent( final String componentType,
                                         final String displayName,
                                         final String description,
                                         final StorageType storageType ) {
        super(componentType, displayName, description);
        this.storageType = storageType;
    }

    protected Address address() {
        final ModeShapeComponent repository = context().getParentResourceComponent();
        final Address addr = new Address(repository.getAddress());

        if (this.storageType == StorageType.BINARY) {
            addr.add(RepositoryComponent.CONFIGURATION_TYPE, RepositoryComponent.BINARY_STORAGE_TYPE);
        } else {
            addr.add(RepositoryComponent.CONFIGURATION_TYPE, RepositoryComponent.INDEX_STORAGE_TYPE);
        }

        addr.add(RepositoryComponent.STORAGE_TYPE, deploymentName());
        return addr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected final void load() throws Exception {
        final Address addr = address();
        final Result result = getASConnection().execute(Operation.Util.createReadResourceOperation(addr, true));
        boolean success = true;

        if (result.isSuccess()) {
            final Object tempResult = result.getResult();

            if ((tempResult != null) && (tempResult instanceof Map<?, ?>)) {
                final Map<?, ?> propMap = (Map<?, ?>)tempResult;
                ModeShapePlugin.LOG.debug("Loading " + propMap.size() + " properties for " + this.storageType + " storage '"
                                          + deploymentName() + '\'');
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
                                          repository().deploymentName()));
        }
    }

    /**
     * @return the repository component parent (never <code>null</code>)
     */
    public RepositoryComponent repository() {
        return (RepositoryComponent)context().getParentResourceComponent();
    }

    protected enum StorageType {
        BINARY,
        INDEX
    }

}
