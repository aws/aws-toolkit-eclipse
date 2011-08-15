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

import com.amazonaws.services.rds.model.DBInstance;

class ImportDBInstanceDataModel {
    private String dbPassword;
    private DBInstance dbInstance;
    private File jdbcDriver;

    public static final String DB_PASSWORD = "dbPassword";
    public static final String DB_INSTANCE = "dbInstance";

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
    public void setJdbcDriver(File customDriver) {
        this.jdbcDriver = customDriver;
    }
}