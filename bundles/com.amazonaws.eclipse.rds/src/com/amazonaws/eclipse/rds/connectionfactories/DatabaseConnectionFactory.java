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
package com.amazonaws.eclipse.rds.connectionfactories;

import java.util.Properties;

import com.amazonaws.eclipse.rds.ImportDBInstanceDataModel;

/**
 * Implementations of this class describe the configuration values for
 * configuring a database driver and connecting to a specific RDS database
 * engine (ex: Oracle, MySQL, PostgreSQL, etc).
 */
public abstract class DatabaseConnectionFactory {

    /**
     * Returns the complete JDBC connection string for connecting to a specific
     * database engine.
     */
    public abstract String createJdbcUrl();

    /**
     * Returns the driver name to use for a connection to a specific database
     * engine.
     */
    public abstract String createDriverName();

    /**
     * Returns the Eclipse DTP driver template ID used to create new drivers for
     * a specific database engine.
     */
    public abstract String getDriverTemplate();

    /**
     * Returns the Eclipse DTP connection profile provider ID used to create new
     * connection profiles for a specific database engine.
     */
    public abstract String getConnectionProfileProviderId();

    /**
     * Returns the name of the class from the JDBC driver library that
     * implements the JDBC driver for a specific database engine.
     */
    public abstract String getDriverClass();

    /**
     * Returns the database vendor name for a specific database engine.
     */
    public abstract String getDatabaseVendor();

    /**
     * Returns the database version for a specific database engine.
     */
    public abstract String getDatabaseVersion();

    /**
     * This method can be optionally implemented to supply any custom DB driver properties.
     */
    public Properties getAdditionalDriverProperties() {
        return null;
    }

    /**
     * Creates a specific database connection factory based on the details in
     * the provided <code>wizardDataModel</code>, which describes what database
     * the user is connecting to.
     *
     * @param wizardDataModel
     *            Details of the database connection that's being established.
     *
     * @return A specific implementation of DatabaseConnectionFactory that has
     *         the database specific logic and configuration to connect to the
     *         specified database.
     */
    public static DatabaseConnectionFactory createConnectionFactory(ImportDBInstanceDataModel wizardDataModel) {
        
        final String dbEngine = wizardDataModel.getDbInstance().getEngine();
        
        if (dbEngine.startsWith("oracle")) {
            return new OracleConnectionFactory(wizardDataModel);
        } else if (dbEngine.startsWith("mysql")) {
            return new MySqlConnectionFactory(wizardDataModel);
        } else if (dbEngine.startsWith("postgres")) {
            return new PostgreSqlConnectionFactory(wizardDataModel);
        } else if (dbEngine.startsWith("sqlserver")) {
            return new SqlServerConnectionFactory(wizardDataModel);
        } else if (dbEngine.startsWith("aurora")) {
            return new AuroraConnectionFactory(wizardDataModel);
        }

        throw new RuntimeException("Unsupported database engine: " + dbEngine);
    }
}
