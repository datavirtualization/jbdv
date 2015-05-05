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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.rhq.metric.RepositoryMetrics;
import org.modeshape.rhq.metric.RepositoryMetricsPersister;
import org.modeshape.rhq.util.I18n;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ModeShape repository resource component.
 */
public final class RepositoryComponent extends ModeShapeComponent {

    /**
     * The component type of a binary storage child type of a repository.
     */
    public static final String BINARY_STORAGE_TYPE = "binary-storage";

    /**
     * The component type of a configuration child type of a repository.
     */
    public static final String CONFIGURATION_TYPE = "configuration";

    /**
     * The component type of an index storage child type of a repository.
     */
    public static final String INDEX_STORAGE_TYPE = "index-storage";

    private static final String METRIC_OP_NAME = "read-attribute";

    /**
     * Currently RHQ does not allow multiple data points, for a given metric name, to be added to the same report/request.
     */
    private static final boolean MULTIPLE_DATA_POINTS_ALLOWED = false;

    /**
     * The component type of a storage child type of a repository.
     */
    public static final String STORAGE_TYPE = "storage-type";

    /**
     * The component type of a repository.
     */
    public static final String TYPE = "repository";

    /**
     * @return an operation that can be used to obtain repository authenticator child components (never <code>null</code>)
     */
    public static Operation createGetAuthenticators() {
        return new Operation("read-children-resources", "child-type", AuthenticatorComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository configuration child components (never <code>null</code>)
     */
    public static Operation createGetConfigurations() {
        return new Operation("read-children-resources", "child-type", CONFIGURATION_TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository connector child components (never <code>null</code>)
     */
    public static Operation createGetConnectors() {
        return new Operation("read-children-resources", "child-type", ConnectorComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository sequencer child components (never <code>null</code>)
     */
    public static Operation createGetSequencers() {
        return new Operation("read-children-resources", "child-type", SequencerComponent.TYPE);
    }

    /**
     * @return an operation that can be used to obtain repository text extractor child components (never <code>null</code>)
     */
    public static Operation createGetTextExtractors() {
        return new Operation("read-children-resources", "child-type", TextExtractorComponent.TYPE);
    }

    private RepositoryMetrics metrics;

    private RepositoryMetricsPersister persister;

    /**
     * Constructs a repository component.
     */
    public RepositoryComponent() {
        super(TYPE, PluginI18n.repositoryDisplayName, PluginI18n.repositoryDescription);
    }

    private void addMetricValues( final MeasurementScheduleRequest request,
                                  final MeasurementReport report ) throws Exception {
        final String metric = request.getName();
        final long timeOfRequest = System.currentTimeMillis();
        final Long prevTime = this.metrics.collectionTime(metric);
        final Address opAddr = new Address(getAddress());
        boolean persist = false;

        if (prevTime == null) {
            // create data points for each point in each window
            WINDOW_PROCESSING: for (final Window window : Window.values()) {
                final String metricName = (metric + '-' + window.getLiteral());
                final Operation op = new Operation(METRIC_OP_NAME).addArg("name", metricName);
                final Result repoResult = getASConnection().execute(Operation.Util.createRhqOperation(op, opAddr));
                final Object tempResult = repoResult.getResult();

                if ((tempResult != null) && (tempResult instanceof List<?>)) {
                    final List<?> values = (List<?>)tempResult;

                    if (!values.isEmpty()) {
                        final long interval = RepositoryMetrics.intervalFor(window);
                        long collectionTime = timeOfRequest;

                        for (int i = (values.size() - 1); i >= 0; --i) {
                            final Object value = values.get(i);

                            if (value instanceof Integer) {
                                final MeasurementDataNumeric dataPoint = new MeasurementDataNumeric(
                                                                                                    collectionTime,
                                                                                                    request,
                                                                                                    ((Integer)value).doubleValue());
                                report.addData(dataPoint);

                                if (!MULTIPLE_DATA_POINTS_ALLOWED) {
                                    persist = true;
                                    break WINDOW_PROCESSING;
                                }
                            } else if (value != null) {
                                ModeShapePlugin.LOG.error(I18n.bind(PluginI18n.unexpectedMetricValue, value, metricName));
                                break;
                            } // window may have null values at the end if window is not completed

                            collectionTime -= interval;
                        }
                    }

                    persist = true;
                } else {
                    ModeShapePlugin.LOG.debug(I18n.bind(PluginI18n.unavailableMetricValue, metricName));
                }
            }
        } else {
            // determine the window to use and the number of points needed
            final long diff = (timeOfRequest - prevTime);
            final int numDataPointsNeeded = RepositoryMetrics.dataPointsNeeded(diff);
            final Window window = RepositoryMetrics.window(diff);

            final String metricName = (metric + '-' + window.getLiteral());
            final Operation op = new Operation(METRIC_OP_NAME).addArg("name", metricName);
            final Result repoResult = getASConnection().execute(Operation.Util.createRhqOperation(op, opAddr));
            final Object tempResult = repoResult.getResult();

            if ((tempResult != null) && (tempResult instanceof List<?>)) {
                final List<?> values = (List<?>)tempResult;

                if (!values.isEmpty()) {
                    final long timeGap = RepositoryMetrics.intervalFor(window);
                    long collectionTime = timeOfRequest;
                    int numProcessed = 0;

                    for (int i = (values.size() - 1); ((i >= 0) && (numProcessed < numDataPointsNeeded)); --i) {
                        final Object value = values.get(i);

                        if (value instanceof Integer) {
                            final MeasurementDataNumeric dataPoint = new MeasurementDataNumeric(collectionTime, request,
                                                                                                ((Integer)value).doubleValue());
                            report.addData(dataPoint);
                            ++numProcessed;

                            if (!MULTIPLE_DATA_POINTS_ALLOWED) {
                                break;
                            }
                        } else if (value != null) { // window may have null values at the end window is not completed
                            ModeShapePlugin.LOG.error(I18n.bind(PluginI18n.unexpectedMetricValue, value, metricName));
                            break;
                        }

                        collectionTime -= timeGap;
                    }
                }

                persist = true;
            } else {
                ModeShapePlugin.LOG.debug(I18n.bind(PluginI18n.unavailableMetricValue, metricName));
            }
        }

        if (persist) {
            this.metrics.log(metric, timeOfRequest);
            this.persister.write(this.metrics);
        }
    }

    /**
     * @return the requested binary storage address (never <code>null</code>)
     */
    public Address createBinaryStorageAddress() {
        final Address addr = new Address(getAddress());
        addr.add(RepositoryComponent.CONFIGURATION_TYPE, RepositoryComponent.BINARY_STORAGE_TYPE);
        return addr;
    }

    /**
     * @return the requested index storage address (never <code>null</code>)
     */
    public Address createIndexStorageAddress() {
        final Address addr = new Address(getAddress());
        addr.add(RepositoryComponent.CONFIGURATION_TYPE, RepositoryComponent.INDEX_STORAGE_TYPE);
        return addr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.rhq.modules.plugins.jbossas7.BaseComponent#getAddress()
     */
    @Override
    public Address getAddress() {
        final Address addr = ModeShapePlugin.createModeShapeAddress();
        addr.add(TYPE, deploymentName());
        return addr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    @Override
    public void getValues( final MeasurementReport report,
                           final Set<MeasurementScheduleRequest> metrics ) throws Exception {
        for (final MeasurementScheduleRequest request : metrics) {
            addMetricValues(request, report);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#load()
     */
    @Override
    protected void load() throws Exception {
        final Address repoAddr = getAddress();
        final Result result = getASConnection().execute(Operation.Util.createReadResourceOperation(repoAddr, true));
        boolean success = true;

        if (result.isSuccess()) {
            final Object tempResult = result.getResult();

            if ((tempResult != null) && (tempResult instanceof Map<?, ?>)) {
                final Map<?, ?> propMap = (Map<?, ?>)tempResult;
                ModeShapePlugin.LOG.debug("Loading " + propMap.size() + " properties for repository '" + deploymentName() + '\'');
                loadProperties(propMap);
            } else {
                success = false;
            }
        } else {
            success = false;
        }

        if (!success) {
            throw new Exception(I18n.bind(PluginI18n.errorLoadingRepositoryProperties, deploymentName()));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.util.Collection)
     */
    @Override
    protected void loadProperty( final String name,
                                 final Collection<?> valueList ) throws Exception {
        String propName = null;
        String mapName = null;
        String mapProp1 = null;
        String mapProp2 = null;

        if (RhqId.NODE_TYPES.equals(name)) {
            propName = RhqId.CND_URI;
        } else if (RhqId.PREDEFINED_WORKSPACE_NAMES.equals(name)) {
            propName = RhqId.WORKSPACE_NAME;
        } else if (RhqId.ANONYMOUS_ROLES.equals(name)) {
            propName = RhqId.ROLE;
        } else if (RhqId.WORKSPACES_INITIAL_CONTENT.equals(name)) {
            mapName = RhqId.INITIAL_CONTENT;
            mapProp1 = RhqId.PREDEFINED_WORKSPACE;
            mapProp2 = RhqId.FILE_PATH;
        } else {
            throw new Exception(I18n.bind(PluginI18n.unknownProperty, name, deploymentName(), type()));
        }

        final PropertyList values = new PropertyList(name);

        if (mapName == null) {
            for (final Object obj : valueList) {
                final Property prop = new PropertySimple(propName, obj);
                values.add(prop);
            }
        } else {
            assert (mapProp1 != null);
            assert (mapProp2 != null);

            for (final Object obj : valueList) {
                final PropertyMap map = new PropertyMap(mapName);
                final Map<?, ?> propMap = (Map<?, ?>)obj;
                final Entry<?, ?> prop = propMap.entrySet().iterator().next(); // only one entry per map
                final Property workspaceProp = new PropertySimple(mapProp1, prop.getKey());
                map.put(workspaceProp);
                final Property pathProp = new PropertySimple(mapProp2, prop.getValue());
                map.put(pathProp);
                values.add(map);
            }
        }

        resourceConfiguration().put(values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#loadProperty(java.lang.String, java.lang.String)
     */
    @Override
    protected void loadProperty( final String name,
                                 final String value ) throws Exception {
        // list/map properties without values must still be loaded as list/map
        String mapName = null;
        String propName = null;

        if (RhqId.NODE_TYPES.equals(name)) {
            propName = RhqId.CND_URI;
        } else if (RhqId.PREDEFINED_WORKSPACE_NAMES.equals(name)) {
            propName = RhqId.WORKSPACE_NAME;
        } else if (RhqId.ANONYMOUS_ROLES.equals(name)) {
            propName = RhqId.ROLE;
        } else if (RhqId.WORKSPACES_INITIAL_CONTENT.equals(name)) {
            mapName = RhqId.INITIAL_CONTENT;
        }

        if (propName != null) {
            final PropertyList values = new PropertyList(name); // no values so empty list
            resourceConfiguration().put(values);
        } else if (mapName != null) {
            final PropertyList values = new PropertyList(name);
            final PropertyMap propMap = new PropertyMap(mapName); // no values so empty map
            values.add(propMap);
            resourceConfiguration().put(values);
        } else {
            super.loadProperty(name, value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.rhq.ModeShapeComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    @Override
    public void start( final ResourceContext<ModeShapeComponent> resourceContext )
        throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);
        this.persister = new RepositoryMetricsPersister(context().getDataDirectory(), deploymentName());
        this.metrics = new RepositoryMetrics(deploymentName());
    }

    /**
     * Identifiers that match the <code>rhq-plugin.xml</code> identifiers and pertain to list and map properties.
     */
    private interface RhqId {
        String ANONYMOUS_ROLES = "anonymous-roles";
        String CND_URI = "cnd-uri";
        String FILE_PATH = "file-path";
        String INITIAL_CONTENT = "initial-content";
        String NODE_TYPES = "node-types";
        String PREDEFINED_WORKSPACE = "predefined-workspace";
        String PREDEFINED_WORKSPACE_NAMES = "predefined-workspace-names";
        String ROLE = "role";
        String WORKSPACE_NAME = "workspace-name";
        String WORKSPACES_INITIAL_CONTENT = "workspaces-initial-content";
    }

}
