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

package org.teiid.rhq.plugin.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.modules.plugins.jbossas7.json.Address;

public final class DmrUtil {
	

	protected final static Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY); 
	public static String TEIID_SUBSYSTEM = "teiid";
	public static String TRANSPORT = "transport";
	public static String ODBC = "odbc";
	public static String JDBC = "jdbc";
	
	public static Address getTeiidAddress() {
		Address addr = new Address(TEIID_SUBSYSTEM);
		addr.add("subsystem", TEIID_SUBSYSTEM);
		return addr;
	}
	
	public static Address getJDBCTransportAddress() {
		Address jdbc = new Address(getTeiidAddress());
		jdbc.add(TRANSPORT, JDBC);
		return jdbc;
	}
	
	public static Address getODBCTransportAddress() {
		Address odbc = new Address(getTeiidAddress());
		odbc.add(TRANSPORT, ODBC);
		return odbc;
	}

}
