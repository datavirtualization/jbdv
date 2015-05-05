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

import org.modeshape.rhq.webapp.WebAppComponent;

/**
 * The ModeShape RHQ AS 7 engine component.
 */
public final class EngineComponent extends ModeShapeComponent {

    static final String DESCRIPTION = PluginI18n.engineDescription;
    static final String DISPLAY_NAME = PluginI18n.engineDisplayName;
    public static final String TYPE = "ModeShapeEngine";

    static Operation createGetRepositories() {
        return new Operation("read-children-resources", "child-type", RepositoryComponent.TYPE);
    }

    /**
     * @return an operation that will return web app components (never <code>null</code>)
     */
    public static Operation createGetWebApps() {
        return new Operation("read-children-resources", "child-type", WebAppComponent.TYPE);
    }

    /**
     * Constructs an engine component.
     */
    public EngineComponent() {
        super(TYPE, DISPLAY_NAME, DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected void load() throws Exception {
        // nothing to do
    }

}
