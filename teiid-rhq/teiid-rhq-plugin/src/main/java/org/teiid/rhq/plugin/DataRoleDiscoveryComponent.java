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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.teiid.rhq.plugin.util.PluginConstants;

/**
 * Discovery component for Data Roles of a VDB
 * 
 */
public class DataRoleDiscoveryComponent implements ResourceDiscoveryComponent {

	private final Log log = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext discoveryContext)
			throws InvalidPluginConfigurationException, Exception {
		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
		VDBComponent parentComponent = (VDBComponent) discoveryContext
				.getParentResourceComponent();
		ASConnection connection = parentComponent.getASConnection();

		Map<String, Object> vdbMap = VDBComponent.getVdbMap(connection, parentComponent.deploymentName,
				parentComponent.getResourceConfiguration().getSimple("version")
						.getStringValue());

		// Get data roles from VDB
		List<Map<String, Object>> dataPolicies = (List<Map<String, Object>>) vdbMap.get(VDBComponent.DATA_POLICIES);
		for (Map<String, Object> policy : dataPolicies) {
		     String dataRoleName = (String) policy.get(DataRoleComponent.POLICY_NAME);
		     Boolean anyAuthenticated =  (Boolean) policy.get(DataRoleComponent.ANY_AUTHENTICATED);
		     String description = (String) policy.get(DataRoleComponent.POLICY_DESCRIPTION);
		     Boolean allowTempTableCreate = (Boolean) policy.get(DataRoleComponent.ALLOW_CREATE_TEMP_TABLES);
			/**
			 *
			 * A discovered resource must have a unique key, that must stay
			 * the same when the resource is discovered the next time
			 */
			 DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
			 discoveryContext.getResourceType(), // ResourceType
			 dataRoleName, // Resource Key
			 dataRoleName, // Resource Name
			 null, // Version
			 PluginConstants.ComponentType.DATA_ROLE.DESCRIPTION, // Description
			 discoveryContext.getDefaultPluginConfiguration(), // Plugin config
			 null // Process info from a process scan
			 );
			
			 
			 Configuration configuration = detail.getPluginConfiguration();
			
			 configuration.put(new PropertySimple("name", dataRoleName));
			 configuration.put(new PropertySimple("anyAuthenticated", anyAuthenticated));
			 configuration.put(new PropertySimple("description", description));
			 configuration.put(new PropertySimple("allowCreateTempTables", allowTempTableCreate));
			
			 //Load data permissions list
			 PropertyList dataPermissionsList = new PropertyList(
					 "dataPermissionsList");
			 configuration.put(dataPermissionsList);
			 List<Map<String, Object>> dataPermissions = (List<Map<String, Object>> ) policy.get(DataRoleComponent.DATA_PERMISSIONS);
			 if (dataPermissions==null) return discoveredResources;
			 if (dataPermissions != null) {
				 for (Map<String, Object> dataPermission : dataPermissions) {
					 PropertyMap dataPermissionsMap = new PropertyMap(
							 "map");
					 dataPermissionsList.add(dataPermissionsMap);
					 dataPermissionsMap.put(new PropertySimple("resourceName", dataPermission.get(DataRoleComponent.RESOURCE_NAME)));
					 dataPermissionsMap.put(new PropertySimple("allowCreate", dataPermission.get(DataRoleComponent.ALLOW_CREATE)));
					 dataPermissionsMap.put(new PropertySimple("allowUpdate", dataPermission.get(DataRoleComponent.ALLOW_UPDATE)));
					 dataPermissionsMap.put(new PropertySimple("allowRead", dataPermission.get(DataRoleComponent.ALLOW_READ)));
				 }
			
			 }
			 
			 //Load mapped role names list
			 PropertyList mappedRoleNameList = new PropertyList(
			 "mappedRoleNameList");
			 configuration.put(mappedRoleNameList);
			 List<String> mappedRoleNames = (List<String>) policy.get(DataRoleComponent.MAPPED_ROLE_NAMES);
			 if (mappedRoleNames != null) {
				 for (String mappedRoleName : mappedRoleNames) {
					 mappedRoleNameList.add(new PropertySimple("name", mappedRoleName));
				 }
			
			 }
			 
			 // Add to return values
			 discoveredResources.add(detail);
			 log.debug("Discovered Teiid VDB Data Role: " + dataRoleName);
			 }

		return discoveredResources;

	}
}