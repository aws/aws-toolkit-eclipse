/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.datatools.enablement.simpledb.connection;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;

/**
 * Utility functions for SimpleDB connections, such as listing the available
 * SimpleDB service endpoints, the default endpoint, and for filling in missing
 * required properties in connection profile properties.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class SimpleDBConnectionUtils {

    /** Properties file key for the list of available SimpleDB endpoints */
    private static final String ENDPOINTS_PROPERTY = "endpoints"; //$NON-NLS-1$

    /** Properties file key for the default SimpleDB endpoint */
    private static final String DEFAULT_ENDPOINT_PROPERTY = "defaultEndpoint"; //$NON-NLS-1$

    /**
     * The backup SimpleDB endpoint to use if we run into any problems loading
     * the data from the connection properties file.
     */
    private static final String SIMPLEDB_BACKUP_ENDPOINT = "sdb.amazonaws.com"; //$NON-NLS-1$


    /**
     * Returns the default endpoint to use for SimpleDB connections.
     *
     * @return The default endpoint to use for SimpleDB connections.
     */
    public String getDefaultEndpoint() {
        String endpoint = lookupConnectionProperty(DEFAULT_ENDPOINT_PROPERTY);
        if (endpoint != null) {
            return endpoint;
        }

        return SIMPLEDB_BACKUP_ENDPOINT;
    }

    /**
     * Returns a map of available SimpleDB service endpoints, keyed by the
     * associated region name.
     *
     * @return A map of available SimpleDB service endpoints, keyed by the
     *         associated region name.
     */
    public Map<String, String> getAvailableEndpoints() {
        String endpointsPropertyValue = lookupConnectionProperty(ENDPOINTS_PROPERTY);
        if (endpointsPropertyValue != null) {
            Map<String, String> availableEndpointsByRegionName = new LinkedHashMap<>();
            for (String endpointRecord : endpointsPropertyValue.split(",")) {
                endpointRecord = endpointRecord.trim();

                // Find the last space in the endpoint record so that we can pull
                // out everything after that as the endpoint, and everything before
                // that as the region label.
                int recordSeparatorIndex = endpointRecord.lastIndexOf(" ");

                String regionName = endpointRecord.substring(0, recordSeparatorIndex);
                String endpoint = endpointRecord.substring(recordSeparatorIndex + 1);
                availableEndpointsByRegionName.put(regionName, endpoint);
            }

            return availableEndpointsByRegionName;
        }

        Map<String, String> defaultEndpoint = new HashMap<>();
        defaultEndpoint.put("US-East", SIMPLEDB_BACKUP_ENDPOINT);
        return defaultEndpoint;
    }

    /**
     * Initializes any required SimpleDB connection profile properties to safe
     * defaults if they are missing.
     *
     * <p>
     * For example, if the user has a connection profile, created with an older
     * version of the tools, that is missing a new required parameter, this
     * method will handle assigning it a reasonable default value.
     */
    public void initializeMissingProperties(final Properties properties) {
        /*
         * The first version of the toolkit didn't allow users to use multiple
         * service endpoints, so if we detect that we don't have an endpoint
         * set, we know to default to the original endpoint, sdb.amazonaws.com.
         */
        String endpoint = properties.getProperty(ISimpleDBConnectionProfileConstants.ENDPOINT);
        if (endpoint == null) {
            endpoint = getDefaultEndpoint();
            properties.setProperty(ISimpleDBConnectionProfileConstants.ENDPOINT, endpoint);
        }

        String accountId = properties.getProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID);
        if (accountId == null) {
            accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
            properties.setProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID, accountId);
        }
    }


    /*
     * Private Interface
     */

    /**
     * Looks up the specified property in the connection properties file and
     * returns the (trimmed) value. If there is no value specified, or the value
     * specified is a blank string, or if the connection properties file can't
     * be read, then null is returned.
     *
     * @param propertyName
     *            The name of the property to look up.
     *
     * @return The value of the specified property, otherwise null if the
     *         property wasn't found, the properties file couldn't be read, or
     *         the property was an empty string.
     */
    private String lookupConnectionProperty(final String propertyName) {
        URL resource = Activator.getDefault().getBundle().getResource("properties/connection.properties");

        try {
            Properties properties = new Properties();
            properties.load(resource.openStream());
            String propertyValue = properties.getProperty(propertyName);

            // Bail out and return null if we didn't find a property by that name
            if (propertyValue == null) {
                return null;
            }

            // Otherwise trim it and return it if it has real content
            propertyValue = propertyValue.trim();
            if (propertyValue.length() > 0) {
                return propertyValue;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
