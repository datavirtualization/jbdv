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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.admin.TeiidModuleView;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.DATA_ROLE;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.VDB;

/**
 * Component class for a Teiid VDB Data Role
 * 
 */
public class DataRoleComponent extends Facet {
	private final Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);
	
	
	public static final String ROLE = "role-name";
	public static final String POLICY_NAME = "policy-name";
	public static final String POLICY_DESCRIPTION = "policy-description";
	public static final String ALLOW_CREATE_TEMP_TABLES = "allow-create-temp-tables";
	public static final String ANY_AUTHENTICATED = "any-authenticated";
	public static final String DATA_PERMISSIONS = "data-permissions";
	public static final String RESOURCE_NAME = "resource-name";
	public static final String ALLOW_CREATE = "allow-create";
	public static final String ALLOW_UPDATE = "allow-update";
	public static final String ALLOW_READ = "allow-read";
	public static final String MAPPED_ROLE_NAMES = "mapped-role-names";
	private static final String ANY_AUTHENTICATED_ROLE = "anyAuthenticated";
	private String vdbName = null;
	private String vdbVersion = null;
	

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.teiid.rhq.plugin.Facet#start(org.rhq.core.pluginapi.inventory.
	 * ResourceContext)
	 */
	@Override
	public void start(ResourceContext context) {
		this.resourceConfiguration = context.getPluginConfiguration();
		this.componentType = PluginConstants.ComponentType.DATA_ROLE.NAME;
		vdbName = ((VDBComponent)context.getParentResourceComponent()).getResourceConfiguration().getSimple("name")
				.getStringValue();
		vdbVersion = ((VDBComponent)context.getParentResourceComponent()).getResourceConfiguration().getSimple("version")
		.getStringValue();
		try {
			super.start(context);
			}catch (Exception e){
				
			}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teiid.rhq.plugin.Facet#getAvailability()
	 */
	@Override
	public AvailabilityType getAvailability() {
		return ((VDBComponent) this.resourceContext
				.getParentResourceComponent()).getAvailability();
	}

	@Override
	String getComponentType() {
		return PluginConstants.ComponentType.DATA_ROLE.NAME;
	}

	@Override
	public void getValues(MeasurementReport arg0,
			Set<MeasurementScheduleRequest> arg1) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * The plugin container will call this method when it has a new
	 * configuration for your managed resource. Your plugin will re-configure
	 * the managed resource in your own custom way, setting its configuration
	 * based on the new values of the given configuration.
	 * 
	 * @see ConfigurationFacet#updateResourceConfiguration(ConfigurationUpdateReport)
	 */
	public void updateResourceConfiguration(ConfigurationUpdateReport report) {

		Configuration resourceConfig = report.getConfiguration();
		resourceConfiguration = resourceConfig.deepCopy();
		report.setStatus(ConfigurationUpdateStatus.SUCCESS);
		
		// Update date role anyAuthenticated 
		PropertySimple anyAuthRoleProperty = resourceConfiguration.getSimple(ANY_AUTHENTICATED_ROLE);		
		String roleName = resourceConfiguration.getSimple("name").getStringValue();
		Boolean anyAuthenticated = anyAuthRoleProperty.getBooleanValue();
		
		Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
		additionalProperties.put(VDBComponent.VDBNAME, vdbName);
		additionalProperties.put(VDBComponent.VERSION, vdbVersion);
		additionalProperties.put(DATA_ROLE.Operations.Parameters.DATA_ROLE, roleName);
		String authenticatedRoleOperation = null;
		if (anyAuthenticated){
			authenticatedRoleOperation = DATA_ROLE.Operations.ADD_ANYAUTHENTICATED_ROLE;
		}else{
			authenticatedRoleOperation = DATA_ROLE.Operations.REMOVE_AUTHENTICATED_ROLE;
		}
		
		Result result = TeiidModuleView.executeOperation(getASConnection(), authenticatedRoleOperation, DmrUtil.getTeiidAddress(), additionalProperties);
		if (!result.isSuccess()){
			report.setStatus(ConfigurationUpdateStatus.FAILURE);
			report.setErrorMessage(result.getFailureDescription());
			return;
		}
	
		//Now update role names by removing existing and adding list values from configuration as new	
		Map<String, Object> vdbMap = VDBComponent.getVdbMap(getASConnection(), vdbName,
				vdbVersion);
		
		// Get data roles from VDB and find the current role
		List<Map<String, Object>> dataPolicies = (List<Map<String, Object>>) vdbMap.get(VDBComponent.DATA_POLICIES);
		for (Map<String, Object> policy : dataPolicies) {
			String dataRoleName = (String) policy.get(DataRoleComponent.POLICY_NAME);
			if (dataRoleName.equals(roleName)){
				List<String> mappedRoleNames = (List<String>) policy.get(DataRoleComponent.MAPPED_ROLE_NAMES);
				//First remove all mapped roles
				if (mappedRoleNames != null) {
					for (String mappedRoleName : mappedRoleNames) {
						additionalProperties.put(DATA_ROLE.Operations.Parameters.MAPPED_ROLE_NAME, mappedRoleName);
						result = TeiidModuleView.executeOperation(getASConnection(), DATA_ROLE.Operations.REMOVE_DATA_ROLE, DmrUtil.getTeiidAddress(), additionalProperties);
						if (!result.isSuccess()){
							report.setStatus(ConfigurationUpdateStatus.FAILURE);
							report.setErrorMessage(result.getFailureDescription());
							return;
						}
					}
				}
				//Now add any mapped roles from the config
				List<Property> mappedRoleNamePropertyList = resourceConfiguration.getList("mappedRoleNameList").getList(); //$NON-NLS-1$
				if (mappedRoleNamePropertyList != null) {
					for (Property mappedRoleName : mappedRoleNamePropertyList) {
						additionalProperties.put(DATA_ROLE.Operations.Parameters.MAPPED_ROLE_NAME, ((PropertySimple)mappedRoleName).getStringValue());
						result = TeiidModuleView.executeOperation(getASConnection(), DATA_ROLE.Operations.ADD_DATA_ROLE, DmrUtil.getTeiidAddress(), additionalProperties);
						if (!result.isSuccess()){
							report.setStatus(ConfigurationUpdateStatus.FAILURE);
							report.setErrorMessage(result.getFailureDescription());
							return;
						}
					}
				}
			}
		}
	
	}

	@Override
	public Configuration loadResourceConfiguration() {

		VDBComponent parentComponent = (VDBComponent) this.resourceContext
				.getParentResourceComponent();
		Configuration configuration = this.resourceConfiguration;
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
			 
			 configuration.put(new PropertySimple("name", dataRoleName));
			 configuration.put(new PropertySimple("anyAuthenticated", anyAuthenticated));
			 configuration.put(new PropertySimple("description", description));
			 configuration.put(new PropertySimple("allowCreateTempTables", allowTempTableCreate));
			
			 //Load data permissions list
			 PropertyList dataPermissionsList = new PropertyList(
					 "dataPermissionsList");
			 configuration.put(dataPermissionsList);
			 List<Map<String, Object>> dataPermissions = (List<Map<String, Object>> ) policy.get(DataRoleComponent.DATA_PERMISSIONS);
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
		}

		return configuration;

	}

	@Override
	public ASConnection getASConnection() {
		return ((VDBComponent) this.resourceContext
				.getParentResourceComponent()).getASConnection();
	}

}
