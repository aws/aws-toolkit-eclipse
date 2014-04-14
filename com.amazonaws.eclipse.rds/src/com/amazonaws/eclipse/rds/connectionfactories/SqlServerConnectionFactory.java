/*
 * Copyright 2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.rds.connectionfactories;

import com.amazonaws.eclipse.rds.ImportDBInstanceDataModel;

/**
 * Configuration details for Microsoft SQL Server connections.
 */
public class SqlServerConnectionFactory extends DatabaseConnectionFactory {

    private final ImportDBInstanceDataModel wizardDataModel;

    public SqlServerConnectionFactory(ImportDBInstanceDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;
    }

    @Override
    public String getDriverClass() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getDatabaseVendor() {
        return "SQL Server";
    }

    @Override
    public String getDatabaseVersion() {
        return "2012";
    }

    @Override
    public String createJdbcUrl() {
        // Example SQL Server JDBC URL: jdbc:sqlserver://server:1521
        //
        // NOTE: For SQL Server, we always use the default database, so
        // we don't specify a database name in the JDBC connection string
        String host = wizardDataModel.getDbInstance().getEndpoint().getAddress();
        Integer port = wizardDataModel.getDbInstance().getEndpoint().getPort();
        return "jdbc:sqlserver://" + host + ":" + port;
    }

    @Override
    public String createDriverName() {
        return "RDS SQL Server Driver";
    }

    @Override
    public String getDriverTemplate() {
        return "org.eclipse.datatools.enablement.msft.sqlserver.2008.driverTemplate";
    }

    @Override
    public String getConnectionProfileProviderId() {
        return "org.eclipse.datatools.enablement.msft.sqlserver.connectionProfile";
    }
}