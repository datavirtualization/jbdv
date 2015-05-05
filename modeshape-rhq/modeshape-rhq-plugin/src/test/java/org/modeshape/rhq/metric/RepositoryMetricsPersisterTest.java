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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import org.junit.Before;
import org.junit.Test;

/**
 * A test class for {@link RepositoryMetricsPersister}.
 */
public class RepositoryMetricsPersisterTest {

    private RepositoryMetricsPersister persister;
    private String repoName;

    @Before
    public void createPersister() {
        final String tmpDirName = System.getProperty("java.io.tmpdir");
        this.repoName = Long.toString(System.currentTimeMillis());
        this.persister = new RepositoryMetricsPersister(new File(tmpDirName), this.repoName);
    }

    @Test
    public void shouldCreateEmptyDataPointsCollection() throws Exception {
        final RepositoryMetrics empty = this.persister.read();
        assertThat(empty.count(), is(0));
        assertThat(empty.repository(), is(this.repoName));
    }

    @Test
    public void shouldRoundTrip() throws Exception {
        final RepositoryMetrics in = new RepositoryMetrics(this.repoName);

        for (int i = 0; i < 10; ++i) {
            in.log(("duration-metric-" + i), System.currentTimeMillis());
            in.log(("value-metric-" + i), System.currentTimeMillis());
        }

        this.persister.write(in);

        final RepositoryMetrics out = this.persister.read();
        assertThat(out, is(in));
    }

    @Test
    public void shouldWriteEmptyDataPointsCollection() throws Exception {
        this.persister.write(new RepositoryMetrics(this.repoName));
    }

}
