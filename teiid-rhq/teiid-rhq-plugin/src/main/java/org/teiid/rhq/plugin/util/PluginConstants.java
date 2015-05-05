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

/**
 * These are the Constants that used in conjunction with using the DMR
 * 
 * @since 8.3
 */
public interface PluginConstants {

	/**
	 * Log4j log category to use
	 */
	public final static String DEFAULT_LOGGER_CATEGORY = "org.rhq"; //$NON-NLS-1$

	/**
	 * These are properties required for executing operations in the Teiid
	 * module
	 */
	public interface ComponentType {

		public interface Platform {

			public final static String NAME = "Platform"; //$NON-NLS-1$
			public final static String TEIID_TYPE = "teiid"; //$NON-NLS-1$
			public final static String TEIID_SUB_TYPE = "dqp"; //$NON-NLS-1$
			public final static String TYPE = "ConnectionFactory"; //$NON-NLS-1$
			public final static String SUBTYPE = "NoTx"; //$NON-NLS-1$
			public final static String TEIID_RUNTIME_ENGINE = "RuntimeEngineDeployer"; //$NON-NLS-1$
			public final static String TEIID_ENGINE_RESOURCE_NAME = "Data Services"; //$NON-NLS-1$
			public final static String TEIID_ENGINE_RESOURCE_DESCRIPTION = "Teiid Data Service Runtime Engine"; //$NON-NLS-1$

			public static interface Operations {

				public final static String ADD_DATA_ROLE = "add-data-role";
				public final static String ASSIGN_DATASOURCE = "assign-datasource";
				public final static String GET_CACHE_TYPES = "cache-types";
				public final static String CLEAR_CACHE = "clear-cache";
				public final static String CHANGE_VDB_CONNECTION_TYPE = "change-vdb-connection-type";
				public final static String GET_QUERIES = "list-requests"; //$NON-NLS-1$					
				public final static String GET_LONGRUNNINGQUERIES = "list-long-running-requests"; //$NON-NLS-1$
				public final static String GET_ENGINE_STATISTICS = "engine-statistics"; //$NON-NLS-1$
				public final static String EXECUTE_QUERY = "execute-query"; //$NON-NLS-1$
				public final static String KILL_REQUEST = "cancel-request"; //$NON-NLS-1$
				public final static String KILL_SESSION = "terminate-session"; //$NON-NLS-1$
				public final static String KILL_TRANSACTION = "terminate-transaction"; //$NON-NLS-1$
				public final static String GET_PROPERTIES = "getProperties"; //$NON-NLS-1$
				public final static String GET_REQUESTS = "list-requests"; //$NON-NLS-1$
				public final static String GET_TRANSACTIONS = "list-transactions"; //$NON-NLS-1$
				public final static String GET_SESSIONS = "list-sessions"; //$NON-NLS-1$
				public final static String GET_BUFFER_USAGE = "userBufferSpace"; //$NON-NLS-1$
				public final static String GET_CACHE_STATS = "cache-statistics"; //$NON-NLS-1$
				public final static String DEPLOY_VDB_BY_URL = "deployVdbByUrl"; //$NON-NLS-1$
				public final static String GET_WORKERPOOL_STATSTICS = "workerpool-statistics";
				public final static String RESTART_VDB = "restart-vdb";
				public final static String WRITE_ATTRIBUTE = "write-attribute";
				public final static String LIST_VDBS = "list-vdbs";
				public final static String GET_VDB = "get-vdb";
				public final static String REMOVE_DATA_ROLE = "remove-data-role";
				public final static String LIST_REQUESTS_PER_SESSION = "list-requests-per-session";
				public final static String LIST_REQUESTS_PER_VDB = "list-requests-per-vdb";
				public final static String LIST_SESSIONS = "list-sessions";
				public final static String LIST_TRANSACTIONS = "list-transactions";
				public final static String lIST_TRANSLATORS = "list-translators";

			}

			public static interface Metrics {
				public final static String QUERY_COUNT = "queryCount"; //$NON-NLS-1$            
				public final static String SESSION_COUNT = "sessionCount"; //$NON-NLS-1$
				public final static String LONG_RUNNING_QUERIES = "longRunningQueries"; //$NON-NLS-1$     
				// TODO Do we still need this?
				public final static String BUFFER_USAGE = "userBufferSpace"; //$NON-NLS-1$
			}
		}

		public interface VDB {

			public final static String NAME = "Teiid Virtual Database"; //$NON-NLS-1$
			public final static String VERSION = "version"; //$NON-NLS-1$
			public final static String DESCRIPTION = "Teiid Virtual Database (VDB)"; //$NON-NLS-1$

			public static interface Operations {

				public final static String CLEAR_CACHE = "clearCache"; //$NON-NLS-1$	
				public final static String EXECUTE_QUERIES = "executeQuery"; //$NON-NLS-1$
				public final static String GET_LONGRUNNINGQUERIES = "getLongRunningRequests"; //$NON-NLS-1$
				public final static String KILL_REQUEST = "cancelRequest"; //$NON-NLS-1$
				public final static String KILL_SESSION = "terminateSession"; //$NON-NLS-1$
				public final static String GET_PROPERTIES = "getProperties"; //$NON-NLS-1$
				public final static String GET_REQUESTS = "list-requests-per-vdb"; //$NON-NLS-1$
				public final static String GET_SESSIONS = "getSessions"; //$NON-NLS-1$
				public final static String GET_MATVIEWS = "getMaterializedViews"; //$NON-NLS-1$
				public final static String RELOAD_MATVIEW = "reloadMaterializedView"; //$NON-NLS-1$
				public final static String CHANGE_VDB_CONN_TYPE = "change-vdb-connection-type"; //$NON-NLS-1$
				public final static String ASSIGN_DATASOURCE = "assign-datasource"; //$NON-NLS-1$

			}

			public static interface Metrics {

				public final static String STATUS = "status"; //$NON-NLS-1$ 
				public final static String QUERY_COUNT = "queryCount"; //$NON-NLS-1$            
				public final static String ERROR_COUNT = "errorCount"; //$NON-NLS-1$
				public final static String SESSION_COUNT = "sessionCount"; //$NON-NLS-1$
				public final static String LONG_RUNNING_QUERIES = "longRunningQueries"; //$NON-NLS-1$     

			}

		}

		public interface ASSystem {
		
			public static interface Operations {

				public final static String DEPLOYMENT = "deployment"; //$NON-NLS-1$
				public final static String ADD = "add"; //$NON-NLS-1$
			}
			
			public static interface Parameters {
				public static final String VDB_URL = "url"; //$NON-NLS-1$
				public static final String VDB_NAME = "vdb-name"; //$NON-NLS-1$
				public final static String CONTENT = "content"; //$NON-NLS-1$
				public final static String ENABLED = "enabled"; //$NON-NLS-1$
			}
		}

		public interface DATA_ROLE {

			public final static String NAME = "VDB Data Role"; //$NON-NLS-1$
			public final static String DESCRIPTION = "Data/Security Role for a Teiid Virtual Database (VDB)"; //$NON-NLS-1$

			public static interface Operations {

				public final static String REMOVE_AUTHENTICATED_ROLE = "remove-anyauthenticated-role";
				public final static String ADD_ANYAUTHENTICATED_ROLE = "add-anyauthenticated-role";
				public final static String REMOVE_DATA_ROLE = "remove-data-role";
				public final static String ADD_DATA_ROLE = "add-data-role";

				public static interface Parameters {
					public static final String MAPPED_ROLE_NAME = "mapped-role";
					public static final String DATA_ROLE = "data-role";
				}
			}

			public static interface Metrics {
			}

		}

		public interface Translator {

			public final static String TYPE = "teiid"; //$NON-NLS-1$
			public final static String SUBTYPE = "translator"; //$NON-NLS-1$
			public final static String NAME = "Translator"; //$NON-NLS-1$

			public static interface Operations {

			}

			public static interface Metrics {

			}

		}

		public interface Model {

			public final static String TYPE = "teiid"; //$NON-NLS-1$
			public final static String SUBTYPE = "model"; //$NON-NLS-1$
			public final static String NAME = "Model"; //$NON-NLS-1$
			public final static String DESCRIPTION = "Model used to map to a source"; //$NON-NLS-1$

		}

		public interface Connector {

			public final static String TYPE = "ConnectionFactory"; //$NON-NLS-1$
			public final static String SUBTYPE_NOTX = "NoTx"; //$NON-NLS-1$
			public final static String SUBTYPE_TX = "Tx"; //$NON-NLS-1$
			public final static String NAME = "Enterprise Connector"; //$NON-NLS-1$
			public final static String DESCRIPTION = "JBoss Enterprise Connector Binding"; //$NON-NLS-1$

			public static interface Operations {

				public final static String RESTART_CONNECTOR = "restart"; //$NON-NLS-1$            
				public final static String STOP_CONNECTOR = "stop"; //$NON-NLS-1$ 

			}

		}

		public interface Session {

			public final static String TYPE = "Runtime.Sesssion"; //$NON-NLS-1$

			public static interface Query {

				public final static String GET_SESSIONS = "getSessions"; //$NON-NLS-1$
			}
		}

		public interface Queries {

			public final static String TYPE = "Runtime.Queries"; //$NON-NLS-1$

			public static interface Query {

				public final static String GET_QUERIES = "listQueries"; //$NON-NLS-1$
			}
		}

	}

	/**
	 * Use these metric names when calling getValues() on the connection
	 * interface.
	 * 
	 * @since 1.0
	 */
	public interface Metric {
		public final static String HIGH_WATER_MARK = "highWatermark"; //$NON-NLS-1$

	}

	/**
	 * Use these operation names when calling executeOperation() on the
	 * connection interface.
	 * 
	 * @since 1.0
	 */
	public static interface Operation {
		public final static String KILL_REQUEST = "killRequest"; //$NON-NLS-1$
		public final static String GET_VDBS = "listVDBs"; //$NON-NLS-1$
		public final static String GET_PROPERTIES = "getProperties"; //$NON-NLS-1$
		public final static String GET_REQUESTS = "getRequests"; //$NON-NLS-1$
		public final static String GET_SESSIONS = "getActiveSessions"; //$NON-NLS-1$

		/**
		 * Use these value names when calling executeOperation() on the
		 * connection interface. These will correlate with parameters used in
		 * operations.
		 * 
		 * @since 1.0
		 */
		public static interface Value {
			public final static String STOP_NOW = "stopNow"; //$NON-NLS-1$  
			public final static String MAT_VIEW_QUERY = "select SchemaName, Name, TargetSchemaName, TargetName, " + //$NON-NLS-1$ 
					"Valid, LoadState, Updated, Cardinality from SYSADMIN.MATVIEWS "
					+ //$NON-NLS-1$  
					"where SchemaName != 'pg_catalog'"; //$NON-NLS-1$  
			public final static String MAT_VIEW_REFRESH = "exec SYSADMIN.refreshMatView('param1','param2');"; //$NON-NLS-1$
			public final static String WAIT_UNTIL_FINISHED = "waitUntilFinished"; //$NON-NLS-1$

			public final static String INCLUDE_SOURCE_QUERIES = "includeSourceQueries"; //$NON-NLS-1$

			public final static String LONG_RUNNING_QUERY_LIMIT = "longRunningQueryLimit"; //$NON-NLS-1$

			public final static String FIELD_LIST = "fieldList"; //$NON-NLS-1$
			public final static String TRANSACTION_ID = "transaction-id"; //$NON-NLS-1$
			public final static String REQUEST_ID = "request-id"; //$NON-NLS-1$
			public final static String SESSION_ID = "session-id"; //$NON-NLS-1$
			public final static String VDB_URL = "vdbUrl"; //$NON-NLS-1$
			public final static String VDB_DEPLOY_NAME = "vdbDeployName"; //$NON-NLS-1$
			public final static String NAME = "Name"; //$NON-NLS-1$
			public final static String VALUE = "Value"; //$NON-NLS-1$
			public final static String MATVIEW_SCHEMA = "schema"; //$NON-NLS-1$
			public final static String MATVIEW_TABLE = "table"; //$NON-NLS-1$
			public final static String INVALIDATE_MATVIEW = "invalidate"; //$NON-NLS-1$
			public final static String CACHE_TYPE = "cacheType"; //$NON-NLS-1$
			public final static String VDB_NAME = "vdb-name"; //$NON-NLS-1$
			public final static String VDB_VERSION = "vdb-version"; //$NON-NLS-1$
			public final static String SQL_QUERY = "sql-query"; //$NON-NLS-1$
			public final static String CACHETYPE = "cache-type"; //$NON-NLS-1$
			public final static String TIMEOUT_IN_MILLI = "timeout-in-milli"; //$NON-NLS-1$
			public final static String TRANSACTIONID = "xid"; //$NON-NLS-1$
			public final static String SESSION = "session"; //$NON-NLS-1$

		}

	}
}
