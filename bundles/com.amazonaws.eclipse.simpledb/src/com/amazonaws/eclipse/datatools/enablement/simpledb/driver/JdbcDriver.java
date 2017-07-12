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
package com.amazonaws.eclipse.datatools.enablement.simpledb.driver;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.eclipse.core.AwsClientUtils;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;

/**
 * Creates a JDBC wrapper infrastructure around AmazonSDB client library.
 */
public class JdbcDriver implements Driver {

    private final Class<?> driverClass;

    /**
     * Creates a JDBC wrapper infrastructure around AmazonSDB client library.
     *
     * @param driverClass
     *          AmazonSDB client class
     */
    public JdbcDriver(final Class<?> driverClass) {
        this.driverClass = driverClass;
    }

    /* (non-Javadoc)
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return false;
    }

    /**
     * @param url
     *          is not used
     * @param info
     *          properties contain users access and secret keys as 'user' and 'password'
     * @return {@link JdbcConnection}
     * @throws SQLException
     */
    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        String access = info.getProperty("user"); //$NON-NLS-1$
        String secret = info.getProperty("password"); //$NON-NLS-1$
        String endpoint = info.getProperty("endpoint"); //$NON-NLS-1$

        if (access == null || access.trim().length() == 0 || secret == null || secret.trim().length() == 0) {
            throw new SQLException("AWS access credentials are missing", "08001", 8001); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (endpoint == null || endpoint.trim().length() == 0) {
            throw new SQLException("Endpoint is missing", "08001", 8001); //$NON-NLS-1$ //$NON-NLS-2$
        }

        try {
            // testing to fail early
            AmazonSimpleDB service = getClient(access, secret, endpoint);
            ListDomainsRequest req = new ListDomainsRequest();
            req.setMaxNumberOfDomains(10);
            service.listDomains(req);

            JdbcConnection conn = new JdbcConnection(this, access, secret, endpoint);
            return conn;
        } catch (AmazonServiceException e) {
            SQLException se = new SQLException(e.getLocalizedMessage(), "08001", e.getStatusCode()); //$NON-NLS-1$
            se.initCause(e);
            throw se;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            SQLException se = new SQLException(e.getLocalizedMessage(), "08001", 8001); //$NON-NLS-1$
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Returns an Amazon SimpleDB client, configured with the specified access
     * key, secret key, and endpoint.
     *
     * @param access
     *            The AWS access key to use for authentication in the returned
     *            client.
     * @param secret
     *            The AWS secret access key to use for authentication in the
     *            returned client.
     * @param endpoint
     *            The SimpleDB service endpoint that the returned client should
     *            communicate with.
     *
     * @return An Amazon SimpleDB client, configured with the specified access
     *         key, secret key, and endpoint.
     *
     * @throws SQLException
     *             If any problems are encountered creating the client to
     *             return.
     */
    public AmazonSimpleDB getClient(final String access, final String secret, final String endpoint) throws SQLException {
        try {
            AWSCredentials credentials = new BasicAWSCredentials(access, secret);

            String userAgent = AwsClientUtils.formatUserAgentString("SimpleDBEclipsePlugin", Activator.getDefault()); //$NON-NLS-1$

            ClientConfiguration config = new ClientConfiguration();
            config.setUserAgent(userAgent);

            Activator plugin = Activator.getDefault();
            if (plugin != null) {
                IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
                if (proxyService.isProxiesEnabled()) {
                    IProxyData proxyData = proxyService.getProxyDataForHost(endpoint, IProxyData.HTTPS_PROXY_TYPE);
                    if (proxyData != null) {
                        config.setProxyHost(proxyData.getHost());
                        config.setProxyPort(proxyData.getPort());

                        if (proxyData.isRequiresAuthentication()) {
                            config.setProxyUsername(proxyData.getUserId());
                            config.setProxyPassword(proxyData.getPassword());
                        }
                    }
                }
            }

            Constructor<?> cstr = this.driverClass.getConstructor(new Class[] { AWSCredentials.class, ClientConfiguration.class });
            AmazonSimpleDB service = (AmazonSimpleDB) cstr.newInstance(new Object[] { credentials, config });
            service.setEndpoint("https://" + endpoint.trim());

            return service;
        } catch (Exception e) {
            SQLException se = new SQLException(e.getLocalizedMessage(), "08001", 8001); //$NON-NLS-1$
            se.initCause(e);
            throw se;
        }
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
