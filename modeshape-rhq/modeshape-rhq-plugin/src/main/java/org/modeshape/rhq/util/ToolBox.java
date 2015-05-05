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
package org.modeshape.rhq.util;

import java.util.Collection;
import java.util.Map;

/**
 * A collection of utilities.
 */
public final class ToolBox {

    /**
     * @param collection the collection being checked (can be <code>null</code> or empty)
     * @return <code>true</code> if <code>null</code> or empty
     */
    public static boolean isEmpty( final Collection<?> collection ) {
        return ((collection == null) || collection.isEmpty());
    }

    /**
     * @param map the map being checked (can be <code>null</code> or empty)
     * @return <code>true</code> if <code>null</code> or empty
     */
    public static boolean isEmpty( final Map<?, ?> map ) {
        return ((map == null) || map.isEmpty());
    }

    /**
     * @param array the array being checked (can be <code>null</code> or empty)
     * @return <code>true</code> if <code>null</code> or empty
     */
    public static boolean isEmpty( final Object[] array ) {
        return ((array == null) || (array.length == 0));
    }

    /**
     * @param text the string being checked (can be <code>null</code> or empty)
     * @return <code>true</code> if <code>null</code> or empty
     */
    public static boolean isEmpty( final String text ) {
        return ((text == null) || text.isEmpty());
    }

    /**
     * @param collectionBeingChecked the collection being checked (can be <code>null</code> or empty)
     * @param identifier the identifier used in the error message (cannot be <code>null</code> or empty)
     * @throws IllegalArgumentException if the collection is <code>null</code> or empty
     */
    public static void verifyNotEmpty( final Object[] collectionBeingChecked,
                                       final String identifier ) {
        assert !isEmpty(identifier) : "identifier cannot be empty"; //$NON-NLS-1$

        if (isEmpty(collectionBeingChecked)) {
            throw new IllegalArgumentException(I18n.bind(UtilI18n.collectionIsEmpty, identifier));
        }
    }

    /**
     * @param textBeingChecked the text being checked (can be <code>null</code> or empty)
     * @param identifier the identifier used in the error message (cannot be <code>null</code> or empty)
     * @throws IllegalArgumentException if the text is <code>null</code> or empty
     */
    public static void verifyNotEmpty( final String textBeingChecked,
                                       final String identifier ) {
        assert !isEmpty(identifier) : "identifier cannot be empty"; //$NON-NLS-1$

        if (isEmpty(textBeingChecked)) {
            throw new IllegalArgumentException(I18n.bind(UtilI18n.stringIsEmpty, identifier));
        }
    }

    /**
     * @param objBeingChecked the object being checked (can be <code>null</code>)
     * @param identifier the identifier used in the error message (cannot be <code>null</code> or empty)
     * @throws IllegalArgumentException if the object is <code>null</code>
     */
    public static void verifyNotNull( final Object objBeingChecked,
                                      final String identifier ) {
        assert !isEmpty(identifier) : "identifier cannot be empty"; //$NON-NLS-1$

        if (objBeingChecked == null) {
            throw new IllegalArgumentException(I18n.bind(UtilI18n.stringIsEmpty, identifier));
        }
    }

    public static void verifyPositive( final long number,
                                       final String identifier ) {
        if (number < 0) {
            throw new IllegalArgumentException(I18n.bind(UtilI18n.numberIsNotPositive, number, identifier));
        }
    }

    /**
     * Don't allow construction outside this class.
     */
    private ToolBox() {
        // nothing to do
    }

}
