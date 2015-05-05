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

import org.modeshape.rhq.util.I18n;

/**
 * Localized messages for the modeshape-rhq-plugin/org.modeshape.rhq package.
 */
public class PluginI18n extends I18n {

    public static String authenticatorDescription;
    public static String authenticatorDisplayName;
    public static String cacheBinaryStorageDescription;
    public static String cacheBinaryStorageDisplayName;
    public static String compositeBinaryStorageDescription;
    public static String compositeBinaryStorageDisplayName;
    public static String connectorDescription;
    public static String connectorDisplayName;
    public static String customBinaryStorageDescription;
    public static String customBinaryStorageDisplayName;
    public static String customIndexStorageDescription;
    public static String customIndexStorageDisplayName;
    public static String dbBinaryStorageDescription;
    public static String dbBinaryStorageDisplayName;
    public static String engineDescription;
    public static String engineDisplayName;
    public static String engineNotDiscovered;
    public static String errorDiscoveringRepositories;
    public static String errorLoadingProperties;
    public static String errorLoadingRepositoryProperties;
    public static String errorLoadingResource;
    public static String errorLoadingWebAppProperties;
    public static String fileBinaryStorageDescription;
    public static String fileBinaryStorageDisplayName;
    public static String indexStorageDescription;
    public static String indexStorageDisplayName;
    public static String localFileIndexStorageDescription;
    public static String localFileIndexStorageDisplayName;
    public static String masterFileIndexStorageDescription;
    public static String masterFileIndexStorageDisplayName;
    public static String noRepositoriesDiscovered;
    public static String nullMetricsWindow;
    public static String repositoryDescription;
    public static String repositoryDisplayName;
    public static String repositoryNotFound;
    public static String sequencerDescription;
    public static String sequencerDisplayName;
    public static String slaveFileIndexStorageDescription;
    public static String slaveFileIndexStorageDisplayName;
    public static String textExtractorDescription;
    public static String textExtractorDisplayName;
    public static String unavailableMetricValue;
    public static String unexpectedMetricValue;
    public static String unexpectedParentComponent;
    public static String unknownMetric;
    public static String unknownProperty;
    public static String valueNotDefined;
    public static String webAppDescription;
    public static String webAppDisplayName;

    static {
        final PluginI18n i18n = new PluginI18n();
        i18n.initialize();
    }

    /**
     * Don't allow construction outside of this class.
     */
    private PluginI18n() {
        // nothing to do
    }

}
