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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver.JdbcDatabaseMetaData;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver.JdbcPreparedStatement;
import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver.JdbcStatement;
import com.amazonaws.services.simpledb.AmazonSimpleDB;

/**
 * JDBC Connection wrapper around AmazonSimpleDB driver.
 */
public class JdbcConnection implements Connection {

    private JdbcDriver driver;
    private String accessKey;
    private String secretKey;

    private boolean readOnly = false;
    private boolean autoCommit = true;
    private JdbcDatabaseMetaData metaData;

    /** Map of domain_name to list of attribute_name, where attributes are temporary ones not yet existing in the SDB. */
    private Map<String, List<String>> pendingColumns = new HashMap<>();

    /** The SimpleDB service endpoint this JDBC driver will talk to */
    private final String endpoint;

    /**
     * Creates a new JDBC connection to Amazon SimpleDB, using the specified
     * driver to connect to the specified endpoint, and the access key and
     * secret key to authenticate.
     *
     * @param driver
     *            The SimpleDB JDBC Driver object to use to communicate to
     *            SimpleDB.
     * @param accessKey
     *            The AWS access key for the desired account.
     * @param secretKey
     *            The AWS secret access key for the desired account.
     * @param endpoint
     *            The Amazon SimpleDB endpoint to communicate with.
     */
    public JdbcConnection(final JdbcDriver driver, final String accessKey, final String secretKey, final String endpoint) {
        this.driver = driver;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
    }

    /**
     * @return an instance of the AmazonSDB driver
     */
    public AmazonSimpleDB getClient() {
        try {
            return this.driver.getClient(this.accessKey, this.secretKey, this.endpoint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public void close() throws SQLException {
        this.driver = null;
        this.accessKey = null;
        this.secretKey = null;
    }

    @Override
    public void commit() throws SQLException {
        assertOpen();
        //    close();
    }

    @Override
    public void rollback() throws SQLException {
        assertOpen();
        //    close();
    }

    private void assertCursorOptions(final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
            throw new SQLException("Only TYPE_FORWARD_ONLY cursor is supported"); //$NON-NLS-1$
        }
        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new SQLException("Only CONCUR_READ_ONLY cursor is supported"); //$NON-NLS-1$
        }
        if (resultSetHoldability != ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            throw new SQLException("Only CLOSE_CURSORS_AT_COMMIT is supported"); //$NON-NLS-1$
        }
    }

    private void assertOpen() throws SQLException {
        if (this.driver == null) {
            throw new SQLException("database connection closed"); //$NON-NLS-1$
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        return createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        assertCursorOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new JdbcStatement(this);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return this.autoCommit;
    }

    @Override
    public String getCatalog() throws SQLException {
        assertOpen();
        return null;
    }

    @Override
    public void setCatalog(final String catalog) throws SQLException {
        assertOpen();
    }

    @Override
    public int getHoldability() throws SQLException {
        assertOpen();
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public void setHoldability(final int holdability) throws SQLException {
        assertOpen();
        if (holdability != ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            throw new SQLException("Only CLOSE_CURSORS_AT_COMMIT cursor is supported"); //$NON-NLS-1$
        }
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (this.metaData == null) {
            this.metaData = new JdbcDatabaseMetaData(this);
        }
        return this.metaData;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.driver == null;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.readOnly;
    }

    @Override
    public String nativeSQL(final String sql) throws SQLException {
        return sql;
    }

    @Override
    public CallableStatement prepareCall(final String sql) throws SQLException {
        return prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
    throws SQLException {
        return prepareCall(sql, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        throw new SQLException("SimpleDB does not support Stored Procedures"); //$NON-NLS-1$
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        // NB! ignores autoGeneratedKeys
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        // NB! ignores columnIndexes
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        // NB! ignores columnNames
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
    throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency, ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        assertCursorOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new JdbcPreparedStatement(this, sql);
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    @Override
    public void setReadOnly(final boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    @Override
    public int getTransactionIsolation() {
        return TRANSACTION_SERIALIZABLE;
    }

    @Override
    public void setTransactionIsolation(final int level) throws SQLException {
        if (level != TRANSACTION_SERIALIZABLE) {
            throw new SQLException("SDB supports only TRANSACTION_SERIALIZABLE"); //$NON-NLS-1$
        }
    }

    // NOT SUPPORTED ////////////////////////////////////////////////////////////

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public Savepoint setSavepoint(final String name) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public void rollback(final Savepoint savepoint) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    //////////////////////////////////////////////////////////////////////////////

    /**
     * Temporarily holds an attribute which can't be added directly to SDB without a value.
     *
     * @param table
     *          domain name
     * @param column
     *          attribute name
     */
    public void addPendingColumn(final String table, final String column) {
        List<String> columns = this.pendingColumns.get(table);
        if (columns == null) {
            columns = new ArrayList<>();
            this.pendingColumns.put(table, columns);
        }
        if (!columns.contains(column)) {
            columns.add(column);
        }
    }

    /**
     * Removes temporary attribute when it's no longer needed.
     *
     * @param table
     * @param column
     * @return <code>true</code> if pending columns list contained the specified column
     */
    public boolean removePendingColumn(final String table, final String column) {
        List<String> columns = this.pendingColumns.get(table);
        if (columns != null) {
            boolean result = columns.remove(column);
            if (columns.isEmpty()) {
                this.pendingColumns.remove(table);
            }
            return result;
        }
        return false;
    }

    /**
     * A list of temporary attributes to be returned by JDBC driver, but not existing in SDB yet since there is no values.
     *
     * @param table
     *          domain name
     * @return list of attribute names
     */
    public List<String> getPendingColumns(final String table) {
        return this.pendingColumns.get(table);
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements)
    throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Struct createStruct(final String typeName, final Object[] attributes)
    throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getClientInfo(final String name) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValid(final int timeout) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setClientInfo(final Properties properties)
    throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setClientInfo(final String name, final String value)
    throws SQLClientInfoException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isWrapperFor(final Class<?> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
    }

    @Override
    public String getSchema() throws SQLException {
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds)
            throws SQLException {
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }
}
