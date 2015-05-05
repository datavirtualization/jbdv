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
import org.modeshape.rhq.util.I18n;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Used to discover the ModeShape repository text extractor components.
 */
public class TextExtractorDiscoveryComponent implements ResourceDiscoveryComponent<TextExtractorComponent> {

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources( final ResourceDiscoveryContext<TextExtractorComponent> context )
        throws Exception {
        final ModeShapeComponent repository = context.getParentResourceComponent();

        if (!(repository instanceof RepositoryComponent)) {
            throw new InvalidPluginConfigurationException(I18n.bind(PluginI18n.unexpectedParentComponent,
                                                                    repository.getClass().getName(),
                                                                    getClass().getSimpleName()));
        }

        final Address repoAddr = new Address(repository.getAddress());
        final ASConnection connection = repository.getASConnection();
        final Result result = connection.execute(Operation.Util.createRhqOperation(RepositoryComponent.createGetTextExtractors(),
                                                                                   repoAddr));

        if (result.isSuccess()) {
            final Object temp = result.getResult();

            if ((temp != null) && (temp instanceof Map<?, ?>)) {
                final Map<?, ?> textExtractorMap = (Map<?, ?>)temp;

                if (textExtractorMap.isEmpty()) {
                    ModeShapePlugin.LOG.debug("No text extractors discovered in repository '" + repository.deploymentName()
                                              + '\'');
                } else {
                    final Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(
                                                                                                                      textExtractorMap.size());
                    DiscoveredResourceDetails detail = null;

                    for (final Object key : textExtractorMap.keySet()) {
                        // context.getDefaultPluginConfiguration() returns a new config which is needed
                        final Configuration config = context.getDefaultPluginConfiguration(); // needs to stay inside loop
                        final String textExtractorName = (String)key;

                        detail = new DiscoveredResourceDetails(context.getResourceType(), textExtractorName, textExtractorName,
                                                               ModeShapePlugin.VERSION,
                                                               context.getResourceType().getDescription(), config, null);

                        discoveredResources.add(detail);
                        ModeShapePlugin.LOG.debug("Discovered ModeShape Text Extractor '" + textExtractorName
                                                  + "' in repository '" + repository.deploymentName() + '\'');
                    }

                    return discoveredResources;
                }

                return Collections.emptySet();
            }
        }

        return Collections.emptySet();
    }

}
