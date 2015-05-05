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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.version.PackageVersions;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.teiid.rhq.admin.TeiidModuleView;
import org.teiid.rhq.plugin.objects.ConfigurationWriteDelegate;
import org.teiid.rhq.plugin.objects.ExecutedOperationResultImpl;
import org.teiid.rhq.plugin.objects.ExecutedResult;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;

/**
 * This class implements required RHQ interfaces and provides common logic used
 * by all MetaMatrix components.
 */
public abstract class Facet extends BaseComponent<BaseComponent<?>> implements
		MeasurementFacet,
		OperationFacet, ConfigurationFacet {

	protected final Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	/**
	 * Represents the resource configuration of the custom product being
	 * managed.
	 */
	protected Configuration resourceConfiguration;
	

	/**
	 * Represents the plugin configuration of the custom product being
	 * managed.
	 */
	protected Configuration pluginConfiguration;

	/**
	 * All AMPS plugins are stateful - this context contains information that
	 * your resource component can use when performing its processing.
	 */
	protected ResourceContext<?> resourceContext;

	protected String name;

	private String identifier;

	protected String componentType;

	protected boolean isAvailable = false;

	private final Log log = LogFactory.getLog(this.getClass());

	private Map<String, String> propertiesMap = new HashMap<String, String>();
	
	Address address;
	
	public Address getAddress() {
		return  address;
	}

	String key;

	protected String deploymentName;
	protected String deploymentUrl;

	private PackageVersions versions = null;

	/**
	 * Name of the backing package type that will be used when discovering
	 * packages. This corresponds to the name of the package type defined in the
	 * plugin descriptor. For simplicity, the package type for VDBs is called
	 * "vdb". This is still unique within the context of the parent resource
	 * type and lets this class use the same package type name in both cases.
	 */
	private static final String PKG_TYPE_VDB = "vdb"; //$NON-NLS-1$

	/**
	 * Architecture string used in describing discovered packages.
	 */
	private static final String ARCHITECTURE = "noarch"; //$NON-NLS-1$

	abstract String getComponentType();

	/**
	 * This is called when your component has been started with the given
	 * context. You normally initialize some internal state of your component as
	 * well as attempt to make a stateful connection to your managed resource.
	 * 
	 * @see ResourceComponent#start(ResourceContext)
	 */
	public void start(ResourceContext context) {
		resourceContext = context;
		deploymentName = context.getResourceKey();
        pluginConfiguration = context.getPluginConfiguration();
        address = DmrUtil.getTeiidAddress();
        key = context.getResourceKey();
	}

	/**
	 * This is called when the component is being stopped, usually due to the
	 * plugin container shutting down. You can perform some cleanup here; though
	 * normally not much needs to be done here.
	 * 
	 * @see ResourceComponent#stop()
	 */
	public void stop() {
		this.isAvailable = false;
	}

	/**
	 * @return the resourceConfiguration
	 */
	public Configuration getResourceConfiguration() {
		return resourceConfiguration;
	}

	/**
	 * @param resourceConfiguration
	 *            the resourceConfiguration to set
	 */
	public void setResourceConfiguration(Configuration resourceConfiguration) {
		this.resourceConfiguration = resourceConfiguration;
	}

	public String componentType() {
		return name;
	}

	protected void setComponentName(String componentName) {
		this.name = componentName;
	}

	public String getComponentIdentifier() {
		return identifier;
	}

	protected void setComponentIdentifier(String identifier) {
		this.identifier = identifier;
	}

	protected void setOperationArguments(String name,
			Configuration configuration, Map<String, Object> argumentMap) {
		// moved this logic up to the associated implemented class
		throw new InvalidPluginConfigurationException(
				"Not implemented on component type " + this.getComponentType() //$NON-NLS-1$
						+ " named " + this.name); //$NON-NLS-1$

	}

	protected void setMetricArguments(String name, Configuration configuration,
			Map<String, Object> argumentMap) {
		// moved this logic up to the associated implemented class
		throw new InvalidPluginConfigurationException(
				"Not implemented on component type " + this.getComponentType() //$NON-NLS-1$
						+ " named " + this.name); //$NON-NLS-1$

	}

	protected void execute(final ASConnection connection,
			final ExecutedResult result, final Map<String, Object> valueMap) {
		TeiidModuleView dqp = new TeiidModuleView();

		try {
			dqp.executeOperation(connection, result, valueMap);
		} catch (Exception e) {
			new RuntimeException(e);
		}

	}

	/*
	 * (non-Javadoc) This method is called by JON to check the availability of
	 * the inventoried component on a time scheduled basis
	 * 
	 * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
	 */
	public AvailabilityType getAvailability() {

		LOG.debug("Checking availability of  " + identifier); //$NON-NLS-1$

		return AvailabilityType.UP;
	}

	/**
	 * Helper method that indicates the latest status based on the last
	 * getAvailabilit() call.
	 * 
	 * @return true if the resource is available
	 */
	protected boolean isAvailable() {
		return true;
	}

	/**
	 * The plugin container will call this method when your resource component
	 * has been scheduled to collect some measurements now. It is within this
	 * method that you actually talk to the managed resource and collect the
	 * measurement data that is has emitted.
	 * 
	 * @see MeasurementFacet#getValues(MeasurementReport, Set)
	 */
	public abstract void getValues(MeasurementReport arg0,
			Set<MeasurementScheduleRequest> arg1) throws Exception;

	/**
	 * The plugin container will call this method when it wants to invoke an
	 * operation on your managed resource. Your plugin will connect to the
	 * managed resource and invoke the analogous operation in your own custom
	 * way.
	 * 
	 * @see OperationFacet#invokeOperation(String, Configuration)
	 */
	public OperationResult invokeOperation(String name,
			Configuration configuration) {
		Map<String, Object> valueMap = new HashMap<String, Object>();

		Set operationDefinitionSet = this.resourceContext.getResourceType()
				.getOperationDefinitions();

		ExecutedResult result = new ExecutedOperationResultImpl(this
				.getComponentType(), name, operationDefinitionSet);

		setOperationArguments(name, configuration, valueMap);

		execute(getASConnection(), result, valueMap);

		return ((ExecutedOperationResultImpl) result).getOperationResult();

	}

	@Override
	 public Configuration loadResourceConfiguration() throws Exception {

		//Implemented in the component
	     return null;
	     
	}

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        ConfigurationDefinition configDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), getAddress());
        delegate.setPropertiesMap(propertiesMap);
        delegate.updateResourceConfiguration(report);
    }

	protected static Configuration getDefaultPluginConfiguration(
			ResourceType resourceType) {
		ConfigurationTemplate pluginConfigDefaultTemplate = resourceType
				.getPluginConfigurationDefinition().getDefaultTemplate();
		return (pluginConfigDefaultTemplate != null) ? pluginConfigDefaultTemplate
				.createConfiguration()
				: new Configuration();
	}

	/**
	 * Returns an instantiated and loaded versions store access point.
	 * 
	 * @return will not be <code>null</code>
	 */
	private org.rhq.core.pluginapi.content.version.PackageVersions loadPackageVersions() {
		if (this.versions == null) {
			ResourceType resourceType = resourceContext.getResourceType();
			String pluginName = resourceType.getPlugin();
			File dataDirectoryFile = resourceContext.getDataDirectory();
			dataDirectoryFile.mkdirs();
			String dataDirectory = dataDirectoryFile.getAbsolutePath();
			log.trace("Creating application versions store with plugin name [" //$NON-NLS-1$
					+ pluginName + "] and data directory [" + dataDirectory //$NON-NLS-1$
					+ "]"); //$NON-NLS-1$
			this.versions = new PackageVersions(pluginName, dataDirectory);
			this.versions.loadFromDisk();
		}

		return this.versions;
	}
	
	public Map<String, String> getPropertiesMap() {
		return propertiesMap;
	}

	public void setPropertiesMap(Map<String, String> propertiesMap) {
		this.propertiesMap = propertiesMap;
	}

}
