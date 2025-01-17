package org.teiid.query.processor;

import static org.junit.Assert.*;
import static org.teiid.query.processor.TestProcessor.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.MetadataStore;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.optimizer.TestOptimizer;
import org.teiid.query.optimizer.TestOptimizer.ComparisonMode;
import org.teiid.query.optimizer.capabilities.BasicSourceCapabilities;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.FakeCapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.SourceCapabilities.Capability;
import org.teiid.query.sql.lang.BatchedUpdateCommand;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.Insert;
import org.teiid.query.sql.symbol.Constant;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.translator.SourceSystemFunctions;

@SuppressWarnings("nls")
public class TestInsertProcessing {
	
    @Test public void testSelectIntoWithTypeConversion() {
    	MetadataStore metadataStore = new MetadataStore();
        Schema pm1 = RealMetadataFactory.createPhysicalModel("pm1", metadataStore); //$NON-NLS-1$
        Table pm1g1 = RealMetadataFactory.createPhysicalGroup("g1", pm1); //$NON-NLS-1$
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.BATCHED_UPDATES, true); 
        caps.setFunctionSupport(SourceSystemFunctions.CONVERT, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 

        RealMetadataFactory.createElements(pm1g1, 
                                    new String[] { "e1", "e2", "e3" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
                                    new String[] { DataTypeManager.DefaultDataTypes.BIG_INTEGER, DataTypeManager.DefaultDataTypes.FLOAT, DataTypeManager.DefaultDataTypes.FLOAT});
                                
        QueryMetadataInterface metadata = RealMetadataFactory.createTransformationMetadata(metadataStore, "foo");
        
        HardcodedDataManager dataManager = new HardcodedDataManager();
        dataManager.addData("BatchedUpdate{I}",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { new Integer(1) })}); 
        
        String sql = "SELECT 1, convert(1, float), convert(1, float) INTO pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 
        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(1) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
        
        BatchedUpdateCommand buc = (BatchedUpdateCommand)dataManager.getCommandHistory().iterator().next();
        Insert insert = (Insert)buc.getUpdateCommands().get(0);
                
        Constant value0 = (Constant)insert.getValues().get(0);
        Constant value1 = (Constant)insert.getValues().get(1);
        
        assertEquals(DataTypeManager.DefaultDataClasses.BIG_INTEGER, value0.getValue().getClass());
        assertEquals(DataTypeManager.DefaultDataClasses.FLOAT, value1.getValue().getClass());
    }
    
    @Test public void testSelectInto_Case5569a_BATCH_NO_BULK_NO() {
        boolean doBatching  = false;
        boolean doBulkInsert = false;
        helpSelectInto_Case5569Processor( doBatching, doBulkInsert );
    }    
    
    @Test public void testSelectInto_Case5569b_BATCH_YES_BULK_NO() {
        boolean doBatching  = true;
        boolean doBulkInsert = false;
        helpSelectInto_Case5569Processor( doBatching, doBulkInsert );
    }
    
    @Test public void testSelectInto_Case5569c_BATCH_NO_BULK_YES() {
        boolean doBatching  = false;
        boolean doBulkInsert = true;
        helpSelectInto_Case5569Processor( doBatching, doBulkInsert );
    }

    public void helpSelectInto_Case5569Processor( boolean doBatching, boolean doBulkInsert ) {
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
 
        caps.setCapabilitySupport(Capability.BATCHED_UPDATES, doBatching); 
        caps.setCapabilitySupport(Capability.BULK_UPDATE, doBulkInsert); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_ITERATOR, doBulkInsert);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, false);

        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 

        QueryMetadataInterface metadata = RealMetadataFactory.example1Cached();
        
        HardcodedDataManager dataManager = new HardcodedDataManager();

        dataManager.addData("SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 FROM pm1.g1",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { "1", new Integer(1), Boolean.FALSE, new Double(1)}),     //$NON-NLS-1$
                                         Arrays.asList(new Object[] { "2", new Integer(2), Boolean.TRUE, new Double(2) })});    //$NON-NLS-1$
        
        if (doBulkInsert) {
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES (...)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(2)})});             
        } 
        else 
        if (doBatching) {
            dataManager.addData("BatchedUpdate{I,I}",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(2)})});             
        } else {
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});             
        }

        String sql = "SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 INTO pm1.g2 from pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(2) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
        
        // if not doBulkInsert and is doBatching,
        //    check the command hist to ensure it contains the expected commands
        if ( !doBulkInsert && doBatching ) {
            BatchedUpdateCommand bu = (BatchedUpdateCommand)dataManager.getCommandHistory().get(1);
            assertEquals(2, bu.getUpdateCommands().size());
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)", bu.getUpdateCommands().get(0).toString() );  //$NON-NLS-1$
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)", bu.getUpdateCommands().get(1).toString() );  //$NON-NLS-1$ 
        }        
    }
    

    @Test public void testSelectInto_Case5412a() {
        MetadataStore metadataStore = new MetadataStore();
        // test setting BULK_INSERT capability to true
        Schema pm1 = RealMetadataFactory.createPhysicalModel("pm1", metadataStore); //$NON-NLS-1$
        Table pm1g1 = RealMetadataFactory.createPhysicalGroup("g1", pm1); //$NON-NLS-1$
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_ITERATOR, true);
        caps.setFunctionSupport(SourceSystemFunctions.CONVERT, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 

        RealMetadataFactory.createElements(pm1g1, 
                                    new String[] { "e1", "e2" }, //$NON-NLS-1$ //$NON-NLS-2$ 
                                    new String[] { DataTypeManager.DefaultDataTypes.BIG_INTEGER, DataTypeManager.DefaultDataTypes.FLOAT});
                                
        QueryMetadataInterface metadata = RealMetadataFactory.createTransformationMetadata(metadataStore, "foo");
        
        HardcodedDataManager dataManager = new HardcodedDataManager();
        dataManager.addData("INSERT INTO pm1.g1 (e1, e2) VALUES (...)",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { new Integer(1) })}); 
        
        String sql = "SELECT 1, convert(1, float) INTO pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 
        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(1) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
    }

    
    @Test public void testSelectInto_Case5412b() {
        MetadataStore metadataStore = new MetadataStore();
        // test setting BULK_INSERT capability to false
        Schema pm1 = RealMetadataFactory.createPhysicalModel("pm1", metadataStore); //$NON-NLS-1$
        Table pm1g1 = RealMetadataFactory.createPhysicalGroup("g1", pm1); //$NON-NLS-1$
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.BULK_UPDATE, false); 
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 
        caps.setFunctionSupport(SourceSystemFunctions.CONVERT, true);
        RealMetadataFactory.createElements(pm1g1, 
                                    new String[] { "e1", "e2" }, //$NON-NLS-1$ //$NON-NLS-2$ 
                                    new String[] { DataTypeManager.DefaultDataTypes.BIG_INTEGER, DataTypeManager.DefaultDataTypes.FLOAT});
                                
        QueryMetadataInterface metadata = RealMetadataFactory.createTransformationMetadata(metadataStore, "foo");
        
        HardcodedDataManager dataManager = new HardcodedDataManager();
        dataManager.addData("INSERT INTO pm1.g1 (e1, e2) VALUES (1, 1.0)",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { new Integer(1) })}); 
        
        String sql = "SELECT 1, convert(1, float) INTO pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 
        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(1) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
        
        Insert insert = (Insert)dataManager.getCommandHistory().iterator().next();
        
        Constant value0 = (Constant)insert.getValues().get(0);
        Constant value1 = (Constant)insert.getValues().get(1);
        
        assertEquals(DataTypeManager.DefaultDataClasses.BIG_INTEGER, value0.getValue().getClass());
        assertEquals(DataTypeManager.DefaultDataClasses.FLOAT, value1.getValue().getClass());
    }
    
    @Test public void testInsertIntoWithSubquery_None() {
        helpInsertIntoWithSubquery( null );
    }    
    
    @Test public void testInsertIntoWithSubquery_Batch() {
        helpInsertIntoWithSubquery( Capability.BATCHED_UPDATES );
    }
    
    @Test public void testInsertIntoWithSubquery_Bulk() {
        helpInsertIntoWithSubquery( Capability.INSERT_WITH_ITERATOR );
    }
    
    @Test public void testInsertIntoWithSubquery_Pushdown() {
        helpInsertIntoWithSubquery( Capability.INSERT_WITH_QUERYEXPRESSION );
    }

    public void helpInsertIntoWithSubquery( Capability cap ) {
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
 
        caps.setCapabilitySupport(cap, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, false);

        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 

        QueryMetadataInterface metadata = RealMetadataFactory.example1Cached();
        
        HardcodedDataManager dataManager = new HardcodedDataManager();

        dataManager.addData("SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 FROM pm1.g1",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { "1", new Integer(1), Boolean.FALSE, new Double(1)}),     //$NON-NLS-1$
                                         Arrays.asList(new Object[] { "2", new Integer(2), Boolean.TRUE, new Double(2) })});    //$NON-NLS-1$
        
        if (cap != null) {
        	switch (cap) {
	        case INSERT_WITH_ITERATOR:
	            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES (...)",  //$NON-NLS-1$ 
	                    new List[] { Arrays.asList(new Object[] { new Integer(2)})});
	            break;
	        case BATCHED_UPDATES:
	            dataManager.addData("BatchedUpdate{I,I}",  //$NON-NLS-1$ 
	                    new List[] { Arrays.asList(new Object[] { new Integer(2)})});
	            break;
	        case INSERT_WITH_QUERYEXPRESSION:
	        	dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 FROM pm1.g1",  //$NON-NLS-1$ 
	                    new List[] { Arrays.asList(new Object[] { new Integer(2)})});
	        	break;
	        }
        } else {
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});             
        }

        String sql = "INSERT INTO pm1.g2 SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 from pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(2) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
        
        // if not doBulkInsert and is doBatching,
        //    check the command hist to ensure it contains the expected commands
        if ( cap == Capability.BATCHED_UPDATES ) {
            BatchedUpdateCommand bu = (BatchedUpdateCommand)dataManager.getCommandHistory().get(1);
            assertEquals(2, bu.getUpdateCommands().size());
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)", bu.getUpdateCommands().get(0).toString() );  //$NON-NLS-1$
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)", bu.getUpdateCommands().get(1).toString() );  //$NON-NLS-1$ 
        }        
    }
    
    @Test public void testInsertIntoWithSubquery2_BATCH_NO_BULK_NO() {
        boolean doBatching  = false;
        boolean doBulkInsert = false;
        helpInsertIntoWithSubquery2( doBatching, doBulkInsert );
    }    
    
    @Test public void testInsertIntoWithSubquery2_BATCH_YES_BULK_NO() {
        boolean doBatching  = true;
        boolean doBulkInsert = false;
        helpInsertIntoWithSubquery2( doBatching, doBulkInsert );
    }
    
    @Test public void testInsertIntoWithSubquery2_BATCH_NO_BULK_YES() {
        boolean doBatching  = false;
        boolean doBulkInsert = true;
        helpInsertIntoWithSubquery2( doBatching, doBulkInsert );
    }

    public void helpInsertIntoWithSubquery2( boolean doBatching, boolean doBulkInsert ) {
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder(); 
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
 
        caps.setCapabilitySupport(Capability.BATCHED_UPDATES, doBatching); 
        caps.setCapabilitySupport(Capability.BULK_UPDATE, doBulkInsert); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_ITERATOR, doBulkInsert);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, false);

        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$ 

        QueryMetadataInterface metadata = RealMetadataFactory.example1Cached();
        
        HardcodedDataManager dataManager = new HardcodedDataManager();

        dataManager.addData("SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 FROM pm1.g1",  //$NON-NLS-1$ 
                            new List[] { Arrays.asList(new Object[] { "1", new Integer(1), Boolean.FALSE, new Double(1)}),     //$NON-NLS-1$
                                         Arrays.asList(new Object[] { "2", new Integer(2), Boolean.TRUE, new Double(2) })});    //$NON-NLS-1$
        
        if (doBulkInsert) {
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES (...)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(4)})});             
        } 
        else 
        if (doBatching) {
            dataManager.addData("BatchedUpdate{I,I}",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(2)})});             
        } else {
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});
            dataManager.addData("INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)",  //$NON-NLS-1$ 
                                new List[] { Arrays.asList(new Object[] { new Integer(1)})});             
        }

        String sql = "INSERT INTO pm1.g2 SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 from pm1.g1 UNION ALL SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 from pm1.g1"; //$NON-NLS-1$
//        String sql = "SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 INTO pm1.g2 from pm1.g1"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        List<?>[] expected = new List[] {   
            Arrays.asList(new Object[] { new Integer(4) }), 
        }; 
        
        helpProcess(plan, dataManager, expected);
        
        // if not doBulkInsert and is doBatching,
        //    check the command hist to ensure it contains the expected commands
        if ( !doBulkInsert && doBatching ) {
            BatchedUpdateCommand bu = (BatchedUpdateCommand)dataManager.getCommandHistory().get(2);
            assertEquals(2, bu.getUpdateCommands().size());
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('1', 1, FALSE, 1.0)", bu.getUpdateCommands().get(0).toString() );  //$NON-NLS-1$
            assertEquals( "INSERT INTO pm1.g2 (e1, e2, e3, e4) VALUES ('2', 2, TRUE, 2.0)", bu.getUpdateCommands().get(1).toString() );  //$NON-NLS-1$ 
        }        
    }
    
    @Test public void testInsertIntoVirtualWithQueryExpression() { 
        String sql = "insert into vm1.g1 (e1, e2, e3, e4) select * from pm1.g1"; //$NON-NLS-1$
        
        List<?>[] expected = new List[] { 
            Arrays.asList(6),
        };    
    
        FakeDataManager dataManager = new FakeDataManager();
        sampleData1(dataManager);
        
        // Plan query
        ProcessorPlan plan = helpGetPlan(sql, RealMetadataFactory.example1Cached());

        // Run query
        helpProcess(plan, dataManager, expected);
    }
    
    @Test public void testInsertQueryExpression() throws Exception {
        String sql = "insert into pm1.g1 select * from pm1.g2"; //$NON-NLS-1$
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_QUERYEXPRESSION, true);
        DefaultCapabilitiesFinder capFinder = new DefaultCapabilitiesFinder(caps); 
        
        QueryMetadataInterface metadata = RealMetadataFactory.example1Cached();
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        HardcodedDataManager dataManager = new HardcodedDataManager(metadata);
        List<?>[] expected = new List<?>[] {Arrays.asList(1)};
		dataManager.addData("INSERT INTO g1 (e1, e2, e3, e4) SELECT g2.e1, g2.e2, g2.e3, g2.e4 FROM g2", expected);
        helpProcess(plan, dataManager, expected);
    }
    
    @Test public void testInsertQueryExpression1() throws Exception {
        String sql = "insert into pm1.g1 (e1) select e1 from pm1.g2"; //$NON-NLS-1$
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_QUERYEXPRESSION, true);
        DefaultCapabilitiesFinder capFinder = new DefaultCapabilitiesFinder(caps); 
        
        QueryMetadataInterface metadata = RealMetadataFactory.example1Cached();
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, metadata, capFinder); 
        
        HardcodedDataManager dataManager = new HardcodedDataManager(metadata);
        List<?>[] expected = new List<?>[] {Arrays.asList(1)};
		dataManager.addData("INSERT INTO g1 (e1) SELECT g2.e1 FROM g2", expected);
        helpProcess(plan, dataManager, expected);
    }
    
    @Test public void testInsertQueryExpressionInlineView() throws Exception {
        QueryMetadataInterface metadata = RealMetadataFactory.fromDDL("CREATE foreign TABLE Test_Insert  (  status varchar(4000) ) options (updatable true);"
        		+ " CREATE foreign TABLE test_a  (  a varchar(4000) )", "x",  "y" );

        String sql = "INSERT INTO Test_Insert SELECT CASE WHEN (status = '0') AND (cnt > 0) THEN '4' ELSE status END AS status FROM"
                + "(SELECT (SELECT COUNT(*) FROM test_a AS smh2) AS cnt, a AS status FROM test_a AS smh) AS a  "; //$NON-NLS-1$
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities(); 
        caps.setCapabilitySupport(Capability.INSERT_WITH_QUERYEXPRESSION, true);
        caps.setCapabilitySupport(Capability.QUERY_SUBQUERIES_SCALAR, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_COUNT_STAR, true);
        caps.setCapabilitySupport(Capability.QUERY_SEARCHED_CASE, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_INLINE_VIEWS, true);
        DefaultCapabilitiesFinder capFinder = new DefaultCapabilitiesFinder(caps); 
        
        TestOptimizer.helpPlan(sql, metadata, new String[] {"INSERT INTO Test_Insert (status) SELECT CASE WHEN (status = '0') AND (cnt > 0) THEN '4' ELSE status END AS status FROM (SELECT a AS status, (SELECT COUNT(*) FROM y.test_a AS g_0) AS cnt FROM test_a AS smh) AS a"}, capFinder, ComparisonMode.EXACT_COMMAND_STRING);
    }
    
    @Test public void testAutoIncrementView() throws Exception {
    	String ddl = "create foreign table t1 (x integer options (auto_increment true), y string) options (updatable true); \n"
    		+ "create view v1 (x integer options (auto_increment true), y string) options (updatable true) as select * from t1;";
    	TransformationMetadata tm = RealMetadataFactory.fromDDL(ddl, "x", "y");
    	
        String sql = "insert into v1 (y) values ('a')"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, tm, new DefaultCapabilitiesFinder()); 
        
        HardcodedDataManager dataManager = new HardcodedDataManager(tm);
        List<?>[] expected = new List<?>[] {Arrays.asList(1)};
		dataManager.addData("INSERT INTO t1 (y) VALUES ('a')", expected);
        helpProcess(plan, dataManager, expected);
    }
    
    @Test public void testMerge() throws Exception {
    	String ddl = "create foreign table t1 (x integer primary key, y string) options (updatable true);";
    	TransformationMetadata tm = RealMetadataFactory.fromDDL(ddl, "x", "y");
    	
        String sql = "merge into t1 (x, y) select 1, 'a' union all select 2, 'b'"; //$NON-NLS-1$
        
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, tm, TestOptimizer.getGenericFinder()); 
        
        HardcodedDataManager dataManager = new HardcodedDataManager(tm);
        dataManager.addData("SELECT 1 FROM t1 AS g_0 WHERE g_0.x = 1", new List<?>[] {Arrays.asList(1)});
        dataManager.addData("UPDATE t1 SET y = 'a' WHERE t1.x = 1", new List<?>[] {Arrays.asList(1)});
        dataManager.addData("SELECT 1 FROM t1 AS g_0 WHERE g_0.x = 2", new List<?>[] {});
        dataManager.addData("INSERT INTO t1 (x, y) VALUES (2, 'b')", new List<?>[] {Arrays.asList(1)});
        helpProcess(plan, dataManager, new List<?>[] {Arrays.asList(2)});
    }
    
    @Test public void testInsertDefaultResolving() throws Exception {
        String sql = "insert into x (y) values ('1')"; //$NON-NLS-1$
        TransformationMetadata tm = RealMetadataFactory.fromDDL("" +
        		"create foreign table t (y string, z string) options (updatable true); " +
        		"create view x (y string, z string default 'a') options (updatable true) as select * from t; " +
        		"create trigger on x instead of insert as for each row begin insert into t (y, z) values (new.y, new.z); end;", "vdb", "source");
        Command command = helpParse(sql); 

        ProcessorPlan plan = helpGetPlan(command, tm, TestOptimizer.getGenericFinder()); 
        
        HardcodedDataManager dataManager = new HardcodedDataManager(tm);
        dataManager.addData("INSERT INTO t (y, z) VALUES ('1', 'a')", new List<?>[] {Arrays.asList(1)});
        helpProcess(plan, dataManager, new List<?>[] {Arrays.asList(1)}); 
    }
    
    @Test public void testInsertQueryExpressionLimitZero() {
        String sql = "Insert into pm1.g1 (e1) select * from (select uuid() from pm1.g2 limit 4) a limit 0"; //$NON-NLS-1$

        List<?>[] expected = new List[] { 
            Arrays.asList(new Object[] { 0})
        };    

        HardcodedDataManager dataManager = new HardcodedDataManager();
        dataManager.addData("SELECT pm1.g2.e1 FROM pm1.g2", Arrays.asList("a"));

        ProcessorPlan plan = helpGetPlan(sql, RealMetadataFactory.example1Cached());

        helpProcess(plan, dataManager, expected);
    }

}
