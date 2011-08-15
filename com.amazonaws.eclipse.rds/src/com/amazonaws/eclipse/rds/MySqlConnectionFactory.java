/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.rds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.osgi.framework.Bundle;

import com.amazonaws.services.rds.model.DBInstance;

public class MySqlConnectionFactory extends DatabaseConnectionFactory {

    private static final String MYSQL_DRIVER_FILE_NAME = "mysql-connector-java-5.1.6-bin.jar";

    private final ImportDBInstanceDataModel wizardDataModel;

    public MySqlConnectionFactory(ImportDBInstanceDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;
    }

    @Override
    public Properties createDriverProperties() {

        Bundle bundle = Platform.getBundle(RDSPlugin.PLUGIN_ID);
        Path path = new Path("lib/" + MYSQL_DRIVER_FILE_NAME);
        URL fileURL = FileLocator.find(bundle, path, null);
        String jarList = path.toOSString();

        try {
            IPath stateLocation = Platform.getStateLocation(Platform.getBundle(RDSPlugin.PLUGIN_ID));
            File mysqlDriversDir = new File(stateLocation.toFile(), "mysqlDrivers");

            String jarPath = FileLocator.resolve(fileURL).getPath();
            File sourceFile = new File(jarPath);
            File destinationFile = new File(mysqlDriversDir, MYSQL_DRIVER_FILE_NAME);

            FileUtils.copyFile(sourceFile, destinationFile);

            jarList = destinationFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to locate MySQL driver on disk.", e);
        }

        Properties driverProperties = new Properties();
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, jarList );
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, "com.mysql.jdbc.Driver"); //$NON-NLS-1$
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID, "MySql");
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID, "5.1");
        driverProperties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String.valueOf(true));
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, "org.eclipse.datatools.enablement.mysql.5_1.driverTemplate");

        return driverProperties;
    }

    @Override
    public String createJdbcUrl() {
        DBInstance dbInstance = wizardDataModel.getDbInstance();
        return "jdbc:mysql://" + dbInstance.getEndpoint().getAddress() +
        ":" + dbInstance.getEndpoint().getPort() + "/" + dbInstance.getDBName();
    }

    @Override
    public String createDriverName() {
        return "RDS MySQL 5.1 Driver";
    }

    @Override
    public String getDriverTemplate() {
        return "org.eclipse.datatools.enablement.mysql.5_1.driverTemplate";
    }

    @Override
    public String getConnectionProfileProviderId() {
        return "org.eclipse.datatools.enablement.mysql.connectionProfile";
    }
}