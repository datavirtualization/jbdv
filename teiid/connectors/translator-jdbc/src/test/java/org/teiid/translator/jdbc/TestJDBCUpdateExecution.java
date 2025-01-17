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

package org.teiid.translator.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.teiid.core.types.DataTypeManager;
import org.teiid.dqp.internal.datamgr.FakeExecutionContextImpl;
import org.teiid.language.Expression;
import org.teiid.language.ExpressionValueSource;
import org.teiid.language.Insert;
import org.teiid.language.Parameter;

public class TestJDBCUpdateExecution {

	@Test public void testInsertIteratorUpdate() throws Exception {
		Insert command = (Insert)TranslationHelper.helpTranslate(TranslationHelper.BQT_VDB, "insert into BQT1.SmallA (IntKey, IntNum) values (1, 2)"); //$NON-NLS-1$
		Parameter param = new Parameter();
		param.setType(DataTypeManager.DefaultDataClasses.INTEGER);
		param.setValueIndex(0);
		List<Expression> values = ((ExpressionValueSource)command.getValueSource()).getValues();
		values.set(0, param);
		param = new Parameter();
		param.setType(DataTypeManager.DefaultDataClasses.INTEGER);
		param.setValueIndex(1);
		values.set(1, param);
		command.setParameterValues(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(1, 2)).iterator());
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement p = Mockito.mock(PreparedStatement.class);
		Mockito.stub(p.executeBatch()).toReturn(new int [] {1, 1});
		Mockito.stub(connection.prepareStatement("INSERT INTO SmallA (IntKey, IntNum) VALUES (?, ?)")).toReturn(p); //$NON-NLS-1$
		
		JDBCExecutionFactory config = new JDBCExecutionFactory();
		
		JDBCUpdateExecution updateExecution = new JDBCUpdateExecution(command, connection, new FakeExecutionContextImpl(), config);
		updateExecution.execute();
		Mockito.verify(p, Mockito.times(2)).addBatch();
	}
	
	@Test public void testAutoGeneretionKeys() throws Exception {
		Insert command = (Insert)TranslationHelper.helpTranslate(TranslationHelper.BQT_VDB, "insert into BQT1.SmallA (IntKey, IntNum) values (1, 2)"); //$NON-NLS-1$
				
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement p = Mockito.mock(PreparedStatement.class);
		Mockito.stub(connection.prepareStatement("INSERT INTO SmallA (IntKey, IntNum) VALUES (1, 2)", Statement.RETURN_GENERATED_KEYS)).toReturn(p); //$NON-NLS-1$
		
		JDBCExecutionFactory config = new JDBCExecutionFactory() {
			@Override
			public boolean supportsGeneratedKeys() {
				return true;
			}			
		};
		ResultSet r = Mockito.mock(ResultSet.class);
		Mockito.stub(r.getMetaData()).toReturn(Mockito.mock(ResultSetMetaData.class));
		
		Mockito.stub(p.getGeneratedKeys()).toReturn(r);
		
		FakeExecutionContextImpl context = new FakeExecutionContextImpl();
		((org.teiid.query.util.CommandContext)context.getCommandContext()).setReturnAutoGeneratedKeys(true);
		
		JDBCUpdateExecution updateExecution = new JDBCUpdateExecution(command, connection, context, config);
		updateExecution.execute();
		Mockito.verify(p, Mockito.times(1)).getGeneratedKeys();
	}	
}
