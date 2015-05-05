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
package org.modeshape.rhq.webapp;

import java.util.Map;
import org.modeshape.rhq.ModeShapeComponent;
import org.modeshape.rhq.ModeShapePlugin;
import org.modeshape.rhq.Operation;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.util.I18n;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ModeShape web application component.
 */
public final class WebAppComponent extends ModeShapeComponent {

    /**
     * The component type of a web application.
     */
    public static final String TYPE = "webapp";

    /**
     * Constructs a web application component.
     */
    public WebAppComponent() {
        super(TYPE, PluginI18n.webAppDisplayName, PluginI18n.webAppDescription);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected void load() throws Exception {
        final Address addr = ModeShapePlugin.createModeShapeAddress();
        addr.add(TYPE, deploymentName());
        final Result result = getASConnection().execute(Operation.Util.createReadResourceOperation(addr, true));
        boolean success = true;

        if (result.isSuccess()) {
            final Object tempResult = result.getResult();

            if ((tempResult != null) && (tempResult instanceof Map<?, ?>)) {
                final Map<?, ?> propMap = (Map<?, ?>)tempResult;
                ModeShapePlugin.LOG.debug("Loading " + propMap.size() + " properties for web app '" + deploymentName() + '\'');
                loadProperties(propMap);
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        if (!success) {
            throw new Exception(I18n.bind(PluginI18n.errorLoadingWebAppProperties, deploymentName()));
        }
    }

}
