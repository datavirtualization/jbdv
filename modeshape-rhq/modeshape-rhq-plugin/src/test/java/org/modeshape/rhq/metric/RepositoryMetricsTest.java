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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.rhq.metric.RepositoryMetrics.WindowDetails;

/**
 * A test class for {@link RepositoryMetrics}.
 */
public class RepositoryMetricsTest {

    private RepositoryMetrics metricsData;
    private String repoName;

    @Before
    public void createRepositoryMetrics() {
        this.repoName = Long.toString(System.currentTimeMillis());
        this.metricsData = new RepositoryMetrics(this.repoName);
    }

    @Test
    public void shouldAddDataPoints() {
        this.metricsData.log("elvis", 1L);
        this.metricsData.log("costello", 2L);
        assertThat(this.metricsData.count(), is(2));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldErrorWhenLoggingNullMetric() {
        this.metricsData.log(null, 1L);
    }

    @Test
    public void shouldHaveInitialStateAtConstruction() {
        assertThat(this.metricsData.count(), is(0));
        assertThat(this.metricsData.repository(), is(this.repoName));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeCollectionTimeWhenObtainingDataPointsNeeded() {
        RepositoryMetrics.dataPointsNeeded(-1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeCollectionTimeWhenObtainingWindow() {
        RepositoryMetrics.window(-1);
    }

    @Test
    public void shouldNotObtainDataPoints() {
        assertThat(RepositoryMetrics.dataPointsNeeded(0), is(0));
    }

    @Test
    public void shouldObtainCollectionTime() {
        this.metricsData.log("elvis", 1L);
        assertThat(this.metricsData.collectionTime("elvis"), is(1L));
    }

    @Test
    public void shouldObtainDayWindow() {
        assertThat(RepositoryMetrics.window(WindowDetails.DAY.millis()), is(Window.PREVIOUS_24_HOURS));
        assertThat(RepositoryMetrics.window(WindowDetails.DAY.millis() - 1), is(Window.PREVIOUS_24_HOURS));
        assertThat(RepositoryMetrics.window(WindowDetails.HOUR.millis() + 1), is(Window.PREVIOUS_24_HOURS));
    }

    @Test
    public void shouldObtainHalfOfWindowDataPoints() {
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.YEAR.millis() / 2), is(WindowDetails.YEAR.count() / 2));
    }

    @Test
    public void shouldObtainHourWindow() {
        assertThat(RepositoryMetrics.window(WindowDetails.HOUR.millis()), is(Window.PREVIOUS_60_MINUTES));
        assertThat(RepositoryMetrics.window(WindowDetails.HOUR.millis() - 1), is(Window.PREVIOUS_60_MINUTES));
        assertThat(RepositoryMetrics.window(WindowDetails.MINUTE.millis() + 1), is(Window.PREVIOUS_60_MINUTES));
    }

    @Test
    public void shouldObtainMaxNumberOfDataPoints() {
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.MINUTE.millis()), is(WindowDetails.MINUTE.count()));
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.HOUR.millis()), is(WindowDetails.HOUR.count()));
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.DAY.millis()), is(WindowDetails.DAY.count()));
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.WEEK.millis()), is(WindowDetails.WEEK.count()));
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.YEAR.millis()), is(WindowDetails.YEAR.count()));
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.YEAR.millis() + 1), is(WindowDetails.YEAR.count()));
    }

    @Test
    public void shouldObtainMinuteWindow() {
        assertThat(RepositoryMetrics.window(WindowDetails.MINUTE.millis()), is(Window.PREVIOUS_60_SECONDS));
        assertThat(RepositoryMetrics.window(WindowDetails.MINUTE.millis() - 1), is(Window.PREVIOUS_60_SECONDS));
    }

    @Test
    public void shouldObtainNullCollectionTime() {
        assertThat(this.metricsData.collectionTime("elvis"), is(nullValue()));
    }

    @Test
    public void shouldObtainOneDataPoint() {
        assertThat(RepositoryMetrics.dataPointsNeeded(WindowDetails.MINUTE.millis()
                                                      - ((WindowDetails.MINUTE.count() - 1) * WindowDetails.MINUTE.interval())),
                   is(1));
    }

    @Test
    public void shouldObtainWeekWindow() {
        assertThat(RepositoryMetrics.window(WindowDetails.WEEK.millis()), is(Window.PREVIOUS_7_DAYS));
        assertThat(RepositoryMetrics.window(WindowDetails.WEEK.millis() - 1), is(Window.PREVIOUS_7_DAYS));
        assertThat(RepositoryMetrics.window(WindowDetails.DAY.millis() + 1), is(Window.PREVIOUS_7_DAYS));
    }

    @Test
    public void shouldObtainYearWindow() {
        assertThat(RepositoryMetrics.window(WindowDetails.YEAR.millis()), is(Window.PREVIOUS_52_WEEKS));
        assertThat(RepositoryMetrics.window(WindowDetails.YEAR.millis() - 1), is(Window.PREVIOUS_52_WEEKS));
        assertThat(RepositoryMetrics.window(WindowDetails.WEEK.millis() + 1), is(Window.PREVIOUS_52_WEEKS));
        assertThat(RepositoryMetrics.window(WindowDetails.YEAR.millis() + 1), is(Window.PREVIOUS_52_WEEKS));
    }

    @Test
    public void shouldVerifyEquality() {
        final String metric = "session-count";
        final long value = 1;
        this.metricsData.log(metric, value);

        final RepositoryMetrics thatMetricsData = new RepositoryMetrics(this.metricsData.repository());
        thatMetricsData.log(metric, value);

        assertThat(this.metricsData, is(thatMetricsData));
        assertThat(this.metricsData.hashCode(), is(thatMetricsData.hashCode()));
    }

    @Test
    public void shouldVerifyInEqualityWhenDifferentCollectionTime() {
        final String metric = "session-count";
        final long value = 1;
        this.metricsData.log(metric, value);

        final RepositoryMetrics thatMetricsData = new RepositoryMetrics(this.metricsData.repository());
        thatMetricsData.log(metric, (value + 1));

        assertThat(this.metricsData, is(not(thatMetricsData)));
        assertThat(this.metricsData.hashCode(), is(not(thatMetricsData.hashCode())));
    }

    @Test
    public void shouldVerifyInEqualityWhenDifferentMetrics() {
        final String metric = "session-count";
        final long value = 1;
        this.metricsData.log(metric, value);

        // compare same repository name but one with one metric and one without a metric
        final RepositoryMetrics thatMetricsData = new RepositoryMetrics(this.metricsData.repository());
        assertThat(this.metricsData, is(not(thatMetricsData)));
        assertThat(this.metricsData.hashCode(), is(not(thatMetricsData.hashCode())));

        // compare same repository name, each with one metric but of different names
        thatMetricsData.log((metric + 'z'), value);
        assertThat(this.metricsData, is(not(thatMetricsData)));
        assertThat(this.metricsData.hashCode(), is(not(thatMetricsData.hashCode())));
    }

    @Test
    public void shouldVerifyInEqualityWhenDifferentRepositoryName() {
        final String metric = "session-count";
        final long value = 1;
        this.metricsData.log(metric, value);

        final RepositoryMetrics thatMetricsData = new RepositoryMetrics(this.metricsData.repository() + 'z');
        thatMetricsData.log(metric, value);

        assertThat(this.metricsData, is(not(thatMetricsData)));
        assertThat(this.metricsData.hashCode(), is(not(thatMetricsData.hashCode())));
    }

}
