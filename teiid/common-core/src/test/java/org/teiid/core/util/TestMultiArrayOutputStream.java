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

package org.teiid.core.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class TestMultiArrayOutputStream {

	@Test public void testArrayWrites() throws IOException {
		MultiArrayOutputStream maos = new MultiArrayOutputStream(2);
		for (int i = 0; i < 10; i++) {
			int len = 1 << i;
			maos.write(new byte[len], 0, len);
		}
		assertEquals((1<<10)-1, maos.getCount());
		assertEquals(1, maos.getIndex());
	}
	
	@Test public void testCount() throws IOException {
		MultiArrayOutputStream maos = new MultiArrayOutputStream(2);
		for (int i = 0; i < 4; i++) {
			maos.write(i);
		}
		assertEquals(4, maos.getCount());
		assertEquals(2, maos.getIndex());
		
		maos = new MultiArrayOutputStream(2);
		for (int i = 0; i < 4; i++) {
			int len = 3;
			maos.write(new byte[len]);
		}
		assertEquals(12, maos.getCount());
		assertEquals(6, maos.getIndex());
	}
	
}
