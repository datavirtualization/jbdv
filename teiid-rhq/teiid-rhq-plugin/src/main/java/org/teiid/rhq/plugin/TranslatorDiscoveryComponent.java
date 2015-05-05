/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.rhq.plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform;

/**
 * Discovery component for Teiid Translator instances
 * 
 */
public class TranslatorDiscoveryComponent implements ResourceDiscoveryComponent {

	private final Log log = LogFactory.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {
		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
		ASConnection connection = ((PlatformComponent) discoveryContext
				.getParentResourceComponent()).getASConnection();
		
		Address addr = DmrUtil.getTeiidAddress();
		Operation op = new Operation(Platform.Operations.lIST_TRANSLATORS, addr);
		Result result = connection.execute(op);
		ArrayList<LinkedHashMap<String, Object>> translatorlist = (ArrayList<LinkedHashMap<String, Object>>) result.getResult();
	

		//Iterate through VDBs
		for (LinkedHashMap<String, Object> map : translatorlist) {
			
			String translatorKey = (String) map.get(TranslatorComponent.TRANSLATORNAME);
			String translatorName = translatorKey;
			String description = (String) map.get(TranslatorComponent.DESCRIPTION);

			/**
			 * 
			 * A discovered resource must have a unique key, that must stay the
			 * same when the resource is discovered the next time
			 */
			//TODO: Figure out version
			DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
					discoveryContext.getResourceType(), // ResourceType
					translatorKey, // Resource Key
					translatorName, // Resource Name
					"1.0", // Version
					description, // Description
					discoveryContext.getDefaultPluginConfiguration(), // Plugin config
					null // Process info from a process scan
			);

			// Add to return values
			discoveredResources.add(detail);
			log.debug("Discovered Teiid Translator: " + translatorName);
		}

		return discoveredResources;
	}

}