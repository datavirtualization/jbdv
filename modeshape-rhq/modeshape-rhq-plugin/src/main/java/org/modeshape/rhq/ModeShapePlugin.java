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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.modules.plugins.jbossas7.json.Address;

/**
 * Common utilities and information used by the plugin components
 */
public class ModeShapePlugin {

    public static final Log LOG = LogFactory.getLog(ModeShapePlugin.class);

    private static final String MODESHAPE_SUBSYSTEM = "modeshape";

    public static final String VERSION = "3.4.0"; // TODO need get get this dynamically (maybe from an engine attribute)

    public static Address createModeShapeAddress() {
        final Address addr = new Address(MODESHAPE_SUBSYSTEM);
        addr.add("subsystem", MODESHAPE_SUBSYSTEM);
        return addr;
    }

    /**
     * Don't allow construction outside this class.
     */
    private ModeShapePlugin() {
        // nothing to do
    }

}
