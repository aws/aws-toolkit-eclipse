/*
 * Copyright 2011-2014 Amazon Technologies, Inc.
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

import org.eclipse.datatools.connectivity.drivers.DriverInstance;

import com.amazonaws.services.rds.model.DBInstance;

public class ImportDBInstanceDataModel {
    private String dbPassword;
    private DBInstance dbInstance;
    private File jdbcDriver;
    private String cidrIpRange;
    private boolean configurePermissions;
    private boolean useExistingDriverDefinition;
    private DriverInstance driverDefinition;

    public static final String DB_PASSWORD = "dbPassword";
    public static final String DB_INSTANCE = "dbInstance";
    public static final String CIDR_IP_RANGE = "cidrIpRange";
    public static final String CONFIGURE_PERMISSIONS = "configurePermissions";
    public static final String USE_EXISTING_DRIVER_DEFINITION = "useExistingDriverDefinition";
    public static final String DRIVER_DEFINITION = "driverDefinition";


    public String getDbPassword() {
        return dbPassword;
    }
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public DBInstance getDbInstance() {
        return dbInstance;
    }
    public void setDbInstance(DBInstance dbInstance) {
        this.dbInstance = dbInstance;
    }

    public File getJdbcDriver() {
        return jdbcDriver;
    }
    public void setJdbcDriver(File jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getCidrIpRange() {
        return cidrIpRange;
    }
    public void setCidrIpRange(String cidrIpRange) {
        this.cidrIpRange = cidrIpRange;
    }

    public boolean getConfigurePermissions() {
        return configurePermissions;
    }
    public void setConfigurePermissions(boolean configurePermissions) {
        this.configurePermissions = configurePermissions;
    }

    public boolean isUseExistingDriverDefinition() {
        return useExistingDriverDefinition;
    }
    public void setUseExistingDriverDefinition(boolean useExistingDriverDefinition) {
        this.useExistingDriverDefinition = useExistingDriverDefinition;
    }

    public DriverInstance getDriverDefinition() {
        return driverDefinition;
    }
    public void setDriverDefinition(DriverInstance driverDefinition) {
        this.driverDefinition = driverDefinition;
    }
}
