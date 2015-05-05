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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform;

/**
 * Discovery component for VDBs
 * 
 */
public class VDBDiscoveryComponent implements ResourceDiscoveryComponent {

	private final Log log = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {
		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
		ASConnection connection = ((PlatformComponent) discoveryContext
				.getParentResourceComponent()).getASConnection();

		
		Address addr = DmrUtil.getTeiidAddress();
		Operation op = new Operation(Platform.Operations.LIST_VDBS, addr);
		Result result = connection.execute(op);
		ArrayList<LinkedHashMap> list = (ArrayList<LinkedHashMap>) result.getResult();
		
		PropertySimple displayPreviewVdbs = ((PlatformComponent)discoveryContext.getParentResourceComponent()).getResourceConfiguration().getSimple(PlatformComponent.DISPLAY_PREVIEW_VDBS);
		
		//Iterate through VDBs
		for (LinkedHashMap<String, Object> map : list) {

			boolean skipVdb = false;
			if (!displayPreviewVdbs.getBooleanValue()){
				ArrayList<Map <String, Object>> properties = (ArrayList<Map <String, Object>>) map.get(VDBComponent.PROPERTIES);
				if (properties != null) {
					for (Map <String, Object> propertyMap : properties) {
						String propertyName = (String) propertyMap.get(VDBComponent.PROPERTY_NAME);
						if (!propertyName.equals("preview")) continue;
						String propertyValue = (String) propertyMap.get(VDBComponent.PROPERTY_VALUE);
						if (propertyValue.equals(Boolean.TRUE.toString())) skipVdb=true;
						break;
					}
				}	
			}
				
			//If this is a Preview VDB and displayPreviewVdbs is false, skip this VDB
			if (skipVdb) continue;
				
			String vdbKey = (String) map.get(VDBComponent.VDBNAME);
			String vdbName = vdbKey;
			Integer vdbVersion = (Integer) map.get(VDBComponent.VERSION);
			String vdbDescription = (String) map.get(VDBComponent.DESCRIPTION);
			String vdbStatus = (String) map.get(VDBComponent.STATUS);
			

			/**
			 * 
			 * A discovered resource must have a unique key, that must stay the
			 * same when the resource is discovered the next time
			 */
			DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
					discoveryContext.getResourceType(), // ResourceType
					vdbKey, // Resource Key
					vdbName, // Resource Name
					vdbVersion.toString(), // Version
					PluginConstants.ComponentType.VDB.DESCRIPTION, // Description
					discoveryContext.getDefaultPluginConfiguration(), // Plugin Config
					null // Process info from a process scan
			);

			// Get plugin config map for properties
			Configuration configuration = detail.getPluginConfiguration();

			configuration.put(new PropertySimple("name", vdbName));
			configuration.put(new PropertySimple("version", vdbVersion));
			configuration
					.put(new PropertySimple("description", vdbDescription));
			configuration.put(new PropertySimple("status", vdbStatus));

			detail.setPluginConfiguration(configuration);

			// Add to return values
			discoveredResources.add(detail);
			log.debug("Discovered Teiid VDB: " + vdbName);
		}

		return discoveredResources;
	}

}