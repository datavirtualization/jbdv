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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.admin.TeiidModuleView;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform;
import org.teiid.rhq.plugin.util.PluginConstants.Operation;

/**
 * 
 */
public class PlatformComponent extends Facet {
	public static final String ODBC_TRANSPORT_CONFIGURATION = "ODBCTransportConfiguration";
	public static final String JDBC_TRANSPORT_CONFIGURATION = "JDBCTransportConfiguration";
	public static final String DOT = ".";
	private final Log LOG = LogFactory.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);
	public static final String DISPLAY_PREVIEW_VDBS = "displayPreviewVDBS";

	@Override
	public void start(ResourceContext context) {
		this.setComponentName(context.getPluginConfiguration().getSimpleValue(	"name", null)); //$NON-NLS-1$
		this.resourceConfiguration = context.getPluginConfiguration();
		
		
		try {
			super.start(context);
			}catch (Exception e){
				
			}
	}

	/**
	 * @see org.teiid.rhq.plugin.Facet#getComponentType()
	 * @since 7.0
	 */
	@Override
	String getComponentType() {
		return PluginConstants.ComponentType.Platform.NAME;
	}

	@Override
	public AvailabilityType getAvailability() {

		Address address = DmrUtil.getTeiidAddress();
		Result result;
		result = getASConnection().execute(new ReadResource(address));
		return (result.isSuccess()) ? AvailabilityType.UP: AvailabilityType.DOWN;
	}

	@Override
	protected void setOperationArguments(String name,
			Configuration configuration, Map<String, Object> valueMap) {
		// Parameter logic for System Operations
		if (name.equals(Platform.Operations.KILL_REQUEST)) {
			valueMap.put(Operation.Value.REQUEST_ID, configuration.getSimple(Operation.Value.REQUEST_ID).getLongValue());
			valueMap.put(Operation.Value.SESSION_ID, configuration.getSimple(Operation.Value.SESSION_ID).getStringValue());
		} else if (name.equals(Platform.Operations.KILL_TRANSACTION)) {
			valueMap.put(Operation.Value.TRANSACTION_ID, configuration.getSimple(Operation.Value.TRANSACTION_ID).getStringValue());
		} else if (name.equals(Platform.Operations.KILL_SESSION)) {
			valueMap.put(Operation.Value.SESSION_ID, configuration.getSimple(Operation.Value.SESSION_ID).getStringValue());
		} else if (name.equals(Platform.Operations.DEPLOY_VDB_BY_URL)) {
			valueMap.put(Operation.Value.VDB_URL, configuration.getSimple(Operation.Value.VDB_URL).getStringValue());
			valueMap.put(Operation.Value.VDB_NAME, configuration.getSimple(Operation.Value.VDB_NAME).getStringValue());
		}
	}

	@Override
	public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

		TeiidModuleView view = new TeiidModuleView();

		Map<String, Object> valueMap = new HashMap<String, Object>();

		try {
			for (MeasurementScheduleRequest request : requests) {
				String name = request.getName();
				LOG.debug("Measurement name = " + name); //$NON-NLS-1$

				// Initialize any parameters to be used in the retrieval of
				// metric values

				Object metric = view.getMetric(getASConnection(),
						getComponentType(), this.getComponentIdentifier(),
						name, valueMap);

				if (metric instanceof Double) {
					report.addData(new MeasurementDataNumeric(request, (Double) metric));
				}
				else if (metric instanceof Integer ){
					report.addData(new MeasurementDataNumeric(request, new Double(((Integer)metric).doubleValue())));
				}
				else if (metric instanceof Long){
					report.addData(new MeasurementDataNumeric(request, new Double(((Long)metric).longValue())));
				}
				else {
					LOG.error("Metric value must be a numeric value"); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to obtain measurement [" + name 	+ "]. Cause: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			throw (e);
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.teiid.rhq.plugin.Facet#loadResourceConfiguration()
	 */
	@Override
	public Configuration loadResourceConfiguration() {

		// Get plugin config
		Configuration c = resourceConfiguration;
		
		PropertySimple propSimple = resourceConfiguration.getSimple(DISPLAY_PREVIEW_VDBS);
		
		if (propSimple.getStringValue()==null){
			c.put(new PropertySimple( DISPLAY_PREVIEW_VDBS, Boolean.FALSE));
		}
		
		getProperties(c);

		return c;

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

		super.updateResourceConfiguration(report);
	}
	
	/**
	 * @param mc
	 * @param configuration
	 * @throws Exception
	 */
	private void getProperties(Configuration configuration) {

		// Get all Teiid base resource properties
		Result result = null;
		result = getASConnection().execute(new ReadResource(DmrUtil.getTeiidAddress()));
		LinkedHashMap resultCollection = null;
		
		if (result.isSuccess()){
			resultCollection = (LinkedHashMap)result.getResult();
		}
		
		Iterator<String> propKeyIter = resultCollection.keySet().iterator();
		
		while (propKeyIter.hasNext()) {
			String name = propKeyIter.next();
			Object value = (Object)resultCollection.get(name);
			setProperties(name, value, configuration);
		}
		
		//Now get JDBC Transport properties
		result = getASConnection().execute(new ReadResource(DmrUtil.getJDBCTransportAddress()));
		
		if (result.isSuccess()){
			resultCollection = (LinkedHashMap)result.getResult();
		}
		
		propKeyIter = resultCollection.keySet().iterator();
		
		while (propKeyIter.hasNext()) {
			String name = propKeyIter.next();
			Object value = (Object)resultCollection.get(name);
			setProperties(JDBC_TRANSPORT_CONFIGURATION + DOT + name, value, configuration);
		}
		
		//Now get ODBC Transport properties
		result = getASConnection().execute(new ReadResource(DmrUtil.getJDBCTransportAddress()));
		
		if (result.isSuccess()){
			resultCollection = (LinkedHashMap)result.getResult();
		}
		
		propKeyIter = resultCollection.keySet().iterator();
		
		while (propKeyIter.hasNext()) {
			String name = propKeyIter.next();
			Object value = (Object)resultCollection.get(name);
			setProperties(ODBC_TRANSPORT_CONFIGURATION + DOT + name, value, configuration);
		}
		
		
	}
	
	/**
	 * @param mcMap
	 * @param configuration
	 */
	private void setProperties(String name, Object value, Configuration configuration) {
				PropertySimple propSimple = new PropertySimple(name, value); //$NON-NLS-1$
				configuration.put(propSimple);
	}

	@Override
	public ASConnection getASConnection() {
		return ((BaseComponent<BaseComponent<?>>) this.resourceContext.getParentResourceComponent()).getASConnection();
	}

}
