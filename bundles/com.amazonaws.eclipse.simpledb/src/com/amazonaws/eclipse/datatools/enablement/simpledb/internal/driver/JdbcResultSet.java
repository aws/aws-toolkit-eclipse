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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * JDBC ResultSet implementation for the Amazon SimpleDB. Wraps around a simple raw data received from the SDB request.
 */
public class JdbcResultSet implements ResultSet, ResultSetMetaData {

    private final JdbcStatement stmt;

    private boolean open = false; // true means have results and can iterate them
    private int row = 0; // number of current row, starts at 1
    private int lastCol; // last column accessed, for wasNull(). -1 if none

    SQLWarning warning = null;

    public JdbcResultSet(final JdbcStatement stmt) {
        this.stmt = stmt;
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.warning = null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.warning;
    }

    @Override
    public void close() throws SQLException {
        this.open = false;
        this.row = 0;
        this.lastCol = -1;
    }

    public void open() {
        this.open = true;
    }

    public boolean isOpen() {
        return this.open;
    }

    /* Throws SQLException if ResultSet is not open. */
    void assertOpen() throws SQLException {
        if (!this.open) {
            throw new SQLException("ResultSet closed"); //$NON-NLS-1$
        }
    }

    @Override
    public String getCursorName() throws SQLException {
        return null;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    @Override
    public Statement getStatement() throws SQLException {
        return this.stmt;
    }

    @Override
    public int findColumn(final String columnName) throws SQLException {
        assertOpen();
        return this.stmt.data.findAttribute(columnName) + 1;
    }

    private int toNativeCol(int col) throws SQLException {
        //    if (colsMeta == null) {
        //      throw new IllegalStateException("SDB JDBC: inconsistent internal state");
        //    }
        //    if (col < 1 || col > colsMeta.length) {
        //      throw new SQLException("column " + col + " out of bounds [1," + colsMeta.length + "]");
        //    }
        return --col;
    }

    @Override
    public int getRow() throws SQLException {
        return this.row;
    }

    @Override
    public boolean next() throws SQLException {
        if (!this.open || this.stmt == null) {
            return false;
        }

        this.lastCol = -1;
        this.row++;

        // check if we are row limited by the statement or the ResultSet
        if (this.stmt.getMaxRows() != 0 && this.row > this.stmt.getMaxRows()) {
            return false;
        }

        if (this.row > this.stmt.data.getRowNum()) {
            close();
            return false;
        }

        return true;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return this.lastCol > 0 && getString(this.lastCol) == null; // simple impl, but might work
    }

    @Override
    public int getType() throws SQLException {
        return TYPE_FORWARD_ONLY;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException {
        //    if (0 > rows || (this.stmt.getMaxRows() != 0 && rows > this.stmt.getMaxRows())) {
        //      throw new SQLException("fetch size " + rows + " out of bounds " + this.stmt.getMaxRows());
        //    }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        assertOpen();
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchDirection(final int d) throws SQLException {
        assertOpen();
        if (d != ResultSet.FETCH_FORWARD) {
            throw new SQLException("only FETCH_FORWARD direction supported"); //$NON-NLS-1$
        }
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return !this.open;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return this.open && this.row == 0;
    }

    @Override
    public boolean isFirst() throws SQLException {
        return this.row == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        return this.stmt.data.getRowNum() == this.row;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    // DATA ACCESS FUNCTIONS ////////////////////////////////////////

    @Override
    public boolean getBoolean(final int col) throws SQLException {
        return getInt(col) == 0 ? false : true;
    }

    @Override
    public boolean getBoolean(final String col) throws SQLException {
        return getBoolean(findColumn(col));
    }

    @Override
    public byte getByte(final int col) throws SQLException {
        return (byte) getInt(col);
    }

    @Override
    public byte getByte(final String col) throws SQLException {
        return getByte(findColumn(col));
    }

    @Override
    public byte[] getBytes(final int col) throws SQLException {
        return getString(col).getBytes();
    }

    @Override
    public byte[] getBytes(final String col) throws SQLException {
        return getBytes(findColumn(col));
    }

    @Override
    public Date getDate(final int col) throws SQLException {
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            return new Date(new SimpleDateFormat().parse(str).getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Date getDate(final int col, final Calendar cal) throws SQLException {
        if (cal == null) {
            return getDate(col);
        }
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            cal.setTimeInMillis(new SimpleDateFormat().parse(str).getTime());
            return new Date(cal.getTime().getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Date getDate(final String col) throws SQLException {
        return getDate(findColumn(col), Calendar.getInstance());
    }

    @Override
    public Date getDate(final String col, final Calendar cal) throws SQLException {
        return getDate(findColumn(col), cal);
    }

    @Override
    public double getDouble(final int col) throws SQLException {
        String str = getString(col);
        if (str == null) {
            return 0d;
        }
        return Double.parseDouble(str);
    }

    @Override
    public double getDouble(final String col) throws SQLException {
        return getDouble(findColumn(col));
    }

    @Override
    public float getFloat(final int col) throws SQLException {
        String str = getString(col);
        if (str == null) {
            return 0f;
        }
        return Float.parseFloat(str);
    }

    @Override
    public float getFloat(final String col) throws SQLException {
        return getFloat(findColumn(col));
    }

    @Override
    public int getInt(final int col) throws SQLException {
        String str = getString(col);
        if (str == null) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    @Override
    public int getInt(final String col) throws SQLException {
        return getInt(findColumn(col));
    }

    @Override
    public long getLong(final int col) throws SQLException {
        String str = getString(col);
        if (str == null) {
            return 0;
        }
        return Long.parseLong(str);
    }

    @Override
    public long getLong(final String col) throws SQLException {
        return getLong(findColumn(col));
    }

    @Override
    public short getShort(final int col) throws SQLException {
        return (short) getInt(col);
    }

    @Override
    public short getShort(final String col) throws SQLException {
        return getShort(findColumn(col));
    }

    @Override
    public String getString(final int col) throws SQLException {
        this.lastCol = col;
        return this.stmt.data.getString(this.row - 1, col - 1, ","); //$NON-NLS-1$
    }

    @Override
    public String getString(final String col) throws SQLException {
        return getString(findColumn(col));
    }

    @Override
    public Time getTime(final int col) throws SQLException {
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            return new Time(new SimpleDateFormat().parse(str).getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Time getTime(final int col, final Calendar cal) throws SQLException {
        if (cal == null) {
            return getTime(col);
        }
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            cal.setTimeInMillis(new SimpleDateFormat().parse(str).getTime());
            return new Time(cal.getTime().getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Time getTime(final String col) throws SQLException {
        return getTime(findColumn(col));
    }

    @Override
    public Time getTime(final String col, final Calendar cal) throws SQLException {
        return getTime(findColumn(col), cal);
    }

    @Override
    public Timestamp getTimestamp(final int col) throws SQLException {
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            return new Timestamp(new SimpleDateFormat().parse(str).getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Timestamp getTimestamp(final int col, final Calendar cal) throws SQLException {
        if (cal == null) {
            return getTimestamp(col);
        }
        try {
            String str = getString(col);
            if (str == null) {
                return null;
            }
            cal.setTimeInMillis(new SimpleDateFormat().parse(str).getTime());
            return new Timestamp(cal.getTime().getTime());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    @Override
    public Timestamp getTimestamp(final String col) throws SQLException {
        return getTimestamp(findColumn(col));
    }

    @Override
    public Timestamp getTimestamp(final String c, final Calendar ca) throws SQLException {
        return getTimestamp(findColumn(c), ca);
    }

    @Override
    public Object getObject(final int col) throws SQLException {
        try {
            this.lastCol = col;
            List<String> obj = this.stmt.data.get(this.row - 1, col - 1);
            if (this.stmt.data.isItemNameColumn(this.row - 1, col - 1)) {
                return new SimpleDBItemName(obj != null && !obj.isEmpty() ? obj.get(0) : null, true); // ItemName
            } else if (obj == null) {
                return null;
            } else if (obj.size() == 1) {
                return obj.get(0); // single value
            } else {
                return new ArrayList<>(obj); // multi-value
            }
        } catch (Exception e) {
            return null;
        }

        // TODO check type somehow?
        //    switch (type) {
        //    case INTEGER:
        //      long val = getLong(col);
        //      if (val > (long) Integer.MAX_VALUE || val < (long) Integer.MIN_VALUE) {
        //        return new Long(val);
        //      } else {
        //        return new Integer((int) val);
        //      }
        //    case FLOAT:
        //      return new Double(getDouble(col));
        //    case BLOB:
        //      return getBytes(col);
        //    default:
        //      return getString(col);
        //    }
    }

    @Override
    public Object getObject(final String col) throws SQLException {
        return getObject(findColumn(col));
    }

    // METADATA /////////////////////////////////////////////////////////////////

    @Override
    public String getCatalogName(final int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public String getColumnClassName(final int column) throws SQLException {
        toNativeCol(column);
        return "java.lang.String"; //$NON-NLS-1$
    }

    @Override
    public int getColumnCount() throws SQLException {
        toNativeCol(1);
        return this.stmt.data.getColumnNum();
    }

    @Override
    public int getColumnDisplaySize(final int column) throws SQLException {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getColumnLabel(final int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public String getColumnName(final int column) throws SQLException {
        toNativeCol(column);
        return this.stmt.data.getAttributes().get(column - 1);
    }

    @Override
    public int getColumnType(final int column) throws SQLException {
        toNativeCol(column);
        return this.stmt.data.isItemNameColumn(this.row - 1, column - 1) ? Types.OTHER : Types.VARCHAR;
    }

    @Override
    public String getColumnTypeName(final int column) throws SQLException {
        toNativeCol(column);
        return this.stmt.data.isItemNameColumn(this.row - 1, column - 1) ? "TEXTID" : "TEXT"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int getPrecision(final int column) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(final int column) throws SQLException {
        return 0;
    }

    @Override
    public String getSchemaName(final int column) throws SQLException {
        return null;
    }

    @Override
    public String getTableName(final int column) throws SQLException {
        return this.stmt.getDomainName();
    }

    @Override
    public boolean isAutoIncrement(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCaseSensitive(final int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isCurrency(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(final int column) throws SQLException {
        return true;
    }

    @Override
    public int isNullable(final int column) throws SQLException {
        return this.stmt.data.isItemNameColumn(this.row - 1, column - 1) ? columnNoNulls : columnNullable;
    }

    @Override
    public boolean isReadOnly(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(final int column) throws SQLException {
        return true;
    }

    @Override
    public boolean isSigned(final int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isWritable(final int column) throws SQLException {
        return true;
    }

    // NOT SUPPORTED ////////////////////////////////////////////////////////////

    @Override
    public Array getArray(final int i) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Array getArray(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getAsciiStream(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getAsciiStream(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public BigDecimal getBigDecimal(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public BigDecimal getBigDecimal(final int col, final int s) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public BigDecimal getBigDecimal(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public BigDecimal getBigDecimal(final String col, final int s) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getBinaryStream(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getBinaryStream(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Blob getBlob(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Blob getBlob(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Reader getCharacterStream(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Reader getCharacterStream(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Clob getClob(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Clob getClob(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Object getObject(final int col, final Map<String, Class<?>> map) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Object getObject(final String col, final Map<String, Class<?>> map) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Ref getRef(final int i) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public Ref getRef(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getUnicodeStream(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public InputStream getUnicodeStream(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public URL getURL(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public URL getURL(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public boolean last() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public boolean previous() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public boolean relative(final int rows) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public boolean absolute(final int row) throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public void afterLast() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public boolean first() throws SQLException {
        throw new SQLException("ResultSet is TYPE_FORWARD_ONLY"); //$NON-NLS-1$
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateArray(final int col, final Array x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateArray(final String col, final Array x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateAsciiStream(final int col, final InputStream x, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateAsciiStream(final String col, final InputStream x, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBigDecimal(final int col, final BigDecimal x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBigDecimal(final String col, final BigDecimal x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBinaryStream(final int c, final InputStream x, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBinaryStream(final String c, final InputStream x, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBlob(final int col, final Blob x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBlob(final String col, final Blob x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBoolean(final int col, final boolean x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBoolean(final String col, final boolean x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateByte(final int col, final byte x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateByte(final String col, final byte x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBytes(final int col, final byte[] x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateBytes(final String col, final byte[] x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateCharacterStream(final int c, final Reader x, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateCharacterStream(final String c, final Reader r, final int l) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateClob(final int col, final Clob x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateClob(final String col, final Clob x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateDate(final int col, final Date x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateDate(final String col, final Date x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateDouble(final int col, final double x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateDouble(final String col, final double x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateFloat(final int col, final float x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateFloat(final String col, final float x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateInt(final int col, final int x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateInt(final String col, final int x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateLong(final int col, final long x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateLong(final String col, final long x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateNull(final int col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateNull(final String col) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateObject(final int c, final Object x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateObject(final int c, final Object x, final int s) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateObject(final String col, final Object x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateObject(final String c, final Object x, final int s) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateRef(final int col, final Ref x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateRef(final String c, final Ref x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateShort(final int c, final short x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateShort(final String c, final short x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateString(final int c, final String x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateString(final String c, final String x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateTime(final int c, final Time x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateTime(final String c, final Time x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateTimestamp(final int c, final Timestamp x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void updateTimestamp(final String c, final Timestamp x) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Reader getNCharacterStream(final int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(final String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(final int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(final String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(final int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(final String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(final int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(final String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(final int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(final String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateAsciiStream(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(final String arg0, final InputStream arg1)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(final int arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAsciiStream(final String arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(final String arg0, final InputStream arg1)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(final int arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBinaryStream(final String arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(final int arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(final String arg0, final InputStream arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(final int arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateBlob(final String arg0, final InputStream arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(final String arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(final int arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateCharacterStream(final String arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(final String arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(final int arg0, final Reader arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateClob(final String arg0, final Reader arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(final String arg0, final Reader arg1)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(final int arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNCharacterStream(final String arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final int arg0, final NClob arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final String arg0, final NClob arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final int arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final String arg0, final Reader arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final int arg0, final Reader arg1, final long arg2) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNClob(final String arg0, final Reader arg1, final long arg2)
    throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNString(final int arg0, final String arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNString(final String arg0, final String arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRowId(final int arg0, final RowId arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateRowId(final String arg0, final RowId arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateSQLXML(final int arg0, final SQLXML arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateSQLXML(final String arg0, final SQLXML arg1) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type)
            throws SQLException {
        return null;
    }
}
