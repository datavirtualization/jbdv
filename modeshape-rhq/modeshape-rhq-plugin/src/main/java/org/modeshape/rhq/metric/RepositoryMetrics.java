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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.rhq.PluginI18n;
import org.modeshape.rhq.util.ToolBox;

/**
 * Represents the repository metrics data.
 */
public final class RepositoryMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @param millisSinceLastCollection the milliseconds since the last collection time (must be positive)
     * @return the number of data points that must be created
     */
    public static int dataPointsNeeded( final long millisSinceLastCollection ) {
        ToolBox.verifyPositive(millisSinceLastCollection, "millisSinceLastCollection");

        int count = -1;
        long interval = -1;
        long windowMillis = -1;

        for (final WindowDetails details : WindowDetails.ascending()) {
            if (millisSinceLastCollection <= details.millis()) {
                count = details.count();
                interval = details.interval();
                windowMillis = details.millis();
                break;
            }
        }

        // if not set than time was greater than largest window duration
        if (count == -1) {
            count = WindowDetails.YEAR.count();
            interval = WindowDetails.YEAR.interval();
            windowMillis = WindowDetails.YEAR.millis();
        }

        long check = windowMillis;

        for (; (count > 0); --count) {
            if (millisSinceLastCollection >= check) {
                return count;
            }

            check -= interval;
        }

        return 0;
    }

    /**
     * @param window the window whose interval is being requested (cannot be <code>null</code>)
     * @return the interval (milliseconds between data points)
     * @throws IllegalArgumentException if the window is <code>null</code>)
     */
    public static long intervalFor( final Window window ) throws IllegalArgumentException {
        ToolBox.verifyNotNull(window, "window");

        for (final WindowDetails details : WindowDetails.values()) {
            if (details.window() == window) {
                return details.interval();
            }
        }

        throw new IllegalArgumentException(PluginI18n.nullMetricsWindow);
    }

    /**
     * @param millisSinceLastCollection the milliseconds since the last collection time (must be positive)
     * @return the window needed to create data points from (never <code>null</code>)
     */
    public static Window window( final long millisSinceLastCollection ) {
        ToolBox.verifyPositive(millisSinceLastCollection, "millisSinceLastCollection");

        for (final WindowDetails details : WindowDetails.ascending()) {
            if (millisSinceLastCollection <= details.millis()) {
                return details.window();
            }
        }

        // collection time greater than the largest window duration
        return Window.PREVIOUS_52_WEEKS;
    }

    /**
     * key = metric name, value = last collection time
     */
    private final Map<String, Long> metricCollectionTimes;

    /**
     * The repository name (never <code>null</code> or empty).
     */
    private final String repository;

    /**
     * @param repositoryName the name of the repository whose metrics is being persisted (cannot be <code>null</code> or empty)
     */
    public RepositoryMetrics( final String repositoryName ) {
        ToolBox.verifyNotEmpty(repositoryName, "repositoryName");
        this.repository = repositoryName;
        this.metricCollectionTimes = new HashMap<String, Long>();
    }

    /**
     * @param metric the metric whose last collection time is being requested (cannot be <code>null</code> or empty)
     * @return the last collection time or <code>null</code> if one does not exist
     */
    public Long collectionTime( final String metric ) {
        ToolBox.verifyNotEmpty(metric, "metric");
        return this.metricCollectionTimes.get(metric);
    }

    /**
     * @return the number of metrics that have been logged
     */
    public int count() {
        return this.metricCollectionTimes.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object thatMetricsData ) {
        if ((thatMetricsData == null) || !getClass().equals(thatMetricsData.getClass())) {
            return false;
        }

        final RepositoryMetrics that = (RepositoryMetrics)thatMetricsData;
        return (this.repository.equals(that.repository) && this.metricCollectionTimes.equals(that.metricCollectionTimes));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (this.repository.hashCode() + this.metricCollectionTimes.hashCode());
    }

    /**
     * @param metric the name of the metric whose last collection time is being logged (cannot be <code>null</code> or empty)
     * @param lastTimeCollected the last collection time
     */
    public void log( final String metric,
                     final long lastTimeCollected ) {
        ToolBox.verifyNotEmpty(metric, "metric");
        this.metricCollectionTimes.put(metric, lastTimeCollected);
    }

    /**
     * @return the name of the repository whose metrics data is being persisted (never <code>null</code> or empty)
     */
    public String repository() {
        return this.repository;
    }

    /**
     * Information about the metric window.
     */
    enum WindowDetails {

        DAY(24, (60 * 60 * 1000), Window.PREVIOUS_24_HOURS),
        HOUR(60, (60 * 1000), Window.PREVIOUS_60_MINUTES),
        MINUTE(10, (6 * 1000), Window.PREVIOUS_60_SECONDS),
        WEEK(7, (24 * 60 * 60 * 1000), Window.PREVIOUS_7_DAYS),
        YEAR(52, (7 * 24 * 60 * 60 * 1000), Window.PREVIOUS_52_WEEKS);

        static WindowDetails[] ascending() {
            return new WindowDetails[] {MINUTE,
                                        HOUR,
                                        DAY,
                                        WEEK,
                                        YEAR};
        }

        private final int count;
        private final long interval;
        private final long millis;
        private final Window window;

        WindowDetails( final int count,
                       final long interval,
                       final Window window ) {
            this.count = count;
            this.interval = interval;
            this.millis = (count * interval);
            this.window = window;
        }

        int count() {
            return this.count;
        }

        long interval() {
            return this.interval;
        }

        long millis() {
            return this.millis;
        }

        Window window() {
            return this.window;
        }

    }

}
