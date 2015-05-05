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
package org.teiid.rhq.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.teiid.rhq.plugin.VDBComponent;
import org.teiid.rhq.plugin.objects.ExecutedResult;
import org.teiid.rhq.plugin.util.DmrUtil;
import org.teiid.rhq.plugin.util.PluginConstants;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.ASSystem;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.Platform.Operations;
import org.teiid.rhq.plugin.util.PluginConstants.ComponentType.VDB;

/**
 * This class gathers metrics and executes operations
 * 
 */
public class TeiidModuleView implements PluginConstants {

	private static final Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	public static final String VDB_EXT = ".vdb"; //$NON-NLS-1$
	public static final String DYNAMIC_VDB_EXT = "-vdb.xml"; //$NON-NLS-1$

	// Cache fields
	private static final String QUERY_SERVICE_RESULT_SET_CACHE = "QUERY_SERVICE_RESULT_SET_CACHE"; //$NON-NLS-1$
	private static final String PREPARED_PLAN_CACHE = "PREPARED_PLAN_CACHE"; //$NON-NLS-1$

	// Engine statistic
	private static final String ENGINE_STATISTIC = "ENGINE_STATISTIC"; //$NON-NLS-1$

	public TeiidModuleView() {
	}

	/*
	 * Metric methods
	 */
	public Object getMetric(ASConnection connection, String componentType,
			String identifier, String metric, Map<String, Object> valueMap)
			throws Exception {
		Object resultObject = new Object();

		if (componentType.equals(PluginConstants.ComponentType.Platform.NAME)) {
			resultObject = getPlatformMetric(connection, componentType, metric,
					valueMap);
		} else if (componentType.equals(PluginConstants.ComponentType.VDB.NAME)) {
			resultObject = getVdbMetric(connection, componentType, identifier,
					metric, valueMap);
		}
		return resultObject;
	}

	private Object getPlatformMetric(ASConnection connection,
			String componentType, String metric, Map<String, Object> valueMap)
			throws Exception {

		Object resultObject = new Object();

		if (metric
				.equals(PluginConstants.ComponentType.Platform.Metrics.QUERY_COUNT)) {
			resultObject = new Double(getQueryCount(connection, null, null)
					.doubleValue());
		} else if (metric
				.equals(PluginConstants.ComponentType.Platform.Metrics.SESSION_COUNT)) {
			resultObject = new Double(getSessionCount(connection, null, null)
					.doubleValue());
		} else if (metric
				.equals(PluginConstants.ComponentType.Platform.Metrics.LONG_RUNNING_QUERIES)) {
			resultObject = new Double(getLongRunningQueryCount(connection,
					null, null).doubleValue());
		} else if (metric.startsWith(ENGINE_STATISTIC + ".")) { //$NON-NLS-1$ 
			return getEngineStatisticProperty(connection, metric);
		} else if (metric.startsWith(PREPARED_PLAN_CACHE + ".") //$NON-NLS-1$
				|| metric.startsWith(QUERY_SERVICE_RESULT_SET_CACHE + ".")) { //$NON-NLS-1$
			return getCacheProperty(connection, metric);
		}
		return resultObject;
	}

	private Object getCacheProperty(ASConnection connection, String metric) {
		int dotIndex = metric.indexOf('.');
		String cacheType = metric.substring(0, dotIndex);
		String property = metric.substring(dotIndex + 1);
		Map<String, Object> map = getCacheStats(connection, cacheType);
		return map.get(property);
	}

	private Object getEngineStatisticProperty(ASConnection connection,
			String metric) {
		int dotIndex = metric.indexOf('.');
		String property = metric.substring(dotIndex + 1);
		Map<String, Object> map = getEngineStatistic(connection);
		return map.get(property);
	}

	private Object getVdbMetric(ASConnection connection, String componentType,
			String identifier, String metric, Map<String, Object> valueMap)
			throws Exception {

		Object resultObject = new Object();
		String vdbName = (String) valueMap.get(VDB.NAME);
		String vdbVersion = (String) valueMap.get(VDB.VERSION);

		if (metric
				.equals(PluginConstants.ComponentType.VDB.Metrics.ERROR_COUNT)) {
			resultObject = getErrorCount(connection, vdbName, vdbVersion);
		} else if (metric
				.equals(PluginConstants.ComponentType.VDB.Metrics.STATUS)) {
			resultObject = getVDBStatus(connection, vdbName, vdbVersion);
		} else if (metric
				.equals(PluginConstants.ComponentType.VDB.Metrics.QUERY_COUNT)) {
			resultObject = new Double(getQueryCount(connection, vdbName,
					vdbVersion).doubleValue());
		} else if (metric
				.equals(PluginConstants.ComponentType.VDB.Metrics.SESSION_COUNT)) {
			resultObject = new Double(getSessionCount(connection, vdbName,
					vdbVersion).doubleValue());
		} else if (metric
				.equals(PluginConstants.ComponentType.VDB.Metrics.LONG_RUNNING_QUERIES)) {
			resultObject = new Double(getLongRunningQueryCount(connection,
					vdbName, vdbVersion).doubleValue());
		}
		return resultObject;
	}

	/*
	 * Operation methods
	 */

	public void executeOperation(ASConnection connection,
			ExecutedResult operationResult, final Map<String, Object> valueMap)
			throws Exception {

		if (operationResult.getComponentType().equals(
				PluginConstants.ComponentType.Platform.NAME)) {
			executePlatformOperation(connection, operationResult,
					operationResult.getOperationName(), valueMap);
		} else if (operationResult.getComponentType().equals(
				PluginConstants.ComponentType.VDB.NAME)) {
			executeVdbOperation(connection, operationResult,
					operationResult.getOperationName(), valueMap);
		}
	}

	private void executePlatformOperation(ASConnection connection,
			ExecutedResult operationResult, final String operationName,
			final Map<String, Object> valueMap) throws Exception {
		List<Map<String, Object>> resultObject = null;
		Result result = null;
		Address address = DmrUtil.getTeiidAddress();

		if (operationName.equals(Platform.Operations.GET_LONGRUNNINGQUERIES)) {
			resultObject = getLongRunningQueries(connection);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(Platform.Operations.GET_SESSIONS)) {
			resultObject = getSessions(connection, null, null);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(Platform.Operations.GET_REQUESTS)) {
			resultObject = getRequests(connection, null, null);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(Platform.Operations.GET_TRANSACTIONS)) {
			resultObject = getTransactions(connection);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(Platform.Operations.KILL_TRANSACTION)) {
			String transactionID = (String) valueMap
					.get(Operation.Value.TRANSACTION_ID);
			Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
			additionalProperties.put(Operation.Value.TRANSACTIONID,
					transactionID);
			operationResult.setContent((Collection<?>) executeOperation(
					connection, operationName, address, additionalProperties));
		} else if (operationName.equals(Platform.Operations.KILL_SESSION)) {
			String sessionID = (String) valueMap
					.get(Operation.Value.SESSION_ID);
			Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
			additionalProperties.put(Operation.Value.SESSION, sessionID);
			operationResult.setContent((Collection<?>) executeOperation(
					connection, operationName, address, additionalProperties));
		} else if (operationName.equals(Platform.Operations.KILL_REQUEST)) {
			Long requestID = (Long) valueMap.get(Operation.Value.REQUEST_ID);
			String sessionID = (String) valueMap
					.get(Operation.Value.SESSION_ID);
			Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
			additionalProperties.put(Operation.Value.REQUEST_ID, requestID);
			additionalProperties.put(Operation.Value.SESSION, sessionID);
			operationResult.setContent((Collection<?>) executeOperation(
					connection, operationName, address, additionalProperties));
		} else if (operationName.equals(Platform.Operations.DEPLOY_VDB_BY_URL)) {
			String vdbUrl = (String) valueMap.get(Operation.Value.VDB_URL);
			String vdbName = (String) valueMap.get(Operation.Value.VDB_NAME);
			HashMap<String, Object> resultValue = new HashMap();
			HashMap<String, Object> resultMessage = new HashMap();

			try {
				result = executeDeployment(connection, vdbUrl, vdbName);
				// If no exceptions, we assume the deployment worked
				if (result.isSuccess()) {
					operationResult.setContent("VDB deployed successfully!"); //$NON-NLS-1$
				} else {
					operationResult.setContent("failure: " + result.getFailureDescription()); //$NON-NLS-1$
				}
			} catch (Exception e) {
				final String msg = "Exception executing operation: " + Platform.Operations.DEPLOY_VDB_BY_URL; //$NON-NLS-1$
				LOG.error(msg, e);
				throw new RuntimeException(e);
			}
		}
	}

	private void executeVdbOperation(ASConnection connection,
			ExecutedResult operationResult, final String operationName,
			final Map<String, Object> valueMap) throws Exception {
		List<Map<String, Object>> resultObject = null;
		String vdbName = (String) valueMap
				.get(PluginConstants.ComponentType.VDB.NAME);
		String vdbVersion = (String) valueMap
				.get(PluginConstants.ComponentType.VDB.VERSION);

		if (operationName.equals(VDB.Operations.GET_SESSIONS)) {
			resultObject = getSessions(connection, vdbName, vdbVersion);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(VDB.Operations.GET_REQUESTS)) {
			resultObject = getRequests(connection, vdbName, vdbVersion);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(VDB.Operations.GET_MATVIEWS)) {
			resultObject = executeMaterializedViewQuery(connection, vdbName,
					vdbVersion);
			operationResult.setContent(resultObject);
		} else if (operationName.equals(VDB.Operations.CLEAR_CACHE)) {

			Result result = null;
			try {
				result = executeClearCache(connection, vdbName,
						Integer.parseInt(vdbVersion),
						(String) valueMap.get(Operation.Value.CACHE_TYPE));

			} catch (Exception e) {
				// Some failure during Clear Cache. Set message here since it
				// has already been logged.
				operationResult.setContent("failure - see log for details"); //$NON-NLS-1$
			}

			// If no exceptions, we assume the clear cache worked
			if (result.isSuccess()) {
				operationResult.setContent("cache successfully cleared!"); //$NON-NLS-1$
			} else {
				operationResult.setContent("failure - see log for details"); //$NON-NLS-1$
			}

		} else if (operationName.equals(VDB.Operations.RELOAD_MATVIEW)) {
			Result result = reloadMaterializedView(connection, vdbName,
					Integer.parseInt(vdbVersion),
					(String) valueMap.get(Operation.Value.MATVIEW_SCHEMA),
					(String) valueMap.get(Operation.Value.MATVIEW_TABLE),
					(Boolean) valueMap.get(Operation.Value.INVALIDATE_MATVIEW));

			if (result == null || !result.isSuccess()) {
				operationResult.setContent("failure - see log for details"); //$NON-NLS-1$
			} else {
				operationResult.setContent("data successfully refreshed!"); //$NON-NLS-1$
			}
		}

	}

	/*
	 * Helper methods
	 */

	protected Result executeClearCache(ASConnection connection, String vdbName,
			int vdbVersion, String cacheType) throws Exception {

		Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
		additionalProperties.put(Operation.Value.CACHETYPE, cacheType);
		additionalProperties.put(Operation.Value.VDB_NAME, vdbName);
		additionalProperties.put(Operation.Value.VDB_VERSION, vdbVersion);
		Result result = executeOperation(connection,
				Platform.Operations.CLEAR_CACHE, DmrUtil.getTeiidAddress(),
				additionalProperties);

		return result;
	}

	protected Result executeDeployment(ASConnection connection, String url,
			String vbdName) throws Exception {
		
		// This will deploy a VDB via URL using the following format 
		// deployment=Subscriptions-vdb.vdb:add(content=[{url=file:///pathtovdb/Subscriptions-vdb.vdb}],enabled=true)
		
		org.rhq.modules.plugins.jbossas7.json.Operation step1 = new org.rhq.modules.plugins.jbossas7.json.Operation(
				ASSystem.Operations.ADD, ASSystem.Operations.DEPLOYMENT, vbdName); //$NON-NLS-1$ //$NON-NLS-2$
		List<Object> content = new ArrayList<Object>(1);
		Map<String, Object> contentValues = new HashMap<String, Object>();
		contentValues.put(ASSystem.Parameters.VDB_URL, url); //$NON-NLS-1$
		content.add(contentValues);
		step1.addAdditionalProperty(ASSystem.Parameters.CONTENT, content); //$NON-NLS-1$
		step1.addAdditionalProperty(ASSystem.Parameters.ENABLED, true); //$NON-NLS-1$

		org.rhq.modules.plugins.jbossas7.json.CompositeOperation cop = new org.rhq.modules.plugins.jbossas7.json.CompositeOperation();
		cop.addStep(step1);
		Result result = connection.execute(cop);
		
		return result;
	}

	protected List<Map<String, Object>> executeMaterializedViewQuery(
			ASConnection connection, String vdbName, String vdbVersion) {

		Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
		additionalProperties.put(Operation.Value.VDB_NAME, vdbName);
		additionalProperties.put(Operation.Value.VDB_VERSION, vdbVersion);
		additionalProperties.put(Operation.Value.SQL_QUERY,
				Operation.Value.MAT_VIEW_QUERY);
		additionalProperties.put(Operation.Value.TIMEOUT_IN_MILLI, "9999999");

		Result result = executeOperation(connection,
				Platform.Operations.EXECUTE_QUERY, DmrUtil.getTeiidAddress(),
				additionalProperties);

		return (List<Map<String, Object>>) result.getResult();

	}

	protected Result reloadMaterializedView(ASConnection connection,
			String vdbName, int vdbVersion, String schema, String table,
			Boolean invalidate) {

		Result result = null;
		String matView = schema + "." + table; //$NON-NLS-1$
		String query = PluginConstants.Operation.Value.MAT_VIEW_REFRESH;
		query = query.replace("param1", matView); //$NON-NLS-1$
		query = query.replace("param2", invalidate.toString()); //$NON-NLS-1$
		Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
		additionalProperties.put(Operation.Value.VDB_NAME, vdbName);
		additionalProperties.put(Operation.Value.VDB_VERSION, vdbVersion);
		additionalProperties.put(Operation.Value.SQL_QUERY, query);
		additionalProperties.put(Operation.Value.TIMEOUT_IN_MILLI, "9999999");

		result = executeOperation(connection, Operations.EXECUTE_QUERY,
				DmrUtil.getTeiidAddress(), additionalProperties);

		return result;

	}

	protected List<Map<String, Object>> getTransactions(ASConnection connection) {

		Address address = DmrUtil.getTeiidAddress();
		Result result = executeOperation(connection,
				Platform.Operations.GET_TRANSACTIONS, address, null);

		return (List<Map<String, Object>>) result.getResult();

	}

	public static String getVDBStatus(ASConnection connection, String vdbName,
			String vdbVersion) {

		Map<String, ?> vdbMap = VDBComponent.getVdbMap(connection, vdbName,
				vdbVersion);
		return (String) vdbMap.get(VDBComponent.STATUS);

	}

	//
	// /**
	// * @param mc
	// * @return
	// */
	// private static ManagedComponent getBufferService(ASConnection connection,
	// ManagedComponent mc) {
	// // try {
	// // mc = ProfileServiceUtil.getBufferService(connection);
	// // } catch (NamingException e) {
	////			final String msg = "NamingException getting the SessionService"; //$NON-NLS-1$
	// // LOG.error(msg, e);
	// // } catch (Exception e1) {
	////			final String msg = "Exception getting the SessionService"; //$NON-NLS-1$
	// // LOG.error(msg, e1);
	// // }
	// return mc;
	// }

	private Integer getQueryCount(ASConnection connection, String vdbName,
			String vdbVersion) throws Exception {

		Address address = DmrUtil.getTeiidAddress();
		Result result;
		if (vdbName!=null){
			result = executeOperation(connection,
					Platform.Operations.GET_REQUESTS, address, null);
		}else{
			Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
			additionalProperties.put(Operation.Value.VDB_NAME, vdbName);
			additionalProperties.put(Operation.Value.VDB_VERSION, vdbVersion);
			result = executeOperation(connection,
					VDB.Operations.GET_REQUESTS, address, additionalProperties);
		}

		return getArraySize(result);

	}

	private Integer getSessionCount(ASConnection connection, String vdbName,
			String vdbVersion) throws Exception {

		Address address = DmrUtil.getTeiidAddress();
		Result result = executeOperation(connection,
				Platform.Operations.GET_SESSIONS, address, null);
		int count = 0;

		// If this is at the VDB level, look for instances of the VDB
		count = getCountForVdb(vdbName, result, count);

		return vdbName != null ? count : getArraySize(result);
	}

	private List<Map<String, Object>> getSessions(ASConnection connection,
			String vdbName, String vdbVersion) throws Exception {

		Address address = DmrUtil.getTeiidAddress();
		Result result = executeOperation(connection,
				Platform.Operations.GET_SESSIONS, address, null);
		List<Map<String, Object>> vdbList = new ArrayList<Map<String, Object>>();

		getVdbList(vdbName, result, vdbList);

		return vdbName != null ? vdbList : (List<Map<String, Object>>) result
				.getResult();
	}

	protected List<Map<String, Object>> getRequests(ASConnection connection,
			String vdbName, String vdbVersion) {

		Address address = DmrUtil.getTeiidAddress();
		Result result;
		if (vdbName==null){
			result = executeOperation(connection,
					Platform.Operations.GET_REQUESTS, address, null);
		}else{
			Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();
			additionalProperties.put(Operation.Value.VDB_NAME, vdbName);
			additionalProperties.put(Operation.Value.VDB_VERSION, vdbVersion);
			result = executeOperation(connection,
					VDB.Operations.GET_REQUESTS, address, additionalProperties);
		}

		return (List<Map<String, Object>>) result.getResult();

	}

	private void getVdbList(String vdbName, Result result,
			List<Map<String, Object>> vdbList) {
		if (vdbName != null) {
			if (result.getResult() != null) {
				List<Map<String, Object>> list = (List<Map<String, Object>>) result
						.getResult();
				for (Map<String, Object> value : list) {
					if (value.get(VDBComponent.VDBNAME).equals(vdbName)) {
						vdbList.add(value);
					}
				}
			}
		}
	}

	private int getCountForVdb(String vdbName, Result result, int count) {
		if (vdbName != null) {
			if (result.getResult() != null) {
				List<Map<String, Object>> list = (List<Map<String, Object>>) result
						.getResult();
				for (Map<String, Object> value : list) {
					if (value.get(VDBComponent.VDBNAME).equals(vdbName)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	private Integer getArraySize(Result result) throws Exception {
		if (result.isSuccess()) {
		} else {
			throw new Exception(result.getFailureDescription());
		}

		return result == null ||  result.getResult() == null ? 0 : ((ArrayList<Map<String, Object>>) result
				.getResult()).size();
	}

	private Integer getErrorCount(ASConnection connection, String vdbName,
			String vdbVersion) {

		int count = 0;

		Map<String, ?> vdbMap = VDBComponent.getVdbMap(connection, vdbName,
				vdbVersion);

		ArrayList<Map<String, Object>> modelList = (ArrayList<Map<String, Object>>) vdbMap
				.get(VDBComponent.MODELS);

		for (Map<String, Object> modelMap : modelList) {

			// Get any model errors/warnings and increment count
			ArrayList<Map<String, String>> errors = (ArrayList<Map<String, String>>) modelMap
					.get(VDBComponent.VALIDITY_ERRORS);

			if (errors != null) {
				count += errors.size();
			}
		}

		return count;
	}

	protected Map<String, Object> getCacheStats(ASConnection connection,
			String type) {

		Map<String, Object> additionalProperties = new HashMap<String, Object>();
		additionalProperties.put(Operation.Value.CACHETYPE, type);
		Result result = executeOperation(connection,
				Platform.Operations.GET_CACHE_STATS, DmrUtil.getTeiidAddress(),
				additionalProperties);

		return (Map<String, Object>) result.getResult();

	}

	protected Integer getLongRunningQueryCount(ASConnection connection,
			String vdbName, String vdbVersion) throws Exception {

		Address address = DmrUtil.getTeiidAddress();
		Result result = executeOperation(connection,
				Platform.Operations.GET_LONGRUNNINGQUERIES, address, null);

		return getArraySize(result);
	}

	protected List<Map<String, Object>> getLongRunningQueries(
			ASConnection connection) throws Exception {

		Address address = DmrUtil.getTeiidAddress();
		Result result = executeOperation(connection,
				Platform.Operations.GET_LONGRUNNINGQUERIES, address, null);

		return (List<Map<String, Object>>) result.getResult();
	}

	public static Result executeOperation(ASConnection connection,
			String operationName, Address operationAddress,
			Map<String, Object> additionalProperties) {
		org.rhq.modules.plugins.jbossas7.json.Operation op = new org.rhq.modules.plugins.jbossas7.json.Operation(
				operationName, operationAddress);
		if (additionalProperties != null) {
			op.setAdditionalProperties(additionalProperties);
		}

		Result result = connection.execute(op);
		return result;
	}

	protected Map<String, Object> getEngineStatistic(ASConnection connection) {

		Result result = executeOperation(connection,
				Platform.Operations.GET_ENGINE_STATISTICS,
				DmrUtil.getTeiidAddress(), null);

		return (Map<String, Object>) result.getResult();
	}

}