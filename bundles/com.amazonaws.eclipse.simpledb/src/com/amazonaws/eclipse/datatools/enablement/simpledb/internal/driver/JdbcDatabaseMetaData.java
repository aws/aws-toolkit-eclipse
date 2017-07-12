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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;

/**
 * SDB JDBC metadata wrapper. Provides domains and attributes and all other
 * database properties. For metadata queries creates specific JDBC statements
 * which are transformed later into requests through the Amazon SDB client, see
 * {@link JdbcStatement}.
 */
public class JdbcDatabaseMetaData implements DatabaseMetaData {

    private JdbcConnection conn;
    private PreparedStatement getTables = null, getColumns = null,
    getTableTypes = null, getTypeInfo = null, getCrossReference = null,
    getCatalogs = null, getSchemas = null, getUDTs = null,
    getColumnsTblName = null, getSuperTypes = null,
    getSuperTables = null, getTablePrivileges = null,
    getExportedKeys = null, getPrimaryKeys = null,
    getProcedures = null, getProcedureColumns = null,
    getAttributes = null, getBestRowIdentifier = null,
    getVersionColumns = null, getColumnPrivileges = null;

    /** Used by PrepStmt to save generating a new statement every call. */
    private PreparedStatement getGeneratedKeys = null;

    public JdbcDatabaseMetaData(final JdbcConnection connection) {
        this.conn = connection;
    }

    void checkOpen() throws SQLException {
        if (this.conn == null) {
            throw new SQLException("connection closed"); //$NON-NLS-1$
        }
    }

    synchronized void close() throws SQLException {
        if (this.conn == null) {
            return;
        }

        try {
            if (this.getTables != null) {
                this.getTables.close();
            }
            if (this.getColumns != null) {
                this.getColumns.close();
            }
            if (this.getTableTypes != null) {
                this.getTableTypes.close();
            }
            if (this.getTypeInfo != null) {
                this.getTypeInfo.close();
            }
            if (this.getCrossReference != null) {
                this.getCrossReference.close();
            }
            if (this.getCatalogs != null) {
                this.getCatalogs.close();
            }
            if (this.getSchemas != null) {
                this.getSchemas.close();
            }
            if (this.getUDTs != null) {
                this.getUDTs.close();
            }
            if (this.getColumnsTblName != null) {
                this.getColumnsTblName.close();
            }
            if (this.getSuperTypes != null) {
                this.getSuperTypes.close();
            }
            if (this.getSuperTables != null) {
                this.getSuperTables.close();
            }
            if (this.getTablePrivileges != null) {
                this.getTablePrivileges.close();
            }
            if (this.getExportedKeys != null) {
                this.getExportedKeys.close();
            }
            if (this.getPrimaryKeys != null) {
                this.getPrimaryKeys.close();
            }
            if (this.getProcedures != null) {
                this.getProcedures.close();
            }
            if (this.getProcedureColumns != null) {
                this.getProcedureColumns.close();
            }
            if (this.getAttributes != null) {
                this.getAttributes.close();
            }
            if (this.getBestRowIdentifier != null) {
                this.getBestRowIdentifier.close();
            }
            if (this.getVersionColumns != null) {
                this.getVersionColumns.close();
            }
            if (this.getColumnPrivileges != null) {
                this.getColumnPrivileges.close();
            }
            if (this.getGeneratedKeys != null) {
                this.getGeneratedKeys.close();
            }

            this.getTables = null;
            this.getColumns = null;
            this.getTableTypes = null;
            this.getTypeInfo = null;
            this.getCrossReference = null;
            this.getCatalogs = null;
            this.getSchemas = null;
            this.getUDTs = null;
            this.getColumnsTblName = null;
            this.getSuperTypes = null;
            this.getSuperTables = null;
            this.getTablePrivileges = null;
            this.getExportedKeys = null;
            this.getPrimaryKeys = null;
            this.getProcedures = null;
            this.getProcedureColumns = null;
            this.getAttributes = null;
            this.getBestRowIdentifier = null;
            this.getVersionColumns = null;
            this.getColumnPrivileges = null;
            this.getGeneratedKeys = null;
        } finally {
            this.conn = null;
        }
    }

    @Override
    public ResultSet getAttributes(final String catalog,
            final String schemaPattern, final String typeNamePattern,
            final String attributeNamePattern) throws SQLException {
        // XXX
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getBestRowIdentifier(final String catalog,
            final String schema, final String table, final int scope,
            final boolean nullable) throws SQLException {
        return new JdbcResultSet(null);
        // throw new SQLException("not supported: bestRowIdentifier");
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        if (this.getCatalogs == null) {
            this.getCatalogs = new JdbcPreparedStatement(this.conn,
            "select null as TABLE_CAT") { //$NON-NLS-1$
                @Override
                ExecutionResult execute(final String queryText,
                        final int startingRow, final int maxRows,
                        final int requestSize, final String nextToken)
                throws SQLException {
                    this.data.add("TABLE_CAT", "", 0); //$NON-NLS-1$ //$NON-NLS-2$
                    return new ExecutionResult(null, 1);
                }
            };
        }

        this.getCatalogs.clearParameters();

        return this.getCatalogs.executeQuery();
    }

    @Override
    public ResultSet getColumnPrivileges(final String catalog,
            final String schema, final String table,
            final String columnNamePattern) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getColumns(final String catalog,
            final String schemaPattern, final String tableNamePattern,
            final String columnNamePattern) throws SQLException {
        if (this.getColumns == null) {
            this.getColumns = new ListAttributesStatement(this.conn, null);
        }
        this.getColumns.clearParameters();

        if (tableNamePattern == null || tableNamePattern.length() == 0) {
            return new JdbcResultSet(null);
        }

        return this.getColumns
        .executeQuery("select * from " + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE + tableNamePattern //$NON-NLS-1$
                + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE
                + " limit 10"); //$NON-NLS-1$
    }

    @Override
    public ResultSet getCrossReference(final String primaryCatalog,
            final String primarySchema, final String primaryTable,
            final String foreignCatalog, final String foreignSchema,
            final String foreignTable) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getExportedKeys(final String catalog, final String schema,
            final String table) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getImportedKeys(final String catalog, final String schema,
            final String table) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getIndexInfo(final String catalog, final String schema,
            final String table, final boolean unique, final boolean approximate)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getPrimaryKeys(final String catalog, final String schema,
            final String table) throws SQLException {
        if (this.getPrimaryKeys == null) {
            this.getPrimaryKeys = new JdbcPreparedStatement(this.conn,
                    "select null") { //$NON-NLS-1$
                @Override
                ExecutionResult execute(final String queryText,
                        final int startingRow, final int maxRows,
                        final int requestSize, final String nextToken)
                throws SQLException {
                    this.data.add("TABLE_CAT", catalog, 0); //$NON-NLS-1$
                    this.data.add("TABLE_SCHEM", schema, 0); //$NON-NLS-1$
                    this.data.add("TABLE_NAME", table, 0); //$NON-NLS-1$
                    this.data.add(
                            "COLUMN_NAME", SimpleDBItemName.ITEM_HEADER, 0); //$NON-NLS-1$
                    this.data.add("KEY_SEQ", "0", 0); //$NON-NLS-1$ //$NON-NLS-2$
                    this.data.add("PK_NAME", null, 0); //$NON-NLS-1$
                    return new ExecutionResult(null, 1);
                }
            };
        }

        this.getPrimaryKeys.clearParameters();

        return this.getPrimaryKeys.executeQuery();
    }

    @Override
    public ResultSet getProcedureColumns(final String catalog,
            final String schemaPattern, final String procedureNamePattern,
            final String columnNamePattern) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getProcedures(final String catalog,
            final String schemaPattern, final String procedureNamePattern)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        if (this.getSchemas == null) {
            this.getSchemas = new JdbcPreparedStatement(this.conn,
                    "select null as TABLE_SCHEM, null as TABLE_CATALOG") { //$NON-NLS-1$
                @Override
                ExecutionResult execute(final String queryText,
                        final int startingRow, final int maxRows,
                        final int requestSize, final String nextToken)
                throws SQLException {
                    this.data.add("TABLE_SCHEM", "", 0); //$NON-NLS-1$ //$NON-NLS-2$
                    this.data.add("TABLE_CATALOG", "", 0); //$NON-NLS-1$ //$NON-NLS-2$
                    return new ExecutionResult(null, 1);
                }
            };
        }

        this.getSchemas.clearParameters();

        return this.getSchemas.executeQuery();
    }

    @Override
    public ResultSet getSuperTables(final String catalog,
            final String schemaPattern, final String tableNamePattern)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getSuperTypes(final String catalog,
            final String schemaPattern, final String typeNamePattern)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getTablePrivileges(final String catalog,
            final String schemaPattern, final String tableNamePattern)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        checkOpen();
        if (this.getTableTypes == null) {
            this.getTableTypes = new JdbcPreparedStatement(this.conn,
                    "select 'TABLE' as TABLE_TYPE;") { //$NON-NLS-1$
                @Override
                ExecutionResult execute(final String queryText,
                        final int startingRow, final int maxRows,
                        final int requestSize, final String nextToken)
                throws SQLException {
                    this.data.add("TABLE_TYPE", "TABLE", 0); //$NON-NLS-1$ //$NON-NLS-2$
                    return new ExecutionResult(null, 1);
                }
            };
        }
        this.getTableTypes.clearParameters();
        return this.getTableTypes.executeQuery();
    }

    @Override
    public synchronized ResultSet getTables(final String c, final String s,
            String t, final String[] types) throws SQLException {
        if (this.getTables == null) {
            this.getTables = new ListDomainsStatement(this.conn, null);
        }

        this.getTables.clearParameters();
        checkOpen();

        t = (t == null || t.length() == 0) ? "%" : t.toUpperCase(); //$NON-NLS-1$

        // String sql =
        // "select null as TABLE_CAT, null as TABLE_SCHEM, upper(name) as TABLE_NAME,"
        // +
        // " upper(type) as TABLE_TYPE, null as REMARKS, null as TYPE_CAT, null as TYPE_SCHEM,"
        // +
        // " null as TYPE_NAME, null as SELF_REFERENCING_COL_NAME, null as REF_GENERATION from domains"
        // + " where TABLE_NAME like '" + escape(t) + "'";
        //
        // if (types != null) {
        // sql += " and TABLE_TYPE in (";
        // for (int i = 0; i < types.length; i++) {
        // if (i > 0) {
        // sql += ", ";
        // }
        // sql += "'" + types[i].toUpperCase() + "'";
        // }
        // sql += ")";
        // }
        //
        // sql += ";";

        return this.getTables
        .executeQuery("select * from ALL_TABLES where " + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE //$NON-NLS-1$
                + "TABLE_NAME" + JdbcStatement.DELIMITED_IDENTIFIER_QUOTE + "='" + t + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getUDTs(final String catalog, final String schemaPattern,
            final String typeNamePattern, final int[] types)
    throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public ResultSet getVersionColumns(final String catalog,
            final String schema, final String table) throws SQLException {
        return new JdbcResultSet(null);
    }

    @Override
    public Connection getConnection() {
        return this.conn;
    }

    @Override
    public int getDatabaseMajorVersion() {
        return 1;
    }

    @Override
    public int getDatabaseMinorVersion() {
        return 0;
    }

    @Override
    public int getDriverMajorVersion() {
        return 1;
    }

    @Override
    public int getDriverMinorVersion() {
        return 0;
    }

    @Override
    public int getJDBCMajorVersion() {
        return 2009;
    }

    @Override
    public int getJDBCMinorVersion() {
        return 4;
    }

    @Override
    public int getDefaultTransactionIsolation() {
        return Connection.TRANSACTION_SERIALIZABLE;
    }

    @Override
    public int getMaxBinaryLiteralLength() {
        return 0;
    }

    @Override
    public int getMaxCatalogNameLength() {
        return 0;
    }

    @Override
    public int getMaxCharLiteralLength() {
        return 0;
    }

    @Override
    public int getMaxColumnNameLength() {
        return 0;
    }

    @Override
    public int getMaxColumnsInGroupBy() {
        return 0;
    }

    @Override
    public int getMaxColumnsInIndex() {
        return 0;
    }

    @Override
    public int getMaxColumnsInOrderBy() {
        return 0;
    }

    @Override
    public int getMaxColumnsInSelect() {
        return 0;
    }

    @Override
    public int getMaxColumnsInTable() {
        return 0;
    }

    @Override
    public int getMaxConnections() {
        return 0;
    }

    @Override
    public int getMaxCursorNameLength() {
        return 0;
    }

    @Override
    public int getMaxIndexLength() {
        return 0;
    }

    @Override
    public int getMaxProcedureNameLength() {
        return 0;
    }

    @Override
    public int getMaxRowSize() {
        return 0;
    }

    @Override
    public int getMaxSchemaNameLength() {
        return 0;
    }

    @Override
    public int getMaxStatementLength() {
        return 0;
    }

    @Override
    public int getMaxStatements() {
        return 0;
    }

    @Override
    public int getMaxTableNameLength() {
        return 0;
    }

    @Override
    public int getMaxTablesInSelect() {
        return 0;
    }

    @Override
    public int getMaxUserNameLength() {
        return 0;
    }

    @Override
    public int getResultSetHoldability() {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public int getSQLStateType() {
        return sqlStateSQL99;
    }

    @Override
    public String getDatabaseProductName() {
        return "SimpleDB"; //$NON-NLS-1$
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return "2009.4.15"; // ? //$NON-NLS-1$
    }

    @Override
    public String getDriverName() {
        return "SimpleDB"; //$NON-NLS-1$
    }

    @Override
    public String getDriverVersion() {
        return "1.0"; //$NON-NLS-1$
    }

    @Override
    public String getExtraNameCharacters() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getCatalogSeparator() {
        return "."; //$NON-NLS-1$
    }

    @Override
    public String getCatalogTerm() {
        return "catalog"; //$NON-NLS-1$
    }

    @Override
    public String getSchemaTerm() {
        return "schema"; //$NON-NLS-1$
    }

    @Override
    public String getProcedureTerm() {
        return "not_implemented"; //$NON-NLS-1$
    }

    @Override
    public String getSearchStringEscape() {
        return null;
    }

    @Override
    public String getIdentifierQuoteString() {
        return "`"; //$NON-NLS-1$
    }

    @Override
    public String getSQLKeywords() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getNumericFunctions() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getStringFunctions() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getSystemFunctions() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getTimeDateFunctions() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getURL() {
        return null;
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public boolean allProceduresAreCallable() {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() {
        return true;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    @Override
    public boolean deletesAreDetected(final int type) {
        return false;
    }

    @Override
    public boolean insertsAreDetected(final int type) {
        return false;
    }

    @Override
    public boolean isCatalogAtStart() {
        return true;
    }

    @Override
    public boolean locatorsUpdateCopy() {
        return false;
    }

    @Override
    public boolean nullPlusNonNullIsNull() {
        return true;
    }

    @Override
    public boolean nullsAreSortedAtEnd() {
        return !nullsAreSortedAtStart();
    }

    @Override
    public boolean nullsAreSortedAtStart() {
        return true;
    }

    @Override
    public boolean nullsAreSortedHigh() {
        return true;
    }

    @Override
    public boolean nullsAreSortedLow() {
        return !nullsAreSortedHigh();
    }

    @Override
    public boolean othersDeletesAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(final int type) {
        return false;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    @Override
    public boolean supportsANSI92FullSQL() {
        return false;
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() {
        return true;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() {
        return true;
    }

    @Override
    public boolean supportsConvert() {
        return false;
    }

    @Override
    public boolean supportsConvert(final int fromType, final int toType) {
        return false;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return true;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() {
        return true;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() {
        return true;
    }

    @Override
    public boolean supportsCoreSQLGrammar() {
        return true;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    @Override
    public boolean supportsLimitedOuterJoins() {
        return true;
    }

    @Override
    public boolean supportsFullOuterJoins() {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return false;
    }

    @Override
    public boolean supportsGroupBy() {
        return true;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() {
        return false;
    }

    @Override
    public boolean supportsGroupByUnrelated() {
        return false;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() {
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() {
        return true;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean supportsMultipleOpenResults() {
        return false;
    }

    @Override
    public boolean supportsMultipleResultSets() {
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() {
        return true;
    }

    @Override
    public boolean supportsNamedParameters() {
        return true;
    }

    @Override
    public boolean supportsNonNullableColumns() {
        return true;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    @Override
    public boolean supportsOrderByUnrelated() {
        return false;
    }

    @Override
    public boolean supportsOuterJoins() {
        return true;
    }

    @Override
    public boolean supportsPositionedDelete() {
        return false;
    }

    @Override
    public boolean supportsPositionedUpdate() {
        return false;
    }

    @Override
    public boolean supportsResultSetConcurrency(final int t, final int c) {
        return t == ResultSet.TYPE_FORWARD_ONLY
        && c == ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean supportsResultSetHoldability(final int h) {
        return h == ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public boolean supportsResultSetType(final int t) {
        return t == ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public boolean supportsSavepoints() {
        return false;
    }

    @Override
    public boolean supportsSchemasInDataManipulation() {
        return false;
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() {
        return false;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() {
        return false;
    }

    @Override
    public boolean supportsStatementPooling() {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInComparisons() {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInExists() {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInIns() {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() {
        return false;
    }

    @Override
    public boolean supportsTableCorrelationNames() {
        return false;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(final int level) {
        return level == Connection.TRANSACTION_SERIALIZABLE;
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public boolean supportsUnion() {
        return true;
    }

    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    @Override
    public boolean updatesAreDetected(final int type) {
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() {
        return false;
    }

    @Override
    public boolean usesLocalFiles() {
        return true;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.conn.isReadOnly();
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getFunctionColumns(final String arg0, final String arg1, final String arg2,
            final String arg3) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getFunctions(final String arg0, final String arg1, final String arg2)
    throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultSet getSchemas(final String arg0, final String arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        // TODO Auto-generated method stub
        return false;
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
    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
            String tableNamePattern, String columnNamePattern)
            throws SQLException {
        return null;
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
    }
}
