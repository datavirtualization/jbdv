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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.plugin.util.DmrUtil;

/**
 * This is the parent node for a Teiid system
 * 
 * Discover subsystems. We need to distinguish two cases denoted by the path
 * plugin config:
 * <ul>
 * <li>Path is a single 'word': here the value denotes a key in the resource
 * path of AS7, that identifies a child type see e.g. the Connectors below the
 * JBossWeb service in the plugin descriptor. There can be multiple resources of
 * the given type. In addition it is possible that a path entry in configuration
 * shares multiple types that are separated by the pipe symbol.</li>
 * <li>Path is a key-value pair (e.g. subsystem=web ). This denotes a singleton
 * subsystem with a fixed path within AS7 (perhaps below another resource in the
 * tree.</li>
 * </ul>
 * 
 */
public class PlatformDiscoveryComponent implements
		ResourceDiscoveryComponent<BaseComponent<?>> {

	private final Log log = LogFactory.getLog(this.getClass());

	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<BaseComponent<?>> context)
			throws Exception {

		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(
				DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(
				DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

		BaseComponent parentComponent = context.getParentResourceComponent();
		ASConnection connection = parentComponent.getASConnection();
		Configuration config = context.getDefaultPluginConfiguration();

		Address addr = DmrUtil.getTeiidAddress();
		Result result = connection.execute(new ReadResource(addr));
		
		if (result.isSuccess()) {
			
			Result versionResult = connection.execute(new ReadAttribute(addr, "runtime-version"));
			String version = null;
			if (versionResult.isSuccess())	{
				version = versionResult.getResult().toString();
			}

			DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
					context.getResourceType(), // DataType
					"teiid", // Key
					"data virtualization", // Name
					version, // Version
					context.getResourceType().getDescription(), // subsystem.description
					config, null);
			config.put(new PropertySimple( "displayPreviewVDBS", Boolean.FALSE));
			detail.setPluginConfiguration(config);

			// Add to return values
			discoveredResources.add(detail);
		}
		
		log.debug("Discovered Teiid instance"); //$NON-NLS-1$
		return discoveredResources;
	}


}