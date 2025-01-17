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
package org.teiid.translator.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.*;

import javax.resource.cci.ConnectionFactory;

import org.bson.types.Binary;
import org.teiid.core.types.*;
import org.teiid.language.*;
import org.teiid.language.visitor.SQLStringVisitor;
import org.teiid.metadata.FunctionMethod;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.mongodb.MongoDBConnection;
import org.teiid.translator.*;
import org.teiid.translator.jdbc.AliasModifier;
import org.teiid.translator.jdbc.FunctionModifier;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBRef;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

@Translator(name="mongodb", description="MongoDB Translator, reads and writes the data to MongoDB")
public class MongoDBExecutionFactory extends ExecutionFactory<ConnectionFactory, MongoDBConnection> {
	private static final String MONGO = "mongo"; //$NON-NLS-1$
	public static final Version TWO_4 = Version.getVersion("2.4"); //$NON-NLS-1$
    public static final Version TWO_6 = Version.getVersion("2.6"); //$NON-NLS-1$
    
    public static final String FUNC_GEO_WITHIN = "geoWithin"; //$NON-NLS-1$
	public static final String FUNC_GEO_INTERSECTS = "geoIntersects"; //$NON-NLS-1$
	public static final String FUNC_GEO_NEAR = "geoNear"; //$NON-NLS-1$
	public static final String FUNC_GEO_NEAR_SPHERE = "geoNearSphere"; //$NON-NLS-1$
    public static final String FUNC_GEO_POLYGON_WITHIN = "geoPolygonWithin"; //$NON-NLS-1$
	public static final String FUNC_GEO_POLYGON_INTERSECTS = "geoPolygonIntersects"; //$NON-NLS-1$
	
	public static final String[] GEOSPATIAL_FUNCTIONS = {FUNC_GEO_WITHIN, FUNC_GEO_INTERSECTS, FUNC_GEO_NEAR, FUNC_GEO_NEAR_SPHERE, FUNC_GEO_POLYGON_WITHIN, FUNC_GEO_POLYGON_INTERSECTS};
	public static final String AVOID_PROJECTION = "AVOID_PROJECTION"; //$NON-NLS-1$
    
	protected Map<String, FunctionModifier> functionModifiers = new TreeMap<String, FunctionModifier>(String.CASE_INSENSITIVE_ORDER);
	private Version version = TWO_4;
	
	public MongoDBExecutionFactory() {
		setSupportsOrderBy(true);
		setSupportsSelectDistinct(true);
		setSupportsDirectQueryProcedure(false);
		setSourceRequiredForMetadata(false);
		setSupportsInnerJoins(true);
		setSupportsOuterJoins(true);
		setSupportedJoinCriteria(SupportedJoinCriteria.KEY);		
	}

	@SuppressWarnings("nls")
	@Override
	public void start() throws TranslatorException {
		super.start();
		
        registerFunctionModifier("+", new AliasModifier("$add"));//$NON-NLS-1$ //$NON-NLS-2$
        registerFunctionModifier("-", new AliasModifier("$subtract"));//$NON-NLS-1$ //$NON-NLS-2$
        registerFunctionModifier("*", new AliasModifier("$multiply"));//$NON-NLS-1$ //$NON-NLS-2$
        registerFunctionModifier("/", new AliasModifier("$divide"));//$NON-NLS-1$ //$NON-NLS-2$

        registerFunctionModifier(SourceSystemFunctions.CONCAT, new AliasModifier("$concat"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.SUBSTRING, new AliasModifier("$substr"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.SUBSTRING, new FunctionModifier() {
            @Override
            public List<?> translate(Function function) {
                function.setName("$substr"); //$NON-NLS-1$

                ArrayList<Expression> params = new ArrayList<Expression>();
                
                params.add(function.getParameters().get(0));
                        
                // MongoDB is zero base index; Teiid is 1 based;
                params.add(LanguageFactory.INSTANCE.createFunction("-", new Expression[] { function.getParameters().get(1),
                        LanguageFactory.INSTANCE.createLiteral(1, TypeFacility.RUNTIME_TYPES.INTEGER) },
                        TypeFacility.RUNTIME_TYPES.INTEGER));
                
                if (function.getParameters().size() == 2) {
                    function.getParameters().add(LanguageFactory.INSTANCE.createLiteral(DataTypeManager.MAX_STRING_LENGTH,
                            TypeFacility.RUNTIME_TYPES.INTEGER));
                }
                
                params.add(function.getParameters().get(2));
                
                function.getParameters().clear();
                function.getParameters().addAll(params);
                return null;
            }
        });
        
        registerFunctionModifier(SourceSystemFunctions.LCASE, new AliasModifier("$toLower"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.UCASE, new AliasModifier("$toUpper"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.DAYOFYEAR, new AliasModifier("$dayOfYear"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.DAYOFMONTH, new AliasModifier("$dayOfMonth"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.DAYOFWEEK, new AliasModifier("$dayOfWeek"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.YEAR, new AliasModifier("$year"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.MONTH, new AliasModifier("$month"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.WEEK, new AliasModifier("$week"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.HOUR, new AliasModifier("$hour"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.MINUTE, new AliasModifier("$minute"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.SECOND, new AliasModifier("$second"));//$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.IFNULL, new AliasModifier("$ifNull")); //$NON-NLS-1$		
		
        FunctionMethod method = null;
        method = addPushDownFunction(MONGO, FUNC_GEO_INTERSECTS, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE+"[][]"); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        method = addPushDownFunction(MONGO, FUNC_GEO_WITHIN, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE+"[][]"); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        method = addPushDownFunction(MONGO, FUNC_GEO_NEAR, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE+"[]", TypeFacility.RUNTIME_NAMES.INTEGER); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        method = addPushDownFunction(MONGO, FUNC_GEO_NEAR_SPHERE, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE+"[]", TypeFacility.RUNTIME_NAMES.INTEGER); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        method = addPushDownFunction(MONGO, FUNC_GEO_POLYGON_INTERSECTS, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        method = addPushDownFunction(MONGO, FUNC_GEO_POLYGON_WITHIN, TypeFacility.RUNTIME_NAMES.BOOLEAN, TypeFacility.RUNTIME_NAMES.STRING, TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE,TypeFacility.RUNTIME_NAMES.DOUBLE); //$NON-NLS-1$ //$NON-NLS-2$
        method.setProperty(AVOID_PROJECTION, "true");
        
        registerFunctionModifier(FUNC_GEO_NEAR, new AliasModifier("$near"));//$NON-NLS-1$
        registerFunctionModifier(FUNC_GEO_NEAR_SPHERE, new AliasModifier("$nearSphere"));//$NON-NLS-1$
        registerFunctionModifier(FUNC_GEO_WITHIN, new AliasModifier("$geoWithin"));//$NON-NLS-1$
        registerFunctionModifier(FUNC_GEO_INTERSECTS, new AliasModifier("$geoIntersects"));//$NON-NLS-1$
        registerFunctionModifier(FUNC_GEO_POLYGON_INTERSECTS, new GeoPolygonFunctionModifier("$geoIntersects"));//$NON-NLS-1$
        registerFunctionModifier(FUNC_GEO_POLYGON_WITHIN, new GeoPolygonFunctionModifier("$geoWithin"));//$NON-NLS-1$
	}
	
	private static class GeoPolygonFunctionModifier extends FunctionModifier {
		private String functionName;
		
		public GeoPolygonFunctionModifier(String name) {
			this.functionName = name;
		}
		
		@Override
		public List<?> translate(Function function) {
			List<Expression> args = function.getParameters();
			Expression north = args.get(1);
			Expression east = args.get(2);
			Expression west = args.get(3);
			Expression south = args.get(4);
			
			ArrayList<Expression> points = new ArrayList<Expression>();
			points.add(new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE, Arrays.asList(west, north)));
			points.add(new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE, Arrays.asList(east, north)));
			points.add(new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE, Arrays.asList(east, south)));
			points.add(new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE, Arrays.asList(west, south)));
			points.add(new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE, Arrays.asList(west, north)));
			
			Expression coordinates = new org.teiid.language.Array(TypeFacility.RUNTIME_TYPES.DOUBLE,  points);			
			
			Function func = LanguageFactory.INSTANCE.createFunction(this.functionName,
					Arrays.asList(args.get(0), 
							LanguageFactory.INSTANCE.createLiteral("Polygon", TypeFacility.RUNTIME_TYPES.STRING), //$NON-NLS-1$
							coordinates
					),
                    Boolean.class);
			return Arrays.asList(func);  
		}
	}
	
    @TranslatorProperty(display="Database Version", description= "Database Version")
    public String getDatabaseVersion() {
        return this.version.toString();
    }

    Version getVersion() {
        return this.version;
    }
    
    /**
     * Sets the database version.  See also {@link #getVersion()}
     * @param version
     */
    public void setDatabaseVersion(String version) {
        this.version = Version.getVersion(version);
    }
    
    public void setDatabaseVersion(Version version) {
        this.version = version;
    }	
	
	@Override
    public MetadataProcessor<MongoDBConnection> getMetadataProcessor() {
	    return new MongoDBMetadataProcessor();
	}

    public void registerFunctionModifier(String name, FunctionModifier modifier) {
    	this.functionModifiers.put(name, modifier);
    }

    public Map<String, FunctionModifier> getFunctionModifiers() {
    	return this.functionModifiers;
    }

	@Override
	public ResultSetExecution createResultSetExecution(QueryExpression command, ExecutionContext executionContext, RuntimeMetadata metadata, MongoDBConnection connection) throws TranslatorException {
		return new MongoDBQueryExecution(this, command, executionContext, metadata, connection);
	}

	@Override
	public ProcedureExecution createProcedureExecution(Call command, ExecutionContext executionContext, RuntimeMetadata metadata, MongoDBConnection connection) throws TranslatorException {
		String nativeQuery = command.getMetadataObject().getProperty(SQLStringVisitor.TEIID_NATIVE_QUERY, false);
		if (nativeQuery != null) {
			return new MongoDBDirectQueryExecution(command.getArguments(), command, executionContext, metadata, connection, nativeQuery, false);
		}
		throw new TranslatorException(MongoDBPlugin.Util.gs(MongoDBPlugin.Event.TEIID18011));
	}

	@Override
	public UpdateExecution createUpdateExecution(Command command, ExecutionContext executionContext, RuntimeMetadata metadata, MongoDBConnection connection) throws TranslatorException {
		return new MongoDBUpdateExecution(this, command, executionContext, metadata, connection);
	}

	@Override
	public ProcedureExecution createDirectExecution(List<Argument> arguments, Command command, ExecutionContext executionContext, RuntimeMetadata metadata, MongoDBConnection connection) throws TranslatorException {
		return new MongoDBDirectQueryExecution(arguments.subList(1, arguments.size()), command, executionContext, metadata, connection, (String)arguments.get(0).getArgumentValue().getValue(), true);
	}

    @Override
	public boolean useAnsiJoin() {
    	return true;
    }

	@Override
	public boolean supportsSelfJoins() {
		return true;
	}

    @Override
	public boolean supportsCompareCriteriaEquals() {
    	return true;
    }

    @Override
	public boolean supportsCompareCriteriaOrdered() {
    	return true;
    }

    @Override
	public boolean supportsLikeCriteria() {
    	return true;
    }

    @Override
	public boolean supportsOrCriteria() {
    	return true;
    }

    @Override
	public boolean supportsOrderByUnrelated() {
    	return true;
    }

    @Override
	public boolean supportsGroupBy() {
    	return true;
    }

    @Override
	public boolean supportsHaving() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesSum() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesAvg() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesMin() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesMax() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesCount() {
    	return true;
    }

    @Override
	public boolean supportsAggregatesCountStar() {
    	return true;
    }

    @Override
	public List<String> getSupportedFunctions() {
        List<String> supportedFunctions = new ArrayList<String>();
        supportedFunctions.addAll(getDefaultSupportedFunctions());

        supportedFunctions.add(SourceSystemFunctions.CONCAT);
        supportedFunctions.add(SourceSystemFunctions.SUBSTRING);
        supportedFunctions.add(SourceSystemFunctions.LCASE);
        supportedFunctions.add(SourceSystemFunctions.UCASE);
        supportedFunctions.add(SourceSystemFunctions.DAYOFYEAR);
        supportedFunctions.add(SourceSystemFunctions.DAYOFMONTH);
        supportedFunctions.add(SourceSystemFunctions.DAYOFWEEK);
        supportedFunctions.add(SourceSystemFunctions.YEAR);
        supportedFunctions.add(SourceSystemFunctions.MONTH);
        supportedFunctions.add(SourceSystemFunctions.WEEK);
        supportedFunctions.add(SourceSystemFunctions.HOUR);
        supportedFunctions.add(SourceSystemFunctions.MINUTE);
        supportedFunctions.add(SourceSystemFunctions.SECOND);
        supportedFunctions.add(SourceSystemFunctions.IFNULL);

        return supportedFunctions;
    }

	public List<String> getDefaultSupportedFunctions(){
		return Arrays.asList(new String[] { "+", "-", "*", "/", "%"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

    @Override
	public boolean supportsInCriteria() {
    	return true;
    }

    @Override
	public boolean supportsNotCriteria() {
    	return true;
    }

    @Override
	public boolean supportsRowLimit() {
    	return true;
    }

    @Override
	public boolean supportsIsNullCriteria() {
    	return true;
    }

    @Override
	public boolean supportsRowOffset() {
    	return true;
    }

    @Override
	public boolean supportsBulkUpdate() {
    	return false;
    }

	@Override
	public boolean supportsLikeRegex() {
		return true;
	}

    @Override
	public boolean supportsSelectExpression() {
    	return true;
    }

    @Override
	public boolean supportsOnlyLiteralComparison() {
		return true;
	}

	/**
	 * @param field
	 * @param expectedClass
	 * @return
	 */
	public Object retrieveValue(Object value, Class<?> expectedClass, DB mongoDB, String fqn, String colName) {
		if (value == null) {
			return null;
		}

		if (value.getClass().equals(expectedClass)) {
			return value;
		}
		
		if (value instanceof DBRef) {
			Object obj = ((DBRef)value).getId();
			if (obj instanceof BasicDBObject) {
				BasicDBObject bdb = (BasicDBObject)obj;
				return bdb.get(colName);
			}
			return obj;
		}
		else if (value instanceof java.util.Date && expectedClass.equals(java.sql.Date.class)) {
			return new java.sql.Date(((java.util.Date) value).getTime());
		}
		else if (value instanceof java.util.Date && expectedClass.equals(java.sql.Timestamp.class)) {
			return new java.sql.Timestamp(((java.util.Date) value).getTime());
		}
		else if (value instanceof java.util.Date && expectedClass.equals(java.sql.Time.class)) {
			return new java.sql.Time(((java.util.Date) value).getTime());
		}
		else if (value instanceof String && expectedClass.equals(BigDecimal.class)) {
			return new BigDecimal((String)value);
		}
		else if (value instanceof String && expectedClass.equals(BigInteger.class)) {
			return new BigInteger((String)value);
		}
		else if (value instanceof String && expectedClass.equals(Character.class)) {
			return new Character(((String)value).charAt(0));
		}
		else if (value instanceof String && expectedClass.equals(BinaryType.class)) {
			return new BinaryType(((String)value).getBytes());
		}
		else if (value instanceof String && expectedClass.equals(Blob.class)) {
			GridFS gfs = new GridFS(mongoDB, fqn);
			final GridFSDBFile resource = gfs.findOne((String)value);
			if (resource == null) {
				return null;
			}
			return new BlobImpl(new InputStreamFactory() {
				@Override
				public InputStream getInputStream() throws IOException {
					return resource.getInputStream();
				}
			});
		}
		else if (value instanceof String && expectedClass.equals(Clob.class)) {
			GridFS gfs = new GridFS(mongoDB, fqn);
			final GridFSDBFile resource = gfs.findOne((String)value);
			if (resource == null) {
				return null;
			}
			return new ClobImpl(new InputStreamFactory() {
				@Override
				public InputStream getInputStream() throws IOException {
					return resource.getInputStream();
				}
			}, -1);
		}
		else if (value instanceof String && expectedClass.equals(SQLXML.class)) {
			GridFS gfs = new GridFS(mongoDB, fqn);
			final GridFSDBFile resource = gfs.findOne((String)value);
			if (resource == null) {
				return null;
			}
			return new SQLXMLImpl(new InputStreamFactory() {
				@Override
				public InputStream getInputStream() throws IOException {
					return resource.getInputStream();
				}
			});
		}
		else if (value instanceof BasicDBList) {
		    BasicDBList arrayValues = (BasicDBList)value;
            //array
		    if (expectedClass.isArray() && !(arrayValues.get(0) instanceof BasicDBObject)) {
		        Class arrayType = expectedClass.getComponentType();
		        Object array = Array.newInstance(arrayType, arrayValues.size());		        
                for (int i = 0; i < arrayValues.size(); i++) {
                    Object arrayItem = retrieveValue(arrayValues.get(i), arrayType, mongoDB, fqn, colName);
                    Array.set(array, i, arrayItem);
                }		        
                value = array;
		    }
		}
		else if (value instanceof org.bson.types.ObjectId) {
		    org.bson.types.ObjectId id = (org.bson.types.ObjectId) value;
		    value = id.toStringBabble();
		}
		return value;
	}

	/**
	 * Mongodb only supports certain data types, Teiid need to serialize them in other compatible
	 * formats, and convert them back while reading them.
	 * @param value
	 * @return
	 */
	public Object convertToMongoType(Object value, DB mongoDB, String fqn) throws TranslatorException {
		if (value == null) {
			return null;
		}

		try {
			if (value instanceof BigDecimal) {
				return ((BigDecimal)value).doubleValue();
			}
			else if (value instanceof BigInteger) {
				return ((BigInteger)value).doubleValue();
			}
			else if (value instanceof Character) {
				return ((Character)value).toString();
			}
			else if (value instanceof java.sql.Date) {
				return new java.util.Date(((java.sql.Date)value).getTime());
			}
			else if (value instanceof java.sql.Time) {
				return new java.util.Date(((java.sql.Time)value).getTime());
			}
			else if (value instanceof java.sql.Timestamp) {
				return new java.util.Date(((java.sql.Timestamp)value).getTime());
			}
			else if (value instanceof BinaryType) {
				return new Binary(((BinaryType)value).getBytes());
			}
			else if (value instanceof byte[]) {
				return new Binary((byte[])value);
			}			
			else if (value instanceof Blob) {
				String uuid = UUID.randomUUID().toString();
				GridFS gfs = new GridFS(mongoDB, fqn);
				GridFSInputFile gfsFile = gfs.createFile(((Blob)value).getBinaryStream());
				gfsFile.setFilename(uuid);
				gfsFile.save();
				return uuid;
			}
			else if (value instanceof Clob) {
				String uuid = UUID.randomUUID().toString();
				GridFS gfs = new GridFS(mongoDB, fqn);
				GridFSInputFile gfsFile = gfs.createFile(((Clob)value).getAsciiStream());
				gfsFile.setFilename(uuid);
				gfsFile.save();
				return uuid;
			}
			else if (value instanceof SQLXML) {
				String uuid = UUID.randomUUID().toString();
				GridFS gfs = new GridFS(mongoDB, fqn);
				GridFSInputFile gfsFile = gfs.createFile(((SQLXML)value).getBinaryStream());
				gfsFile.setFilename(uuid);
				gfsFile.save();
				return uuid;
			}
			else if (value instanceof Object[]) {
			    BasicDBList list = new BasicDBList();
			    for (Object obj:(Object[])value) {
			        list.add(obj);
			    }
			    return list;
			}
			return value;
		} catch (SQLException e) {
			throw new TranslatorException(e);
		}
	}
}
