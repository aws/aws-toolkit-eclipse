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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;

/**
 * JDBC PersistedStatement implementation for the Amazon SimpleDB. Converts queries into Amazon API calls.
 */
public class JdbcPreparedStatement extends JdbcStatement implements PreparedStatement, ParameterMetaData {

    public JdbcPreparedStatement(final JdbcConnection conn, final String sql) {
        super(conn);
        this.sql = sql;
    }

    public void clearParameters() throws SQLException {
        if (this.params != null) {
            this.params.clear();
        }
    }

    private void setParameter(final int index, final Object value) {
        if (this.params == null) {
            this.params = new ArrayList<Object>();
        }
        for (int i = this.params.size() - 1; i < index - 1; i++) {
            this.params.add(null);
        }
        this.params.set(index - 1, value);
    }

    public boolean execute() throws SQLException {
        return execute(this.sql);
    }

    public ResultSet executeQuery() throws SQLException {
        return executeQuery(this.sql);
    }

    public int executeUpdate() throws SQLException {
        return executeUpdate(this.sql);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return getResultSet().getMetaData();
    }

    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
        // NB! ignores Calendar
        setParameter(parameterIndex, x);
    }

    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setInt(final int parameterIndex, final int x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setLong(final int parameterIndex, final long x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        setParameter(parameterIndex, null);
    }

    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        setParameter(parameterIndex, null);
    }

    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scale)
    throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setShort(final int parameterIndex, final short x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setString(final int parameterIndex, final String x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
        // NB! ignores Calendar
        setParameter(parameterIndex, x);
    }

    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
        // NB! ignores Calendar
        setParameter(parameterIndex, x);
    }

    public void setArray(final int parameterIndex, final Array x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        setParameter(parameterIndex, x);
    }

    // PARAMETER META DATA //////////////////////////////////////////////////////

    public ParameterMetaData getParameterMetaData() {
        return this;
    }

    public int getParameterCount() throws SQLException {
        checkOpen();
        return this.params == null ? 0 : this.params.size();
    }

    public String getParameterClassName(final int param) throws SQLException {
        checkOpen();
        return "java.lang.String"; //$NON-NLS-1$
    }

    public String getParameterTypeName(final int pos) {
        return "VARCHAR"; //$NON-NLS-1$
    }

    public int getParameterType(final int pos) {
        return Types.VARCHAR;
    }

    public int getParameterMode(final int pos) {
        return parameterModeIn;
    }

    public int getPrecision(final int pos) {
        return 0;
    }

    public int getScale(final int pos) {
        return 0;
    }

    public int isNullable(final int pos) {
        return parameterNullable;
    }

    public boolean isSigned(final int pos) {
        return true;
    }

    public Statement getStatement() {
        return this;
    }

    // NOT SUPPORTED ////////////////////////////////////////////////////////////

    public void addBatch() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setCharacterStream(final int pos, final Reader reader, final int length) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setRef(final int i, final Ref x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setUnicodeStream(final int pos, final InputStream x, final int length) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    public void setAsciiStream(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setAsciiStream(final int arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setBinaryStream(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setBinaryStream(final int arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setBlob(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setBlob(final int arg0, final InputStream arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setCharacterStream(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setCharacterStream(final int arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setClob(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setClob(final int arg0, final Reader arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNCharacterStream(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNCharacterStream(final int arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNClob(final int arg0, final NClob arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNClob(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNClob(final int arg0, final Reader arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setNString(final int arg0, final String arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setRowId(final int arg0, final RowId arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setSQLXML(final int arg0, final SQLXML arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public void setPoolable(final boolean arg0) throws SQLException {
        // TODO Auto-generated method stub

    }

    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public <T> T unwrap(final Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

}
