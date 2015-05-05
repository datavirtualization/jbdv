package org.modeshape.rhq.storage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.modeshape.rhq.ModeShapeComponent;
import org.modeshape.rhq.ModeShapePlugin;
import org.modeshape.rhq.Operation;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.RepositoryComponent;
import org.modeshape.rhq.storage.ModeShapeStorageComponent.StorageType;
import org.modeshape.rhq.util.I18n;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

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

/**
 * Used to discover the ModeShape repository storage components.
 * 
 * @param <T> the storage type
 */
public abstract class ModeShapeStorageDiscoveryComponent<T> implements ResourceDiscoveryComponent<ModeShapeStorageComponent> {

    private final String componentType;

    private final StorageType storageType;

    protected ModeShapeStorageDiscoveryComponent( final String componentType,
                                                  final StorageType storageType ) {
        this.componentType = componentType;
        this.storageType = storageType;
    }

    protected Address address( final ModeShapeComponent parent ) {
        if (!(parent instanceof RepositoryComponent)) {
            throw new InvalidPluginConfigurationException(I18n.bind(PluginI18n.unexpectedParentComponent,
                                                                    parent.getClass().getName(),
                                                                    getClass().getSimpleName()));
        }

        final RepositoryComponent repository = (RepositoryComponent)parent;
        final Address addr = ((this.storageType == StorageType.INDEX) ? repository.createIndexStorageAddress() : repository.createBinaryStorageAddress());
        addr.add(RepositoryComponent.STORAGE_TYPE, this.componentType);

        return addr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources( final ResourceDiscoveryContext<ModeShapeStorageComponent> context )
        throws Exception {
        final ModeShapeComponent parent = context.getParentResourceComponent();
        final Address addr = address(parent);
        final ASConnection connection = parent.getASConnection();
        final org.rhq.modules.plugins.jbossas7.json.Operation op = operation(addr);

        if (op == null) {
            return Collections.emptySet();
        }

        final Result result = connection.execute(op);

        if (result.isSuccess()) {
            final Object temp = result.getResult();

            if ((temp != null) && (temp instanceof Map<?, ?>)) {
                final Map<?, ?> storageMap = (Map<?, ?>)temp;

                if (!storageMap.isEmpty()) {
                    // // context.getDefaultPluginConfiguration() returns a new config which is needed
                    final Configuration config = context.getDefaultPluginConfiguration(); // needs to stay inside loop
                    final DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                                                                                           context.getResourceType(),
                                                                                           this.componentType,
                                                                                           this.componentType,
                                                                                           ModeShapePlugin.VERSION,
                                                                                           context.getResourceType().getDescription(),
                                                                                           config, null);
                    ModeShapePlugin.LOG.debug("Discovered ModeShape storage configuration resource of type '"
                                              + this.componentType + "'in repository '" + repositoryName(parent) + '\'');
                    return Collections.singleton(detail);
                }
            }
        }

        return Collections.emptySet();
    }

    /**
     * @param address the address at which the operation will be run (cannot be <code>null</code>)
     * @return the operation to execute or <code>null</code> if nothing to run
     */
    protected org.rhq.modules.plugins.jbossas7.json.Operation operation( final Address address ) {
        return Operation.Util.createReadResourceOperation(address, false);
    }

    protected String repositoryName( final ModeShapeComponent parent ) throws Exception {
        if (parent instanceof RepositoryComponent) {
            return parent.deploymentName();
        }

        // this case is for composite binary storages
        final ModeShapeComponent newParent = parent.context().getParentResourceComponent();

        if (newParent instanceof RepositoryComponent) {
            return newParent.deploymentName();
        }

        throw new InvalidPluginConfigurationException(I18n.bind(PluginI18n.unexpectedParentComponent,
                                                                parent.getClass().getName(),
                                                                getClass().getSimpleName()));
    }

    /**
     * @return the component type (never <code>null</code>)
     */
    protected String type() {
        return this.componentType;
    }

}
