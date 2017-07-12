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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.connection;

import org.eclipse.datatools.connectivity.IConnection;
import org.eclipse.datatools.connectivity.IConnectionFactory;
import org.eclipse.datatools.connectivity.IConnectionProfile;

import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.SimpleDBConnectionUtils;

public class SimpleDBConnectionFactory implements IConnectionFactory {

    /**
     * SimpleDB connection utils for listing available endpoints, default
     * endpoint, populating missing required connection profile properties,
     * etc.
     */
    private SimpleDBConnectionUtils simpleDBConnectionUtils = new SimpleDBConnectionUtils();

    /**
     * @see org.eclipse.datatools.connectivity.IConnectionFactory#createConnection(org.eclipse.datatools.connectivity.IConnectionProfile)
     */
    @Override
    public IConnection createConnection(final IConnectionProfile profile) {
        this.simpleDBConnectionUtils.initializeMissingProperties(profile.getBaseProperties());

        SimpleDBConnection connection = new SimpleDBConnection(profile, getClass());
        connection.open();
        return connection;
    }

    /**
     * @see org.eclipse.datatools.connectivity.IConnectionFactory#createConnection(org.eclipse.datatools.connectivity.IConnectionProfile, java.lang.String, java.lang.String)
     */
    @Override
    public IConnection createConnection(final IConnectionProfile profile, final String uid, final String pwd) {
        return createConnection(profile);
    }

}
