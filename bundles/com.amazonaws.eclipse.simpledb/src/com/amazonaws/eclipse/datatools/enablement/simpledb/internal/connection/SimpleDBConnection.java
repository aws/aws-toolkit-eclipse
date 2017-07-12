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

import java.sql.Driver;
import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.JDBCConnection;
import org.eclipse.datatools.connectivity.internal.ConnectivityPlugin;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.ISimpleDBConnectionProfileConstants;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcDriver;

public class SimpleDBConnection extends JDBCConnection {

    private boolean mHasDriver = true;

    public SimpleDBConnection(final IConnectionProfile profile, final Class<?> factoryClass) {
        super(profile, factoryClass);
    }

    @Override
    public void open() {
        if (this.mConnection != null) {
            close();
        }

        this.mConnection = null;
        this.mConnectException = null;

        boolean hasDriver = false;
        try {
            if (getDriverDefinition() != null) {
                hasDriver = true;
                //                super.open();

                if (this.mConnection != null) {
                    close();
                }

                this.mConnection = null;
                this.mConnectException = null;

                internalCreateConnection();
            }
        } catch (Exception e) {
            if (e.getMessage().equalsIgnoreCase(
                    ConnectivityPlugin.getDefault().getResourceString("DriverConnectionBase.error.driverDefinitionNotSpecified"))) //$NON-NLS-1$
            {
                if (profileHasDriverDetails()) {
                    this.mHasDriver = false;
                } else {
                    e.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }

        if (!hasDriver) {
            internalCreateConnection();
        }
    }

    private void internalCreateConnection() {
        try {
            ClassLoader parentCL = getParentClassLoader();
            ClassLoader driverCL = createClassLoader(parentCL);

            this.mConnection = createConnection(driverCL);

            if (this.mConnection == null) {
                // Connect attempt failed without throwing an exception.
                // We'll generate one for them.
                throw new Exception(ConnectivityPlugin.getDefault().getResourceString("DriverConnectionBase.error.unknown")); //$NON-NLS-1$
            }

            initVersions();
            updateVersionCache();
        } catch (Throwable t) {
            this.mConnectException = t;
            clearVersionCache();
        }
    }

    private boolean profileHasDriverDetails() {
        Properties props = getConnectionProfile().getBaseProperties();
        String driverClass = props.getProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID);
        String jarList = props.getProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST);
        if (driverClass != null && jarList != null) {
            return true;
        }
        return false;
    }

    @Override
    protected Object createConnection(final ClassLoader cl) throws Throwable {
        Properties props = getConnectionProfile().getBaseProperties();
        Properties connectionProps = new Properties();

        String connectURL = props.getProperty(IJDBCConnectionProfileConstants.URL_PROP_ID);
        String uid = props.getProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID);
        String pwd = props.getProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID);
        String nameValuePairs = props.getProperty(IJDBCConnectionProfileConstants.CONNECTION_PROPERTIES_PROP_ID);
        String endpoint = props.getProperty(ISimpleDBConnectionProfileConstants.ENDPOINT);
        String accountId = props.getProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID);
        String propDelim = ","; //$NON-NLS-1$

        /*
         * Legacy support: only set their user and pass if they hadn't
         * explicitly set it before.
         */
        if ( uid == null || uid.length() == 0 || pwd == null || pwd.length() == 0 ) {
            AccountInfo accountInfo = null;
            if ( accountId != null ) {
                accountInfo = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(accountId);
            } else {
                // Equivalent to useGlobal legacy property
                accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
            }
            uid = accountInfo.getAccessKey();
            pwd = accountInfo.getSecretKey();
        }

        if (uid != null) {
            connectionProps.setProperty("user", uid); //$NON-NLS-1$
        }
        if (pwd != null) {
            connectionProps.setProperty("password", pwd); //$NON-NLS-1$
        }
        if (endpoint != null) {
            connectionProps.setProperty("endpoint", endpoint); //$NON-NLS-1$
        }

        if (nameValuePairs != null && nameValuePairs.length() > 0) {
            String[] pairs = parseString(nameValuePairs, ","); //$NON-NLS-1$
            String addPairs = ""; //$NON-NLS-1$
            for (int i = 0; i < pairs.length; i++) {
                String[] namevalue = parseString(pairs[i], "="); //$NON-NLS-1$
                connectionProps.setProperty(namevalue[0], namevalue[1]);
                if (i == 0 || i < pairs.length - 1) {
                    addPairs = addPairs + propDelim;
                }
                addPairs = addPairs + pairs[i];
            }
        }

        Driver jdbcDriver = new JdbcDriver(com.amazonaws.services.simpledb.AmazonSimpleDBClient.class);
        return jdbcDriver.connect(connectURL, connectionProps);
    }

}
