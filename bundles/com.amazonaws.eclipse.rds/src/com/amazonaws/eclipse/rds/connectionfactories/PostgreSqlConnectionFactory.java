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
 * Configuration details for PostgreSQL connections.
 */
public class PostgreSqlConnectionFactory extends DatabaseConnectionFactory {

    private final ImportDBInstanceDataModel wizardDataModel;

    public PostgreSqlConnectionFactory(ImportDBInstanceDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;
    }

    @Override
    public String getDriverClass() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getDatabaseVendor() {
        return "PostgreSQL";
    }

    @Override
    public String getDatabaseVersion() {
        return "9.x";
    }

    @Override
    public String createJdbcUrl() {
        // Example PostgreSQL JDBC URL: jdbc:postgresql://server:1521/db
        String host = wizardDataModel.getDbInstance().getEndpoint().getAddress();
        Integer port = wizardDataModel.getDbInstance().getEndpoint().getPort();
        String dbName = wizardDataModel.getDbInstance().getDBName();
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    @Override
    public String createDriverName() {
        return "RDS PostgreSQL Driver";
    }

    @Override
    public String getDriverTemplate() {
        return "org.eclipse.datatools.enablement.postgresql.postgresqlDriverTemplate";
    }

    @Override
    public String getConnectionProfileProviderId() {
        return "org.eclipse.datatools.enablement.postgresql.connectionProfile";
    }
}