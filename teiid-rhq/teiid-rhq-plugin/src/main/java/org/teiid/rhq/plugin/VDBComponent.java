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
import java.util.Collection;
import java.util.HashMap;
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
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.admin.TeiidModuleView;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.VDB;
import org.teiid.rhq.plugin.util.PluginConstants.Operation;

/**
 * Component class for a Teiid VDB
 * 
 */
public class VDBComponent extends Facet {
	private final Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);
	
	public static final String VDBNAME = "vdb-name"; //$NON-NLS-1$
	public static final String CONNECTIONTYPE = "connection-type"; //$NON-NLS-1$
	public static final String STATUS = "status"; //$NON-NLS-1$
	public static final String VERSION = "vdb-version"; //$NON-NLS-1$
	
	public static final String IMPORT_VDBS = "import-vdbs"; //$NON-NLS-1$
	public static final String VDB_DESCRIPTION = "vdb-description"; //$NON-NLS-1$
	public static final String PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String PROPERTY_NAME = "property-name"; //$NON-NLS-1$
	public static final String PROPERTY_VALUE = "property-value"; //$NON-NLS-1$
	public static final String DYNAMIC = "xml-deployment"; //$NON-NLS-1$
	public static final String DATA_POLICIES = "data-policies"; //$NON-NLS-1$
	public static final String DESCRIPTION = "vdb-description"; //$NON-NLS-1$
	public static final String ENTRIES = "entries"; //$NON-NLS-1$
	
	//Models
	public static final String MODELS = "models"; //$NON-NLS-1$
	public static final String MODELTYPE = "model-type"; //$NON-NLS-1$
	public static final String PHYSICAL = "PHYSICAL"; //$NON-NLS-1$
	public static final String MODELNAME = "model-name"; //$NON-NLS-1$
	public static final String VISIBLE = "visible"; //$NON-NLS-1$
	public static final String SOURCE_MAPPINGS = "source-mappings"; //$NON-NLS-1$
	public static final String SOURCE_NAME = "source-name"; //$NON-NLS-1$
	public static final String JNDI_NAME = "jndi-name"; //$NON-NLS-1$
	public static final String DS_NAME = "ds-name"; //$NON-NLS-1$
	public static final String VALIDITY_ERRORS = "validity-errors"; //$NON-NLS-1$
	public static final String SEVERITY = "severity"; //$NON-NLS-1$
	public static final String MESSAGE = "message"; //$NON-NLS-1$
	public static final String METADATA = "metadata"; //$NON-NLS-1$
	public static final String METADATA_TYPE = "metadata-type"; //$NON-NLS-1$
	
	//Translators
	public static final String OVERRIDE_TRANSLATORS = "override-translators"; //$NON-NLS-1$
	public static final String TRANSLATOR_NAME = "translator-name"; //$NON-NLS-1$
	public static final String BASE_TYPE = "base-type"; //$NON-NLS-1$
	public static final String MODULE_NAME = "module-name"; //$NON-NLS-1$
		

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.teiid.rhq.plugin.Facet#start(org.rhq.core.pluginapi.inventory.
	 * ResourceContext)
	 */
	@Override
	public void start(ResourceContext context) {
		this.setComponentName(context.getPluginConfiguration().getSimpleValue(
				"fullName", null));
		this.resourceConfiguration = context.getPluginConfiguration();
		this.componentType = PluginConstants.ComponentType.VDB.NAME;
		
		try {
			super.start(context);
			}catch (Exception e){
				
			}
	}

	@Override
	protected void setOperationArguments(String name,
			Configuration configuration, Map<String, Object> valueMap) {
		// Parameter logic for VDB Metrics
		valueMap.put(VDB.NAME, this.resourceConfiguration.getSimpleValue("name",
				null));
		valueMap.put(VDB.VERSION, this.resourceConfiguration.getSimpleValue(
				VDB.VERSION, null));

		// Parameter logic for VDB Operations
		if (name.equals(VDB.Operations.KILL_REQUEST)) {
			valueMap.put(Operation.Value.REQUEST_ID, configuration.getSimple(
					Operation.Value.REQUEST_ID).getLongValue());
			valueMap.put(Operation.Value.SESSION_ID, configuration.getSimple(
					Operation.Value.SESSION_ID).getLongValue());
		} else if (name.equals(VDB.Operations.CLEAR_CACHE)) {
				valueMap.put(Operation.Value.CACHE_TYPE, configuration.getSimple(
					Operation.Value.CACHE_TYPE).getStringValue());
		} else if (name.equals(Platform.Operations.KILL_SESSION)) {
			valueMap.put(Operation.Value.SESSION_ID, configuration.getSimple(
					Operation.Value.SESSION_ID).getLongValue());
		} else if (name.equals(Platform.Operations.KILL_SESSION)) {
			valueMap.put(Operation.Value.SESSION_ID, configuration.getSimple(
					Operation.Value.SESSION_ID).getLongValue());
		} else if (name.equals(VDB.Operations.RELOAD_MATVIEW)) {
			valueMap
					.put(Operation.Value.MATVIEW_SCHEMA, configuration
							.getSimple(Operation.Value.MATVIEW_SCHEMA)
							.getStringValue());
			valueMap.put(Operation.Value.MATVIEW_TABLE, configuration
					.getSimple(Operation.Value.MATVIEW_TABLE).getStringValue());
			valueMap.put(Operation.Value.INVALIDATE_MATVIEW, configuration
					.getSimple(Operation.Value.INVALIDATE_MATVIEW)
					.getBooleanValue());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teiid.rhq.plugin.Facet#getAvailability()
	 */
	@Override
	public AvailabilityType getAvailability() {
		Map<String, Object> map = getVdbMap(getASConnection(), this.deploymentName, this.resourceConfiguration.getSimple("version").getStringValue());
		String status = (String) map.get(STATUS);
		if (status.equals("ACTIVE")) {
			return AvailabilityType.UP;
		}

		return AvailabilityType.DOWN;
	}

	@Override
	protected void setMetricArguments(String name, Configuration configuration,
			Map<String, Object> valueMap) {
		// Parameter logic for VDB Metrics
		valueMap.put(VDB.NAME, this.deploymentName);
		valueMap.put(VDB.VERSION, this.resourceConfiguration.getSimple("version").getStringValue());
	}

	@Override
	public void getValues(MeasurementReport report,
			Set<MeasurementScheduleRequest> requests) throws Exception {

		TeiidModuleView view = new TeiidModuleView();

		Map<String, Object> valueMap = new HashMap<String, Object>();
		setMetricArguments(VDB.NAME, null, valueMap);

		for (MeasurementScheduleRequest request : requests) {
			String name = request.getName();
			LOG.debug("Measurement name = " + name); //$NON-NLS-1$

			Object metricReturnObject = view.getMetric(getASConnection(),
					getComponentType(), this.getComponentIdentifier(), name,
					valueMap);

			try {
				if (request.getName().equals(
						PluginConstants.ComponentType.VDB.Metrics.ERROR_COUNT)) {
					String message = "";
					if (((Integer) metricReturnObject) > 0) {
						message = "** There are "
								+ ((Integer) metricReturnObject)
								+ " errors reported for this VDB. See the Configuration tab for details. **";
					} else {
						message = "** There are no errors reported for this VDB. **";
					}

					report.addData(new MeasurementDataTrait(request, message));
				} else {
					if (request
							.getName()
							.equals(
									PluginConstants.ComponentType.VDB.Metrics.QUERY_COUNT)) {
						report.addData(new MeasurementDataNumeric(request,
								(Double) metricReturnObject));
					} else {
						if (request
								.getName()
								.equals(
										PluginConstants.ComponentType.VDB.Metrics.SESSION_COUNT)) {
							report.addData(new MeasurementDataNumeric(request,
									(Double) metricReturnObject));
						} else {
							if (request
									.getName()
									.equals(
											PluginConstants.ComponentType.VDB.Metrics.STATUS)) {
								if (((String) metricReturnObject)
										.equals("ACTIVE")) {
									report.addData(new MeasurementDataTrait(
											request, "UP"));
								} else {
									report.addData(new MeasurementDataTrait(
											request, "DOWN"));
								}
							} else {
								if (request
										.getName()
										.equals(
												PluginConstants.ComponentType.VDB.Metrics.LONG_RUNNING_QUERIES)) {
									report.addData(new MeasurementDataNumeric(
											request,
											(Double) metricReturnObject));
								}
							}

						}
					}
				}

			} catch (Exception e) {
				LOG.error("Failed to obtain measurement [" + name //$NON-NLS-1$
						+ "]. Cause: " + e); //$NON-NLS-1$
				// throw(e);
			}
		}

	}

	@Override
	String getComponentType() {
		return PluginConstants.ComponentType.VDB.NAME;
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
		
		String vdbName = this.resourceConfiguration.getSimpleValue("name", null);
		String vdbVersion = this.resourceConfiguration.getSimpleValue(VDB.VERSION, null);
		
		getPropertiesMap().put(VDB.NAME, vdbName);
		getPropertiesMap().put(VDB.VERSION, vdbVersion);

		// First update simple properties
		super.updateResourceConfiguration(report);

		// Then update models
		report.setStatus(ConfigurationUpdateStatus.SUCCESS);
		
		List<Property> multiSourceModelsPropertyList = resourceConfiguration
				.getList("multiSourceModels").getList();
		List<Property> singleSourceModelsPropertyList = resourceConfiguration
				.getList("singleSourceModels").getList();
		ArrayList<List<Property>> sourceMappingList = new ArrayList<List<Property>>();
		sourceMappingList.add(singleSourceModelsPropertyList);
		sourceMappingList.add(multiSourceModelsPropertyList);
		for (List<Property> sourceList:sourceMappingList) {
			for (Property model:sourceList) {
				String sourceName = ((PropertySimple) ((PropertyMap)model)
						.get("sourceName")).getStringValue(); //$NON-NLS-1$
				if (sourceName.equals("See below"))
					continue; // This is a multisource model which we will handle separately
				String modelName = ((PropertySimple) ((PropertyMap)model).get("name")) //$NON-NLS-1$
						.getStringValue();
				String translatorName = ((PropertySimple) ((PropertyMap)model).get("translatorName")) //$NON-NLS-1$
						.getStringValue();
				String dsName = ((PropertySimple) ((PropertyMap)model).get("jndiName")) //$NON-NLS-1$
						.getStringValue();
				
				Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
				additionalProperties.put(VDBComponent.VDBNAME, vdbName);
				additionalProperties.put(VDBComponent.VERSION, vdbVersion);
				additionalProperties.put(VDBComponent.MODELNAME, modelName);
				additionalProperties.put(VDBComponent.SOURCE_NAME, sourceName);
				additionalProperties.put(VDBComponent.TRANSLATOR_NAME, translatorName);
				additionalProperties.put(VDBComponent.DS_NAME, dsName);
				Result result = TeiidModuleView.executeOperation(getASConnection(), VDB.Operations.ASSIGN_DATASOURCE, DmrUtil.getTeiidAddress(), additionalProperties);
				if (!result.isSuccess()){
					report.setStatus(ConfigurationUpdateStatus.FAILURE);
					report.setErrorMessage(result.getFailureDescription());
				}
			}
		}
		

	}

	@Override
	public Configuration loadResourceConfiguration() {
		
		Map<String, Object> map = getVdbMap(getASConnection(), this.deploymentName, this.resourceConfiguration.getSimple("version").getStringValue());
		
		String vdbName = (String) map.get(VDBNAME);
		Integer vdbVersion = (Integer) map.get(VERSION);
		String vdbDescription = (String) map.get(DESCRIPTION);
		String vdbStatus = (String) map.get(STATUS);
		String connectionType = (String) map.get(CONNECTIONTYPE);
		Boolean isDynamic = (Boolean) map.get(DYNAMIC);
		
		// Get plugin config map for models
		Configuration configuration = this.getResourceConfiguration();

		configuration.put(new PropertySimple("name", vdbName));
		configuration.put(new PropertySimple("version", vdbVersion));
		configuration.put(new PropertySimple("description", vdbDescription));
		configuration.put(new PropertySimple("status", vdbStatus));
		configuration.put(new PropertySimple("connectionType", connectionType));
		configuration.put(new PropertySimple("dynamic", isDynamic.toString()));

		try {
			getTranslators(map, configuration);
		} catch (Exception e) {
			final String msg = "Exception in loadResourceConfiguration(): " + e.getMessage(); //$NON-NLS-1$
			LOG.error(msg, e);
		}

		getModels(map, configuration);

		return configuration;

	}

	public static Map<String, Object> getVdbMap(ASConnection connection,String vdbName, String vdbVersion) {
		Address addr = DmrUtil.getTeiidAddress();
		org.rhq.modules.plugins.jbossas7.json.Operation op = new org.rhq.modules.plugins.jbossas7.json.Operation(Platform.Operations.GET_VDB, addr);
		Map<String, Object> additionalProperties = new HashMap<String, Object>();
		additionalProperties.put(VDBNAME, vdbName);
		additionalProperties.put(VERSION, vdbVersion);
		op.setAdditionalProperties(additionalProperties);
		Result result = connection.execute(op);
		LinkedHashMap<String, Object> map = null;

		if (result.isSuccess()){
			map = (LinkedHashMap<String, Object>) result.getResult();
		}
		return map;
	}

	/**
	 * @param mcVdb
	 * @param configuration
	 */
	private void getModels(Map<String,?> vdbMap, Configuration configuration) {
	
		PropertyList sourceModelsList = new PropertyList("singleSourceModels");
		configuration.put(sourceModelsList);

		PropertyList multiSourceModelsList = new PropertyList(
				"multiSourceModels");
		configuration.put(multiSourceModelsList);

		PropertyList logicalModelsList = new PropertyList("logicalModels");
		configuration.put(logicalModelsList);

		PropertyList errorList = new PropertyList("errorList");
		configuration.put(errorList);
		
		ArrayList<Map<String, Object>> modelList = (ArrayList<Map<String, Object>>) vdbMap.get(MODELS);

		for (Map<String, Object> modelMap: modelList) {
			
			String modelType = null;
			
			try {
				modelType = (String) modelMap.get(MODELTYPE);
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			
			boolean isSource = (modelType.equals(PHYSICAL)?true:false);

			Object supportMultiSourceObject = null;
			Boolean supportMultiSource = true;
			try {
				List<Map<String,Object>> propMap = (List<Map<String,Object>>)modelMap.get(PROPERTIES);
				for (Map<String,Object> prop:propMap){
					String propName = (String) prop.get(PROPERTY_NAME);
					if (propName.equals("supports-multi-source-bindings")){
						supportMultiSourceObject = prop.get(PROPERTY_VALUE);
						break;
					}
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			
			if (supportMultiSourceObject==null){
				supportMultiSource = false; //if property is not there, we assume false
			}else{
				supportMultiSource = Boolean.valueOf((String)supportMultiSourceObject);
			}

			String modelName = (String) modelMap.get(MODELNAME);
			Boolean visibility = (Boolean) modelMap.get(VISIBLE);
			String type = (String) modelMap.get(MODELTYPE);

			// Get any model errors/warnings
			ArrayList<Map<String, String>> errors = (ArrayList<Map<String, String>>) modelMap.get(VALIDITY_ERRORS);

			if (errors != null) {
				for (Map<String, String> error : errors) {
					String severity = (String) error.get(SEVERITY);
					String message =  (String) error.get(MESSAGE);

					PropertyMap errorMap = new PropertyMap("errorMap",
							new PropertySimple("severity", severity),
							new PropertySimple("message", message));
					errorList.add(errorMap);
				}
			}

			if (isSource) {
				List<Map<String,Object>> sourceList = (ArrayList<Map<String,Object>>)modelMap.get(SOURCE_MAPPINGS);
				int sourceCount = 0;
				for (Map<String, Object> sourceMap : sourceList) {
	
					sourceCount++;
					String sourceName = (String) sourceMap.get(SOURCE_NAME);
					String jndiName = (String) sourceMap.get(JNDI_NAME);
					String translatorName = (String) sourceMap
							.get(TRANSLATOR_NAME);
					PropertyMap multiSourceModel = null;

					PropertyMap model = null;
					if (supportMultiSource) {
						multiSourceModel = new PropertyMap("map",
								new PropertySimple("name", modelName),
								new PropertySimple("sourceName", sourceName),
								new PropertySimple("jndiName", jndiName),
								new PropertySimple("translatorName",
										translatorName));

						multiSourceModelsList.add(multiSourceModel);

						if (sourceCount==1){
							model = new PropertyMap("map", new PropertySimple(
									"name", modelName), new PropertySimple(
									"sourceName", "See below"), new PropertySimple(
									"jndiName", "See below"), new PropertySimple(
									"translatorName", "See below"),
									new PropertySimple("visibility", visibility),
									new PropertySimple("supportsMultiSource", true));
							sourceModelsList.add(model);
						}
					} else {
						model = new PropertyMap("map", new PropertySimple(
								"name", modelName), new PropertySimple(
								"sourceName", sourceName), new PropertySimple(
								"jndiName", jndiName), new PropertySimple(
								"translatorName", translatorName),
								new PropertySimple("visibility", visibility),
								new PropertySimple("supportsMultiSource",
										supportMultiSource));
						sourceModelsList.add(model);
					}
				} 
			} else {
					PropertyMap model = new PropertyMap("map",
							new PropertySimple("name", modelName),
							new PropertySimple("type", type),
							new PropertySimple("visibility", visibility));

					logicalModelsList.add(model);
				}
			}
	}
	

	/**
	 * @param <T>
	 * @param pValue
	 * @param list
	 */
	public static <T> void getSourceMappingValue(ArrayList<Map<String,String>> pValue,
			Collection<Map<String, String>> list) {
		Map<String, String> map = new HashMap<String, String>();
		list.add(map);
		for (Map<String,String> value : pValue) {
			String sourceName = value.get(SOURCE_NAME);
			String jndi = value.get(JNDI_NAME);
			String translatorName =  value.get(TRANSLATOR_NAME);
			map.put("name", sourceName);
			map.put("jndiName", jndi);
			map.put("translatorName", translatorName);
		}
	}

	/**
	 * @param mcVdb
	 * @param configuration
	 * @throws Exception 
	 */
	private void getTranslators(Map<String, Object> map,
			Configuration configuration) throws Exception {
		ArrayList<Map<String, Object>> arrayList = (ArrayList<Map<String, Object>> )map.get("override-translators");
		if (arrayList == null) {
			return;
		}

		PropertyList translatorsList = new PropertyList("translators");
		configuration.put(translatorsList);

		for (Map<String, Object> translator : arrayList) {
			
			String translatorName = (String) translator.get(TRANSLATOR_NAME);
			String translatorType = (String) translator.get(BASE_TYPE);
			String moduleName = (String) translator.get(MODULE_NAME);
			ArrayList<Map <String, Object>> properties = (ArrayList<Map <String, Object>>) translator.get(PROPERTIES);

			if (properties != null) {
				for (Map <String, Object> propertyMap : properties) {
					String propertyName = (String) propertyMap.get(PROPERTY_NAME);
					String propertyValue = (String) propertyMap.get(PROPERTY_VALUE);
					PropertyMap translatorMap = null;

					translatorMap = new PropertyMap("translatorMap",
							new PropertySimple("name", translatorName),
							new PropertySimple("type", translatorType),
							new PropertySimple("moduleName", moduleName),
							new PropertySimple("propertyName", propertyName),
							new PropertySimple("propertyValue", propertyValue));
					// Only want translator name and value to show up for the
					// first row,
					// so we will blank them out here.
					translatorName = "";
					translatorType = "";
					translatorsList.add(translatorMap);
				}
			}
		}
	}

	@Override
	public ASConnection getASConnection() {
		return ((PlatformComponent) this.resourceContext
				.getParentResourceComponent()).getASConnection();
	}

}
