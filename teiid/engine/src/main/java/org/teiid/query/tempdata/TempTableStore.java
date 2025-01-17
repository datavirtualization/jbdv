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

package org.teiid.query.tempdata;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.teiid.api.exception.query.ExpressionEvaluationException;
import org.teiid.api.exception.query.QueryProcessingException;
import org.teiid.common.buffer.BlockedException;
import org.teiid.common.buffer.BufferManager;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.dqp.service.TransactionContext;
import org.teiid.dqp.service.TransactionContext.Scope;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.metadata.FunctionMethod.Determinism;
import org.teiid.metadata.Table;
import org.teiid.query.QueryPlugin;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TempMetadataID;
import org.teiid.query.metadata.TempMetadataStore;
import org.teiid.query.processor.BatchIterator;
import org.teiid.query.processor.QueryProcessor;
import org.teiid.query.resolver.command.TempTableResolver;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.Create;
import org.teiid.query.sql.lang.Insert;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.util.CommandContext;

/**
 * TempTableStores are transactional, but do not act as full resource manager.
 * This means we are effectively 1PC and don't allow any heuristic exceptions
 * on commit.
 *  
 * Table state snapshoting and a {@link Synchronization} are used to
 * perform the appropriate commit/rollback actions.
 * 
 * Full row level MVCC would be a good next step as it would remove the
 * cost of state cloning and would allow for concurrent read/write transactions. 
 */
public class TempTableStore {
	
    public interface TransactionCallback {
    	void commit();
    	void rollback();
    }
    
    public enum TransactionMode {
    	ISOLATE_READS, //for matviews that have atomic updates
    	ISOLATE_WRITES, //for session/procedure stores that need rollback support - this is effectively READ_UNCOMMITTED
    	NONE
    }
    
    public static class TableProcessor {
		QueryProcessor queryProcessor;
    	List<ElementSymbol> columns;
    	BatchIterator iterator;

    	public TableProcessor(QueryProcessor queryProcessor,
				List<ElementSymbol> columns) {
			this.queryProcessor = queryProcessor;
			this.columns = columns;
			this.iterator = new BatchIterator(queryProcessor);
		}
    	
    	public QueryProcessor getQueryProcessor() {
			return queryProcessor;
		}
    }
    
    public class TempTableSynchronization implements Synchronization {
    	
    	private String id;
    	Set<Long> existingTables = new HashSet<Long>();
    	ConcurrentHashMap<String, TempTable> tables = new ConcurrentHashMap<String, TempTable>();
        private List<TransactionCallback> callbacks = new LinkedList<TransactionCallback>();
    	        
        private boolean completed;
        
        public TempTableSynchronization(final String id) {
        	this.id = id;
        	for (TempTable tempTable : tempTables.values()) {
        		existingTables.add(tempTable.getId());
        	}
        	if (transactionMode == TransactionMode.ISOLATE_WRITES) {
        		addCallback(new TransactionCallback() {
        	        private Map<String, TempMetadataID> clonedMetadata = new ConcurrentHashMap<String, TempMetadataID>(tempMetadataStore.getData());
        	        private Map<String, TempTable> clonedTables = new ConcurrentHashMap<String, TempTable>(tempTables);
					
					@Override
					public void rollback() {
						LogManager.logDetail(LogConstants.CTX_DQP, "Rolling back txn", id, "restoring", clonedTables.keySet(), "using rollback tables", tables); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						//remove any tables created in the scope of this txn
						tempTables.values().removeAll(clonedTables.values());
						for (TempTable table : tempTables.values()) {
							table.remove();
						}
						
						//restore the state
						tempMetadataStore.getData().clear();
						tempMetadataStore.getData().putAll(clonedMetadata);
						tempTables.clear();
						tempTables.putAll(clonedTables);
						
						//overlay the rollback tables
						tempTables.putAll(tables);
					}
					
					@Override
					public void commit() {
						//remove any original tables that were removed in this txn
						clonedTables.values().removeAll(tempTables.values());
						for (TempTable table : clonedTables.values()) {
							table.remove();
						}
					}
				});
        	}
		}
    	
    	@Override
    	public synchronized void afterCompletion(int status) {
    		completed = true;
    		synchronizations.remove(id);
    		if (transactionMode == TransactionMode.ISOLATE_READS) {
				for (TempTable table : tables.values()) {
					table.getActive().decrementAndGet();
				}
    		} else {
    			HashSet<TempTable> current = new HashSet<TempTable>(tempTables.values());
    			current.retainAll(tables.values());
				for (TempTable table : current) {
					table.getActive().set(0);
					table.getTree().clearClonedFlags();
				}
    		}
			for (TransactionCallback callback : callbacks) {
        		if (status == Status.STATUS_COMMITTED) {
        			callback.commit();
        		} else {
        			callback.rollback();
        		}
			}
    		callbacks.clear();
    	}
    	
    	public boolean isCompleted() {
			return completed;
		}
    	
    	@Override
    	public void beforeCompletion() {
    		
    	}
    	
    	public synchronized boolean addCallback(TransactionCallback callback) {
    		if (!completed) {
    			callbacks.add(0, callback);
    		}
    		return !completed;
    	}
    }
    
    private Map<String, TempTableSynchronization> synchronizations = new ConcurrentHashMap<String, TempTableSynchronization>();
    private TransactionMode transactionMode = TransactionMode.NONE;
	
    private TempMetadataStore tempMetadataStore = new TempMetadataStore(new ConcurrentSkipListMap<String, TempMetadataID>(String.CASE_INSENSITIVE_ORDER));
    private Map<String, TempTable> tempTables = new ConcurrentSkipListMap<String, TempTable>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Table> foreignTempTables = new ConcurrentSkipListMap<String, Table>(String.CASE_INSENSITIVE_ORDER);
    private String sessionID;
    private TempTableStore parentTempTableStore;
    
    private HashMap<String, TableProcessor> processors;
    
    public TempTableStore(String sessionID, TransactionMode transactionMode) {
        this.sessionID = sessionID;
        this.transactionMode = transactionMode;
    }
    
    public void setParentTempTableStore(TempTableStore parentTempTableStore) {
		this.parentTempTableStore = parentTempTableStore;
	}
    
    public boolean hasTempTable(String tempTableName) {
    	return tempTables.containsKey(tempTableName) || foreignTempTables.containsKey(tempTableName);
    }
    
    public void setProcessors(HashMap<String, TableProcessor> plans) {
		this.processors = plans;
	}
    
    void addForeignTempTable(final String tempTableName, Create create) {
    	TempMetadataID id = tempMetadataStore.getTempGroupID(tempTableName);
    	if (id == null) {
    		id = tempMetadataStore.addTempGroup(tempTableName, create.getColumnSymbols(), false, true);
    		id.setOriginalMetadataID(create.getTableMetadata());
    		id.getTableData().setModel(create.getTableMetadata().getParent());
    	}
    	this.foreignTempTables.put(tempTableName, create.getTableMetadata());
    }

    /**
     * 
     * @param tempTableName
     * @param create
     * @param buffer
     * @param add
     * @param context may be null for mat views
     * @return
     * @throws TeiidProcessingException
     */
    TempTable addTempTable(final String tempTableName, Create create, BufferManager buffer, boolean add, CommandContext context) throws TeiidProcessingException {
    	List<ElementSymbol> columns = create.getColumnSymbols();
    	TempMetadataID id = tempMetadataStore.getTempGroupID(tempTableName);
    	getSynchronization(context);
    	if (id == null) {
	        //add metadata
	    	id = tempMetadataStore.addTempGroup(tempTableName, columns, false, true);
	        TempTableResolver.addAdditionalMetadata(create, id);
    	}
    	columns = new ArrayList<ElementSymbol>(create.getColumnSymbols());
        if (!create.getPrimaryKey().isEmpty()) {
    		//reorder the columns to put the key in front
    		List<ElementSymbol> primaryKey = create.getPrimaryKey();
    		columns.removeAll(primaryKey);
    		columns.addAll(0, primaryKey);
    	}
        final TempTable tempTable = new TempTable(id, buffer, columns, create.getPrimaryKey().size(), sessionID);
        if (add) {
        	tempTables.put(tempTableName, tempTable);
        }
        return tempTable;
    }
    
    public void removeTempTableByName(final String tempTableName, CommandContext context) throws TeiidProcessingException {
    	TempTableSynchronization synch = getSynchronization(context);
    	tempMetadataStore.removeTempGroup(tempTableName);
        final TempTable table = this.tempTables.remove(tempTableName);
        if (table == null) {
        	foreignTempTables.remove(tempTableName);
        	return;
        }
		if (transactionMode != TransactionMode.ISOLATE_WRITES || synch == null || !synch.existingTables.contains(table.getId())) {
			table.remove();
    	}
    }

	private TempTableSynchronization getSynchronization(CommandContext context) throws TeiidProcessingException {
		TempTableSynchronization synch = null;
		if (context == null || transactionMode == TransactionMode.NONE) {
			return null;
		}
		TransactionContext tc = context.getTransactionContext();
		if (tc == null || tc.getTransactionType() == Scope.NONE) {
			return null;
		}
		String transactionId = tc.getTransactionId();
		synch = synchronizations.get(transactionId);
		if (synch == null) {
			boolean success = false;
			try {
				synch = new TempTableSynchronization(transactionId);
				synchronizations.put(transactionId, synch);
				tc.getTransaction().registerSynchronization(synch);
				success = true;
			} catch (RollbackException e) {
				 throw new TeiidProcessingException(QueryPlugin.Event.TEIID30223, e);
			} catch (SystemException e) {
				 throw new TeiidProcessingException(QueryPlugin.Event.TEIID30224, e);
			} finally {
				if (!success) {
					synchronizations.remove(transactionId);
				}
			}
		}
		return synch;
	}

    public TempMetadataStore getMetadataStore() {
        return tempMetadataStore;
    }
            
    public void removeTempTables() throws TeiidComponentException {
        for (String name : tempTables.keySet()) {
            try {
				removeTempTableByName(name, null);
			} catch (TeiidProcessingException e) {
				 throw new TeiidComponentException(QueryPlugin.Event.TEIID30225, e);
			}
        }
        for (String name : foreignTempTables.keySet()) {
            try {
				removeTempTableByName(name, null);
			} catch (TeiidProcessingException e) {
				 throw new TeiidComponentException(QueryPlugin.Event.TEIID30225, e);
			}
        }
    }
    
    public void setUpdatable(String name, boolean updatable) {
    	TempTable table = tempTables.get(name);
    	if (table != null) {
    		table.setUpdatable(updatable);
    	}
    }
    
    TempTable getTempTable(String tempTableID) {
        return this.tempTables.get(tempTableID);
    }
    
    public HashMap<String, TableProcessor> getProcessors() {
		return processors;
	}
    
    TempTable getOrCreateTempTable(String tempTableID, Command command, BufferManager buffer, boolean delegate, boolean forUpdate, CommandContext context, GroupSymbol group) throws TeiidProcessingException, BlockedException, TeiidComponentException{
		if (!(group.getMetadataID() instanceof TempMetadataID)) {
			//TODO: use a proper metadata
			TempTableStore tts = context.getSessionTempTableStore();
			context.setDeterminismLevel(Determinism.SESSION_DETERMINISTIC);
			if (tts.getTempTable(tempTableID) == null) {
				//implicitly create global (session scoped) temp table
				LogManager.logDetail(LogConstants.CTX_DQP, "binding global temp table to session", group); //$NON-NLS-1$
				QueryMetadataInterface metadata = context.getMetadata();
				Create create = GlobalTableStoreImpl.getCreateCommand(group, false, metadata);
				tts.addTempTable(tempTableID, create, buffer, true, context);
			}
			return getTempTable(tempTableID, command, buffer, delegate, forUpdate, context);
		}
    	TempTable tempTable = getTempTable(tempTableID, command, buffer, delegate, forUpdate, context);
    	if (tempTable != null) {
    		if (processors != null) {
    			TableProcessor withProcessor = processors.get(tempTableID);
    			if (withProcessor != null) {
    				buildWithTable(tempTableID, withProcessor, tempTable);
    			}
    		}
    		return tempTable;
    	}
        //allow implicit temp group definition
        List<ElementSymbol> columns = null;
        if (command instanceof Insert) {
            Insert insert = (Insert)command;
            if(group.isImplicitTempGroupSymbol()) {
                columns = insert.getVariables();
            }
        }
        if (columns == null) {
        	if (processors != null) {
        		TableProcessor withProcessor = processors.get(tempTableID);
        		if (withProcessor != null) {
        	        LogManager.logDetail(LogConstants.CTX_DQP, "Creating temporary table for with clause", tempTableID); //$NON-NLS-1$
        	        Create create = new Create();
        	        create.setTable(new GroupSymbol(tempTableID));
        	        create.setElementSymbolsAsColumns(withProcessor.columns);
        			tempTable = addTempTable(tempTableID, create, buffer, true, context);
    				buildWithTable(tempTableID, withProcessor, tempTable);
    				return tempTable;
        		}
        	}
        	 throw new QueryProcessingException(QueryPlugin.Event.TEIID30226, QueryPlugin.Util.gs(QueryPlugin.Event.TEIID30226, tempTableID));
        }
        LogManager.logDetail(LogConstants.CTX_DQP, "Creating temporary table", tempTableID); //$NON-NLS-1$
        Create create = new Create();
        create.setTable(new GroupSymbol(tempTableID));
        create.setElementSymbolsAsColumns(columns);
        return addTempTable(tempTableID, create, buffer, true, context);       
    }

	private void buildWithTable(String tempTableID,
			TableProcessor withProcessor, TempTable tempTable)
			throws TeiidComponentException, ExpressionEvaluationException,
			TeiidProcessingException {
		tempTable.insert(withProcessor.iterator, withProcessor.columns, false, null);
		tempTable.setUpdatable(false);
		processors.remove(tempTableID);
	}

	private TempTable getTempTable(String tempTableID, Command command,
			BufferManager buffer, boolean delegate, boolean forUpdate, CommandContext context)
			throws TeiidProcessingException {
		final TempTable tempTable = tempTables.get(tempTableID);
        if(tempTable != null) {
        	//isolate if needed
    		if (forUpdate) {
    			if (transactionMode == TransactionMode.ISOLATE_WRITES) {
    				TransactionContext tc = context.getTransactionContext();
        			if (tc != null) {
        				TempTableSynchronization synch = getSynchronization(context);
        				if (synch != null && synch.existingTables.contains(tempTable.getId())) {
        					TempTable result = synch.tables.get(tempTableID);
        					if (result == null) {
        						synchronized (synch) {
									if (synch.isCompleted()) {
										throw new AssertionError("Expected active transaction"); //$NON-NLS-1$
									}
	        						if (!tempTable.getActive().compareAndSet(0, 1)) {
	        	    					 throw new TeiidProcessingException(QueryPlugin.Event.TEIID30227, QueryPlugin.Util.gs(QueryPlugin.Event.TEIID30227, tempTableID));
	        	    				}
	        						synch.tables.put(tempTableID, tempTable.clone());
								}
        					}
        					return tempTable;
        				}
        			} else if (tempTable.getActive().get() != 0) {
        				 throw new TeiidProcessingException(QueryPlugin.Event.TEIID30227, QueryPlugin.Util.gs(QueryPlugin.Event.TEIID30227, tempTableID));
    				}
    			}
    		} else if (transactionMode == TransactionMode.ISOLATE_READS) {
    			TransactionContext tc = context.getTransactionContext();
    			if (tc != null && tc.getIsolationLevel() > Connection.TRANSACTION_READ_COMMITTED) {
    				TempTableSynchronization synch = getSynchronization(context);
    				if (synch != null) {
    					TempTable result = synch.tables.get(tempTableID);
    					if (result == null) {
    						result = tempTable;
    						synchronized (synch) {
								if (!synch.isCompleted()) {
									synch.tables.put(tempTableID, tempTable);
									result.getActive().getAndIncrement();
								}
							}
    					}
    					return result;
    				}
    			}
    		}
            return tempTable;
        }
        if(delegate && this.parentTempTableStore != null){
    		return this.parentTempTableStore.getTempTable(tempTableID, command, buffer, delegate, forUpdate, context);
        }
        return null;
	}
    
    public Set<String> getAllTempTables() {
        Set<String> result = new HashSet<String>(this.tempTables.keySet());
        result.addAll(this.foreignTempTables.keySet());
        return result;
    }
    
    Map<String, TempTable> getTempTables() {
		return tempTables;
	}
    
}
