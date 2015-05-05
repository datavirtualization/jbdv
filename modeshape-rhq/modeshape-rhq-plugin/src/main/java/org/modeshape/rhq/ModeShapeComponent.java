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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.modeshape.rhq.util.ToolBox;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.BaseComponent;

/**
 * The ModeShape RHQ AS 7 resource component base class.
 */
public abstract class ModeShapeComponent extends BaseComponent<ModeShapeComponent> {

    private ResourceContext<ModeShapeComponent> context;
    private String description;
    private String displayName;
    private Configuration resourceConfig;
    private String type;

    /**
     * @param componentType the component type (cannot be <code>null</code> or empty)
     * @param componentDisplayName the component display name (can be <code>null</code> or empty)
     * @param componentDescription the component description (can be <code>null</code> or empty)
     */
    protected ModeShapeComponent( final String componentType,
                                  final String componentDisplayName,
                                  final String componentDescription ) {
        ToolBox.verifyNotEmpty(componentType, "componentType");

        this.type = componentType;
        this.displayName = (((componentDisplayName == null) || componentDisplayName.isEmpty()) ? this.type : componentDisplayName);
        this.description = componentDescription;
    }

    /**
     * @return the resource context (never <code>null</code>)
     */
    public final ResourceContext<ModeShapeComponent> context() {
        return this.context;
    }

    /**
     * @return the component deployment identifier (never <code>null</code> or empty)
     */
    public final String deploymentName() {
        return context().getResourceKey();
    }

    /**
     * @return the component description (can be <code>null</code> or empty)
     */
    public final String description() {
        return this.description;
    }

    /**
     * @return the component display name (never <code>null</code> or empty)
     */
    public final String displayName() {
        return this.displayName;
    }

    /**
     * Called when the resource configuration needs to be loaded.
     * 
     * @throws Exception if there is a problem during load
     */
    protected abstract void load() throws Exception;

    /**
     * @param props the properties being loaded (can be <code>null</code> or empty)
     * @throws Exception if there is a problem loading the properties
     */
    protected final void loadProperties( final Map<?, ?> props ) throws Exception {
        if (props != null) {
            for (final Entry<?, ?> entry : props.entrySet()) {
                Object value = entry.getValue();

                if (value == null) {
                    value = PluginI18n.valueNotDefined;
                }

                if (value instanceof Collection<?>) {
                    loadProperty(entry.getKey().toString(), (Collection<?>)value);
                } else {
                    loadProperty(entry.getKey().toString(), value.toString());
                }
            }
        }
    }

    /**
     * Does nothing. Subclasses need to override if necessary.
     * 
     * @param name the multi-valued property name being loaded (cannot be <code>null</code> or empty)
     * @param valueList the values (can be <code>null</code> or empty)
     * @throws Exception if there is a problem loading the multi-valued property
     */
    protected void loadProperty( final String name,
                                 final Collection<?> valueList ) throws Exception {
        // subclass should implement
    }

    /**
     * @param name the name of the single-value property being loaded (cannot be <code>null</code> or empty)
     * @param value the value (can be <code>null</code> or empty)
     * @throws Exception if there is a problem loading the property
     */
    protected void loadProperty( final String name,
                                 final String value ) throws Exception {
        ToolBox.verifyNotEmpty(name, "name");
        this.resourceConfig.setSimpleValue(name, value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.modules.plugins.jbossas7.BaseComponent#loadResourceConfiguration()
     */
    @Override
    public final Configuration loadResourceConfiguration() throws Exception {
        resourceConfiguration().setProperties(new ArrayList<Property>()); // clear
        load();
        return this.resourceConfig;
    }

    /**
     * @return the plugin configuration used when loading properties (never <code>null</code>)
     */
    protected Configuration resourceConfiguration() {
        return this.resourceConfig;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.modules.plugins.jbossas7.BaseComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    @Override
    public void start( final ResourceContext<ModeShapeComponent> resourceContext )
        throws InvalidPluginConfigurationException, Exception {
        this.context = resourceContext;
        this.resourceConfig = this.context.getPluginConfiguration(); // need to cache this now
        super.start(this.context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.modules.plugins.jbossas7.BaseComponent#stop()
     */
    @Override
    public void stop() {
        this.context = null;
        this.description = null;
        this.displayName = null;
        this.type = null;
        super.stop();
    }

    /**
     * @return the component type (never <code>null</code>)
     */
    public final String type() {
        return this.type;
    }

}
