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
package org.modeshape.rhq.metric;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.modeshape.rhq.util.ToolBox;

/**
 * A reader and writer of ModeShape metrics using the RHQ data directory.
 */
public final class RepositoryMetricsPersister {

    static final String DATA_FILE_EXT = ".data";

    private final File dataDir;
    private File metricsFile;
    private final String repository;

    /**
     * @param pluginDataDir the RHQ plugin's data directory (cannot be <code>null</code>)
     * @param repositoryName the name of the repository (cannot be <code>null</code> or empty)
     */
    public RepositoryMetricsPersister( final File pluginDataDir,
                                       final String repositoryName ) {
        ToolBox.verifyNotNull(pluginDataDir, "pluginDataDir");
        ToolBox.verifyNotEmpty(repositoryName, "repositoryName");

        this.dataDir = pluginDataDir;
        this.repository = repositoryName;
    }

    /**
     * @return the file the metrics are persisted in (never <code>null</code>)
     * @throws Exception if there is a problem creating the file
     */
    private File metricsFile() throws Exception {
        this.dataDir.mkdirs();

        if (this.metricsFile == null) {
            this.metricsFile = new File(this.dataDir, this.repository + DATA_FILE_EXT);
            this.metricsFile.createNewFile();
        }

        return this.metricsFile;
    }

    /**
     * @return the metrics data (never <code>null</code>)
     * @throws Exception if there is a problem reading the data file
     */
    public RepositoryMetrics read() throws Exception {
        final File dataFile = metricsFile();

        if (dataFile.length() == 0) {
            return new RepositoryMetrics(this.repository);
        }

        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(dataFile);
            ois = new ObjectInputStream(fis);
            return (RepositoryMetrics)ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            } else if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * @param metricsData the data being persisted (cannot be <code>null</code>)
     * @throws Exception
     */
    public void write( final RepositoryMetrics metricsData ) throws Exception {
        ToolBox.verifyNotNull(metricsData, "metricsData");

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(metricsFile());
            oos = new ObjectOutputStream(fos);
            oos.writeObject(metricsData);
        } finally {
            if (oos != null) {
                oos.flush();
                oos.close();
            } else if (fos != null) {
                fos.close();
            }
        }
    }

}
