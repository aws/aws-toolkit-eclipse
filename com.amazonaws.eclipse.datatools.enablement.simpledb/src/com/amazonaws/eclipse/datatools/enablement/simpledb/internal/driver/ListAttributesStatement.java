/*
 * Copyright 2008-2011 Amazon Technologies, Inc. 
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;

/**
 * Fetches domain attributes for the given domain from the Amazon SimpleDB.
 */
public class ListAttributesStatement extends JdbcPreparedStatement {

  public ListAttributesStatement(final JdbcConnection conn, final String sql) {
    super(conn, sql);
  }

  /*
  Each column description has the following columns: 
  TABLE_CAT String => table catalog (may be null) 
  TABLE_SCHEM String => table schema (may be null) 
  TABLE_NAME String => table name 
  COLUMN_NAME String => column name 
  DATA_TYPE int => SQL type from java.sql.Types 
  TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified 
  COLUMN_SIZE int => column size. For char or date types this is the maximum number of characters, for numeric or decimal types this is precision. 
  BUFFER_LENGTH is not used. 
  DECIMAL_DIGITS int => the number of fractional digits 
  NUM_PREC_RADIX int => Radix (typically either 10 or 2) 
  NULLABLE int => is NULL allowed. 
  columnNoNulls - might not allow NULL values 
  columnNullable - definitely allows NULL values 
  columnNullableUnknown - nullability unknown 
  REMARKS String => comment describing column (may be null) 
  COLUMN_DEF String => default value (may be null) 
  SQL_DATA_TYPE int => unused 
  SQL_DATETIME_SUB int => unused 
  CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column 
  ORDINAL_POSITION int => index of column in table (starting at 1) 
  IS_NULLABLE String => "NO" means column definitely does not allow NULL values; "YES" means the column might allow NULL values. An empty string means nobody knows. 
  SCOPE_CATLOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF) 
  SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF) 
  SCOPE_TABLE String => table name that this the scope of a reference attribure (null if the DATA_TYPE isn't REF) 
  SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF) 
   */
  @Override
  ExecutionResult execute(final String queryText, final int startingRow, final int maxRows, final int requestSize,
      final String nextToken) throws SQLException {

    super.execute(queryText, startingRow, maxRows, requestSize, nextToken);

    ArrayList<String> attrs = new ArrayList<String>(this.data.getAttributes());
    int itemNameColumn = -1;
    if (attrs.size() > 0) { // there was something in the domain
      itemNameColumn = this.data.getItemNameColumn(0);
    }

    this.data = new RawData();

    // to avoid attr order change on new value appearance, which breaks open editors' table structure
    String itemName = null;
    if (itemNameColumn >= 0) {
      itemName = attrs.remove(itemNameColumn);
    }
    Collections.sort(attrs);
    if (itemNameColumn >= 0) {
      attrs.add(itemNameColumn, itemName);
    }

    String domainName = getDomainName();
    for (int i = 0; i < attrs.size(); i++) {
      this.data.add("TABLE_CAT", "", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("TABLE_SCHEM", "", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("TABLE_NAME", domainName, i); //$NON-NLS-1$
      this.data.add("COLUMN_NAME", attrs.get(i), i); //$NON-NLS-1$
      this.data.add("DATA_TYPE", "12", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("TYPE_NAME", i == itemNameColumn ? "TEXTID" : "TEXT", i); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      this.data.add("COLUMN_SIZE", "1024", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("BUFFER_LENGTH", "1024", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("DECIMAL_DIGITS", "10", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("NUM_PREC_RADIX", "10", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("NULLABLE", i == itemNameColumn ? "0" : "1", i); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      this.data.add("REMARKS", null, i); //$NON-NLS-1$
      this.data.add("COLUMN_DEF", null, i); //$NON-NLS-1$
      this.data.add("SQL_DATA_TYPE", "0", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("SQL_DATETIME_SUB", "0", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("CHAR_OCTET_LENGTH", "1024", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("ORDINAL_POSITION", "" + (i + 1), i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("IS_NULLABLE", "Y", i); //$NON-NLS-1$ //$NON-NLS-2$
      this.data.add("SCOPE_CATLOG", null, i); //$NON-NLS-1$
      this.data.add("SCOPE_SCHEMA", null, i); //$NON-NLS-1$
      this.data.add("SCOPE_TABLE", null, i); //$NON-NLS-1$
      this.data.add("SOURCE_DATA_TYPE", null, i); //$NON-NLS-1$
    }

    return new ExecutionResult(null, attrs.size());
  }
}
