/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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

import java.util.Properties;

import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;

import com.amazonaws.services.rds.model.DBInstance;

public class OracleConnectionFactory extends DatabaseConnectionFactory {
    
    private final ImportDBInstanceDataModel wizardDataModel;
  
    public OracleConnectionFactory(ImportDBInstanceDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;
    }
    
    
    @Override
    public Properties createDriverProperties() {
        Properties driverProperties = new Properties();
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, wizardDataModel.getJdbcDriver().getAbsolutePath());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, "oracle.jdbc.OracleDriver"); //$NON-NLS-1$
        driverProperties.setProperty("org.eclipse.datatools.enablement.oracle.catalogType", "USER");
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID, "Oracle");
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID, "11");
        driverProperties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String.valueOf(true));
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, "org.eclipse.datatools.enablement.oracle.11.driverTemplate");
        return driverProperties;
    }

    @Override
    public String createJdbcUrl() {
        DBInstance dbInstance = wizardDataModel.getDbInstance();
        // Example Oracle JDBC URL: jdbc:oracle:thin:@server:1521:db
        return "jdbc:oracle:thin:@" + dbInstance.getEndpoint().getAddress() +
            ":" + dbInstance.getEndpoint().getPort() + ":" + dbInstance.getDBName();
    }

    @Override
    public String createDriverName() {
        return "RDS Oracle 11 Driver";
    }

    @Override
    public String getDriverTemplate() {
        return "org.eclipse.datatools.enablement.oracle.11.driverTemplate";
    }
    
    @Override
    public String getConnectionProfileProviderId() {
        return "org.eclipse.datatools.enablement.oracle.connectionProfile";
    }
}
