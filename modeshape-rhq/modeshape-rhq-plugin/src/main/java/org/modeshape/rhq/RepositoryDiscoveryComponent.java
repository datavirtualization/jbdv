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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Used to discover the ModeShape repository components.
 */
public class RepositoryDiscoveryComponent implements ResourceDiscoveryComponent<RepositoryComponent> {

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources( final ResourceDiscoveryContext<RepositoryComponent> context )
        throws Exception {
        final BaseComponent<?> parentComponent = context.getParentResourceComponent();
        final ASConnection connection = parentComponent.getASConnection();
        final Address addr = ModeShapePlugin.createModeShapeAddress();
        final Result result = connection.execute(Operation.Util.createReadResourceOperation(addr, true));

        if (result.isSuccess()) {
            final Result repoResult = connection.execute(Operation.Util.createRhqOperation(EngineComponent.createGetRepositories(),
                                                                                           ModeShapePlugin.createModeShapeAddress()));
            final Object temp = repoResult.getResult();

            if ((temp != null) && (temp instanceof Map<?, ?>)) {
                final Map<?, ?> repoMap = (Map<?, ?>)temp;

                // no repositories found
                if (repoMap.isEmpty()) {
                    ModeShapePlugin.LOG.warn(PluginI18n.noRepositoriesDiscovered);
                    return Collections.emptySet();
                }

                // construct detail record for each repository and set properties
                final Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(repoMap.size());
                DiscoveredResourceDetails detail = null;

                for (final Object key : repoMap.keySet()) {
                    // context.getDefaultPluginConfiguration() returns a new config which is needed
                    final Configuration config = context.getDefaultPluginConfiguration(); // needs to stay inside loop
                    final String repoName = (String)key;

                    detail = new DiscoveredResourceDetails(context.getResourceType(), repoName, repoName,
                                                           ModeShapePlugin.VERSION, context.getResourceType().getDescription(),
                                                           config, null);

                    discoveredResources.add(detail);
                    ModeShapePlugin.LOG.debug("Discovered ModeShape Repository: " + repoName);
                }

                return discoveredResources;
            }
        }

        ModeShapePlugin.LOG.error(PluginI18n.errorDiscoveringRepositories);
        return Collections.emptySet();
    }

}
