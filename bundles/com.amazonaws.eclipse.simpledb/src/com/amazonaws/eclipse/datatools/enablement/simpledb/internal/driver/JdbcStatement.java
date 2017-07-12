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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.datatools.modelbase.sql.query.PredicateBasic;
import org.eclipse.datatools.modelbase.sql.query.QueryDeleteStatement;
import org.eclipse.datatools.modelbase.sql.query.QueryInsertStatement;
import org.eclipse.datatools.modelbase.sql.query.QuerySearchCondition;
import org.eclipse.datatools.modelbase.sql.query.QuerySelectStatement;
import org.eclipse.datatools.modelbase.sql.query.QueryUpdateStatement;
import org.eclipse.datatools.modelbase.sql.query.UpdateAssignmentExpression;
import org.eclipse.datatools.modelbase.sql.query.ValueExpressionColumn;
import org.eclipse.datatools.modelbase.sql.query.util.SQLQuerySourceFormat;
import org.eclipse.datatools.sqltools.parsers.sql.query.SQLQueryParseResult;
import org.eclipse.datatools.sqltools.parsers.sql.query.SQLQueryParserFactory;
import org.eclipse.datatools.sqltools.parsers.sql.query.SQLQueryParserManager;
import org.eclipse.emf.common.util.EList;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;

/**
 * JDBC Statement implementation for the Amazon SimpleDB. Converts queries into Amazon API calls.
 */
public class JdbcStatement implements Statement {

    private static final Pattern PATTERN_LIMIT = Pattern.compile("\\s+limit\\s+\\d+"); //$NON-NLS-1$

    private static final Pattern PATTERN_SELECT_STAR = Pattern.compile("^\\s*select\\s+\\*\\s+.*"); //$NON-NLS-1$

    private static final Pattern PATTERN_SELECT_COUNT = Pattern.compile("^\\s*select\\s+count\\s*\\(\\s*\\*\\s*\\).*"); //$NON-NLS-1$

    private static final Pattern PATTERN_WHITESPACE_END = Pattern.compile("\\s+$"); //$NON-NLS-1$

    private static final Pattern PATTERN_WHITESPACE_BEGIN = Pattern.compile("^\\s+"); //$NON-NLS-1$

    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+"); //$NON-NLS-1$

    private static final Pattern PATTERN_FROM_CLAUSE = Pattern.compile("from\\s+[\\S&&[^,]]+"); //$NON-NLS-1$

    /** Amazon SDB prefers all the identifiers in the select clause to be quoted with the given character. */
    public static final char DELIMITED_IDENTIFIER_QUOTE = '`';

    private static final int MAX_ITEMS_PER_QUERY_RESPONSE = 251;

    JdbcConnection conn;
    String sql = null;
    JdbcResultSet resultSet;

    int maxRows = 0; // max. number of rows which can be returned by this statement

    RawData data;

    /** PreparedStatement parameters or filled from usual statement upon parsing */
    List<Object> params = null;

    boolean cancel = false;

    public JdbcStatement(final JdbcConnection conn) {
        this.conn = conn;
        this.resultSet = new JdbcResultSet(this);
    }

    @Override
    public void close() throws SQLException {
        this.resultSet.close();
        this.data = new RawData();
    }

    protected final void checkOpen() throws SQLException {
        if (this.resultSet == null || !this.resultSet.isOpen()) {
            throw new SQLException("statement is closed"); //$NON-NLS-1$
        }
    }

    @Override
    public boolean execute(final String sql) throws SQLException {
        close();
        this.sql = sql;

        if (this.sql == null) {
            throw new SQLException("sql is null");
        }

        trimSQL();

        String lowcaseSql = this.sql.toLowerCase();
        if (!lowcaseSql.startsWith("select ")) {
            executeUpdate(this.sql);
            return false; // no ResultSet
        }

        if (this.sql.length() == 0) {
            throw new SQLException("empty sql");
        }

        int maxRows = getMaxRows();

        //    System.out.println("GET MAXROWS: " + maxRows);

        int limit = -1;
        boolean countQuery = PATTERN_SELECT_COUNT.matcher(lowcaseSql).matches();
        if (!countQuery) {
            // NB! Assuming here that limit word is never a part of an identifier, e.g. attribute
            Matcher m = PATTERN_LIMIT.matcher(lowcaseSql);
            if (m.find()) {
                int limitPos = m.start();
                int endPos = m.end();
                Pattern p = Pattern.compile("\\d+"); //$NON-NLS-1$
                String limitExpression = lowcaseSql.substring(limitPos, endPos);
                m = p.matcher(limitExpression);
                if (m.find()) {
                    limit = Integer.parseInt(limitExpression.substring(m.start(), m.end()).trim());
                    if (limit >= 0 && (limit < maxRows || maxRows <= 0)) {
                        maxRows = limit;
                    }
                }
            }

            if (limit < 0 && maxRows > 0) {
                this.sql += " limit " + maxRows;
            }
        } else {
            maxRows = 1;
        }

        //    System.out.println("EFFECTIVE MAXROWS: " + maxRows);

        int row = 0;
        ExecutionResult result = new ExecutionResult(null, -1);
        do {
            result = execute(this.sql, row, maxRows, MAX_ITEMS_PER_QUERY_RESPONSE, result.nextToken);
            if (result == null) {
                break;
            }
            this.resultSet.open();
            row += result.items;
            if (maxRows > 0 && row >= maxRows) { // reached the limit
                //        if (result.nextToken != null) {
                //          this.resultSet.warning = new SQLWarning("This request has exceeded the limits. The NextToken value is: "
                //              + result.nextToken);
                //        }
                break;
            }
            //      System.out.println("NEXT TOKEN: " + result.nextToken);
        } while (result.nextToken != null && result.nextToken.length() > 0);

        return true; //this.data.getRowNum() > 0;
    }

    private void extractColumnNamesFromSelect() throws SQLException {
        String sqlToParse = this.sql;
        String lowcaseSql = sqlToParse.toLowerCase();

        /*
         * If we know that the query doesn't explicitly specify column names
         * (ex: "select *" or "select count(...)") then we can optimize and bail
         * out rather than trying to parse the query.
         */
        if (PATTERN_SELECT_STAR.matcher(lowcaseSql).find() || PATTERN_SELECT_COUNT.matcher(lowcaseSql).find()) {
            return;
        }

        // strip 'limit', generic parser doesn't like it
        // NB! Assuming here that limit word is never a part of an identifier, e.g. attribute
        Matcher m = PATTERN_LIMIT.matcher(lowcaseSql);
        if (m.find()) {
            int limitPos = m.start();
            int endPos = m.end();
            sqlToParse = this.sql.substring(0, limitPos);
            if (this.sql.length() - endPos > 0) {
                sqlToParse += this.sql.substring(endPos, this.sql.length());
            }
        }

        // Convert double quotes to single quotes since the generic parser chokes on double
        // quotes but they're perfectly valid for SimpleDB
        sqlToParse = sqlToParse.replace('"', '\'');

        try {
            SQLQueryParserManager manager = createParserManager();
            SQLQueryParseResult res = manager.parseQuery(sqlToParse);
            if (res.getSQLStatement() instanceof QuerySelectStatement) {
                QuerySelectStatement stmt = (QuerySelectStatement) res.getSQLStatement();
                EList<?> columns = stmt.getQueryExpr().getQuery().getColumnList();
                if (columns != null) {
                    String[] columnNames = new String[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        Object column = columns.get(i);
                        if (!(column instanceof ValueExpressionColumn)) {
                            continue;
                        }
                        String name = ((ValueExpressionColumn) column).getName();
                        if ("*".equals(name)) { // just in case
                            continue;
                        }
                        columnNames[i] = name;
                    }

                    // validate names
                    for (String columnName : columnNames) {
                        if (columnName == null) {
                            return; // failed to get all the column names, putting it to data map will break everything
                        }
                    }

                    for (String columnName : columnNames) {
                        this.data.addAttribute(columnName);
                    }
                }
            }

        } catch (Exception e) {
            // ignore atm - most probably this is a custom query from scrapbook where column order is not important
            //      throw wrapIntoSqlException(e);
        }
    }

    private SQLQueryParserManager createParserManager() {
        SQLQueryParserManager manager = SQLQueryParserManager.getInstance();
        SQLQuerySourceFormat format = SQLQuerySourceFormat.copyDefaultFormat();
        format.setDelimitedIdentifierQuote(DELIMITED_IDENTIFIER_QUOTE);
        format.setPreserveSourceFormat(true);
        manager.configParser(format, null /*Arrays.asList(new PostParseProcessor[] { new DataTypeResolver(false) })*/);

        manager.setParserFactory(new SQLQueryParserFactory(manager.getSourceFormat()) {
            @Override
            public ValueExpressionColumn createColumnExpression(final String aColumnName) {
                //if (statementTypeOnly) {return null;}
                ValueExpressionColumn colExpr = super.createColumnExpression(aColumnName);

                colExpr.setName(convertSQLIdentifierToCatalogFormat(aColumnName, getDelimitedIdentifierQuote()));

                return colExpr;
            }
        });
        return manager;
    }

    SQLException wrapIntoSqlException(final Exception e) {
        SQLException ex;
        if (e instanceof SQLException) {
            ex = (SQLException) e;
        } else {
            ex = new SQLException(e.getLocalizedMessage());
            ex.initCause(e);
        }
        return ex;
    }

    static String convertSQLIdentifierToCatalogFormat(final String sqlIdentifier, final char idDelimiterQuote) {
        String catalogIdentifier = sqlIdentifier;

        if (sqlIdentifier != null) {
            String delimiter = String.valueOf(idDelimiterQuote);

            boolean isDelimited = sqlIdentifier.startsWith(delimiter) && sqlIdentifier.endsWith(delimiter);
            boolean containsQuotedDelimiters = sqlIdentifier.indexOf(delimiter + delimiter) > -1;

            if (isDelimited) {
                catalogIdentifier = sqlIdentifier.substring(1, sqlIdentifier.length() - 1);

                if (containsQuotedDelimiters) {
                    catalogIdentifier = catalogIdentifier.replaceAll(delimiter + delimiter, delimiter);
                }
            } else {
                catalogIdentifier = sqlIdentifier;
            }
        }

        return catalogIdentifier;
    }

    private void trimSQL() {
        this.sql = this.sql.trim();
        Matcher m = PATTERN_WHITESPACE_BEGIN.matcher(this.sql);
        if (m.find()) {
            this.sql = this.sql.substring(m.end());
        }
        m = PATTERN_WHITESPACE_END.matcher(this.sql);
        if (m.find()) {
            this.sql = this.sql.substring(0, m.start());
        }
        if (this.sql.endsWith(";")) {
            this.sql = this.sql.substring(0, this.sql.length() - 1);
        }
    }

    public String getDomainName() {
        Matcher m = PATTERN_FROM_CLAUSE.matcher(this.sql);
        if (m.find()) {
            String fromExpression = this.sql.substring(m.start(), m.end());
            m = PATTERN_WHITESPACE.matcher(fromExpression);
            if (m.find()) {
                String domainName = convertSQLIdentifierToCatalogFormat(fromExpression.substring(m.end()),
                        DELIMITED_IDENTIFIER_QUOTE);
                return domainName;
            }
        }
        return null;
    }

    /*
     * Collect as many items as SimpleDB allows and return the NextToken, which
     * is used to continue the query in a subsequent call to SimpleDB.
     */
    ExecutionResult execute(final String queryText, final int startingRow, final int maxRows, final int requestSize,
            final String nextToken) throws SQLException {
        if (this.data.getPersistedColumnNum() == 0) {
            extractColumnNamesFromSelect();
        }

        //    System.out.println("FINAL QUERY: " + queryText);
        SelectRequest request = new SelectRequest();
        request.setSelectExpression(queryText);

        if (nextToken != null) {
            request.setNextToken(nextToken);
        }

        SelectResult queryResult;
        try {
            queryResult = this.conn.getClient().select(request);
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }


        boolean shouldAddItemName = this.data.getPersistedColumnNum() == 0
        || this.data.getAttributes().contains(SimpleDBItemName.ITEM_HEADER);

        int row = startingRow;
        //      List<GetAttributesResponse> responses = new ArrayList<GetAttributesResponse>();
        for (Item item : queryResult.getItems()) {
            if (this.cancel) {
                break;
            }

            if (shouldAddItemName) {
                this.data.addItemName(item.getName(), row);
            }

            List<Attribute> attributes = item.getAttributes();
            for (Attribute attr : attributes) {
                this.data.add(attr.getName(), attr.getValue(), row);
            }
            if (attributes.isEmpty()) {
                this.data.ensureRows(row);
            }

            //        GetAttributesRequest aRequest = new GetAttributesRequest();
            //        aRequest.setItemName(item.getName());
            //        aRequest.setDomainName(getDomainName() /*request.getDomainName()*/);
            //        try {
            //          responses.add(this.conn.service.getAttributes(aRequest));
            //        } catch (Exception e) {
            //          throw wrapIntoSqlException(e);
            //        }

            row++;
        }

        //      row = startingRow;
        //      for (GetAttributesResponse aResponse : responses) {
        //        if (this.cancel) {
        //          break;
        //        }
        //
        //        try {
        //          GetAttributesResult aResult = aResponse.getGetAttributesResult();
        //          for (Attribute attribute : aResult.getAttribute()) {
        //            this.data.add(attribute.getName(), attribute.getValue(), row);
        //          }
        //        } catch (Exception e) {
        //          throw wrapIntoSqlException(e);
        //        }
        //
        //        row++;
        //      }

        String newNextToken = queryResult.getNextToken();
        return new ExecutionResult(newNextToken, row - startingRow);

    }

    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        if (execute(sql)) {
            return getResultSet();
        } else {
            throw new SQLException("query didn't return a ResultSet");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public int executeUpdate(final String inSql) throws SQLException {
        this.sql = inSql;
        if (this.sql == null) {
            throw new SQLException("sql is null");
        }

        trimSQL();

        if (this.sql.length() == 0) {
            throw new SQLException("empty sql");
        }

        String lowcaseSql = this.sql.toLowerCase();

        Object req = null;
        // TODO use patterns
        if (lowcaseSql.startsWith("create domain") || lowcaseSql.startsWith("create table")) { //$NON-NLS-1$
            int pos = this.sql.lastIndexOf(" ");
            String domain = convertSQLIdentifierToCatalogFormat(this.sql.substring(pos + 1).trim(),
                    DELIMITED_IDENTIFIER_QUOTE);
            req = new CreateDomainRequest().withDomainName(domain);
        } else if (lowcaseSql.startsWith("delete domain") || lowcaseSql.startsWith("delete table") //$NON-NLS-1$
                || lowcaseSql.startsWith("drop table")) {
            int pos = this.sql.lastIndexOf(" ");
            String domain = convertSQLIdentifierToCatalogFormat(this.sql.substring(pos + 1).trim(),
                    DELIMITED_IDENTIFIER_QUOTE);
            List<String> pending = this.conn.getPendingColumns(domain);
            if (pending != null) {
                pending = new ArrayList<>(pending);
                for (String attr : pending) {
                    this.conn.removePendingColumn(domain, attr);
                }
            }
            req = new DeleteDomainRequest().withDomainName(domain);
        } else if (lowcaseSql.startsWith("delete from")) {
            req = prepareDeleteRowRequest();
        } else if (lowcaseSql.startsWith("alter table ")) {
            req = prepareDropAttributeRequest();
        } else if (lowcaseSql.startsWith("insert ")) {
            req = prepareInsertRequest();
        } else if (lowcaseSql.startsWith("update ")) {
            req = prepareUpdateRequest();
        } else if (lowcaseSql.startsWith("create testdomain ")) {
            req = new ArrayList<>();

            String domain = convertSQLIdentifierToCatalogFormat(this.sql.substring(this.sql.lastIndexOf(" ") + 1).trim(), //$NON-NLS-1$
                    DELIMITED_IDENTIFIER_QUOTE);
            ((List<Object>) req).add(new CreateDomainRequest().withDomainName(domain));

            ReplaceableAttribute attr  = new ReplaceableAttribute().withName("attr1").withValue("val1").withReplace(Boolean.TRUE);
            for (int i = 0; i < 570; i++) {
                ((List<Object>) req).add(new PutAttributesRequest().withDomainName(domain).withItemName("item" + i).withAttributes(attr));
            }
        }

        if (req != null) {
            int result = executeSDBRequest(req);
            if (this.params != null) {
                for (Object obj : this.params) {
                    if (obj instanceof SimpleDBItemName) {
                        ((SimpleDBItemName) obj).setPersisted(true);
                    }
                }
            }
            return result;
        }

        throw new SQLException("unsupported update: " + this.sql);
    }

    @SuppressWarnings("unchecked")
    int executeSDBRequest(final Object req) throws SQLException {
        try {
            if (req == null) {
                // do nothing
                return 0;
            } else if (req instanceof Collection) {
                int sum = 0;
                for (Object singleReq : (Collection<Object>) req) {
                    sum += executeSDBRequest(singleReq);
                }
                return sum;
            } else if (req instanceof CreateDomainRequest) {
                this.conn.getClient().createDomain((CreateDomainRequest) req);
                return 0;
            } else if (req instanceof DeleteDomainRequest) {
                this.conn.getClient().deleteDomain((DeleteDomainRequest) req);
                return 0;
            } else if (req instanceof PutAttributesRequest) {
                this.conn.getClient().putAttributes((PutAttributesRequest) req);
                return 1;
            } else if (req instanceof BatchPutAttributesRequest) {
                this.conn.getClient().batchPutAttributes((BatchPutAttributesRequest) req);
                return ((BatchPutAttributesRequest) req).getItems().size();
            } else if (req instanceof DeleteAttributesRequest) {
                this.conn.getClient().deleteAttributes((DeleteAttributesRequest) req);
                List<Attribute> attribute = ((DeleteAttributesRequest) req).getAttributes();
                return attribute == null || attribute.isEmpty() ? 1 : 0;
            } else {
                throw new SQLException("unsupported query");
            }
        } catch (AmazonServiceException e) {
            throw wrapIntoSqlException(e);
        }
    }

    List<Object> prepareUpdateRequest() throws SQLException {
        if (this.sql.toLowerCase().indexOf(" set ") < 0) { // workaround for DTP bug - sends update statements without set of any columns
            return new ArrayList<>();
        }

        try {
            SQLQueryParserManager manager = createParserManager();
            SQLQueryParseResult res = manager.parseQuery(this.sql);
            QueryUpdateStatement qs = (QueryUpdateStatement) res.getQueryStatement();

            QuerySearchCondition whereClause = qs.getWhereClause();
            if (!(whereClause instanceof PredicateBasic)) {
                throw new SQLException("current SDB JDBC version supports only simple expression `" //$NON-NLS-1$
                        + SimpleDBItemName.ITEM_HEADER + "`='<something>' in WHERE clause");
            }

            if (this.params == null) {
                // TODO some time later extract the parameters from the parsed simple statement
            }

            if (this.params == null) {
                throw new SQLException("current SDB JDBC version supports only parameterized queries");
            }

            String domain = qs.getTargetTable().getName();
            String item = unwrapItemValue(this.params.get(this.params.size() - 1));

            EList<?> assignmentClause = qs.getAssignmentClause();

            if (this.params != null && this.params.size() - 1 != assignmentClause.size()) { // last param is an Item name, thus -1
                throw new SQLException("number of set params doesn't match");
            }

            int tally = 0;
            List<ReplaceableAttribute> attrs = new ArrayList<>();
            for (Object assign : assignmentClause) {
                UpdateAssignmentExpression assignExp = (UpdateAssignmentExpression) assign;
                EList<?> cols = assignExp.getTargetColumnList();
                ValueExpressionColumn col = (ValueExpressionColumn) cols.get(0);
                String colName = col.getName();
                String colValue = (String) this.params.get(tally);
                if (colValue != null) {
                    ReplaceableAttribute attr = new ReplaceableAttribute().withName(colName).withValue(colValue).withReplace(Boolean.TRUE);
                    attrs.add(attr);
                }
                ++tally;
            }

            tally = 0;
            List<Attribute> deleteAttrs = new ArrayList<>();
            for (Object assign : assignmentClause) {
                UpdateAssignmentExpression assignExp = (UpdateAssignmentExpression) assign;
                EList<?> cols = assignExp.getTargetColumnList();
                ValueExpressionColumn col = (ValueExpressionColumn) cols.get(0);
                String colName = col.getName();
                if (SimpleDBItemName.ITEM_HEADER.equals(colName)) { // TODO how we could use ColumnType here instead of hardcoded ColumnName?
                        throw new SQLException("item name cannot be edited once created");
                }
                String colValue = (String) this.params.get(tally);
                if (colValue == null) {
                    Attribute attr = new Attribute().withName(colName).withValue(colValue);
                    deleteAttrs.add(attr);
                }
                ++tally;
            }

            List<Object> reqs = new ArrayList<>();

            if (!attrs.isEmpty()) {
                PutAttributesRequest req = new PutAttributesRequest().withDomainName(domain).withItemName(item);
                req.setAttributes(attrs);
                reqs.add(req);
            }

            if (!deleteAttrs.isEmpty()) {
                DeleteAttributesRequest dreq = new DeleteAttributesRequest().withDomainName(domain).withItemName(item);
                dreq.setAttributes(deleteAttrs);
                reqs.add(dreq);
            }

            return reqs;
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }
    }

    PutAttributesRequest prepareInsertRequest() throws SQLException {
        try {
            SQLQueryParserManager manager = createParserManager();
            SQLQueryParseResult res = manager.parseQuery(this.sql);
            QueryInsertStatement qs = (QueryInsertStatement) res.getQueryStatement();

            String domain = qs.getTargetTable().getName();

            if (this.params == null) {
                // TODO some time later extract the parameters from the parsed simple statement
            }

            if (this.params == null) {
                throw new SQLException("current SDB JDBC version supports only parameterized queries");
            }

            EList<?> targetColumns = qs.getTargetColumnList();
            //      ValuesRow values = (ValuesRow)qs.getSourceValuesRowList().get(0);

            if (this.params != null && this.params.size() != targetColumns.size()) {
                throw new SQLException("number of set params doesn't match");
            }

            int tally = 0;
            String item = null;
            List<ReplaceableAttribute> attrs = new ArrayList<>();
            for (Object assign : targetColumns) {
                ValueExpressionColumn col = (ValueExpressionColumn) assign;
                String colName = col.getName();
                if (tally == 0 && !SimpleDBItemName.ITEM_HEADER.equals(colName)) {
                    throw new SQLException("first parameter must be " + DELIMITED_IDENTIFIER_QUOTE + SimpleDBItemName.ITEM_HEADER //$NON-NLS-1$
                            + DELIMITED_IDENTIFIER_QUOTE);
                }
                Object colValue = this.params.get(tally);
                if (colValue != null) {
                    if (SimpleDBItemName.ITEM_HEADER.equals(colName)) { // TODO how we could use ColumnType here instead of hardcoded ColumnName?
                        item = unwrapItemValue(colValue);
                    } else {
                        if (colValue instanceof String[]) {
                            for (String val : (String[]) colValue) {
                                ReplaceableAttribute attr = new ReplaceableAttribute().withName(colName).withValue(val).withReplace(Boolean.TRUE);
                                attrs.add(attr);
                            }
                        } else {
                            ReplaceableAttribute attr = new ReplaceableAttribute().withName(colName).withValue((String) colValue).withReplace(Boolean.TRUE);
                            attrs.add(attr);
                        }
                    }
                }
                ++tally;
            }

            PutAttributesRequest req = new PutAttributesRequest().withDomainName(domain).withItemName(item);
            req.setAttributes(attrs);
            return req;
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }
    }

    Object prepareDeleteRowRequest() throws SQLException {
        try {
            SQLQueryParserManager manager = createParserManager();
            SQLQueryParseResult res = manager.parseQuery(this.sql);
            QueryDeleteStatement qs = (QueryDeleteStatement) res.getQueryStatement();

            String domain = qs.getTargetTable().getName();

            if (this.params == null) {
                // TODO some time later extract the parameters from the parsed simple statement
            }

            if (this.params == null) {
                throw new SQLException("current SDB JDBC version supports only parameterized queries");
            }

            Object firstParam = this.params.get(0);
            String item = unwrapItemValue(firstParam);

            DeleteAttributesRequest req = new DeleteAttributesRequest().withDomainName(domain).withItemName(item);
            return req;
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }
    }

    Object prepareDropAttributeRequest() throws SQLException {
        try {
            //      SQLQueryParserManager manager = SQLQueryParserManager.getInstance();
            //      SQLQuerySourceFormat format = SQLQuerySourceFormat.copyDefaultFormat();
            //      format.setDelimitedIdentifierQuote('`');
            //      manager.configParser(format, null);
            //
            //      SQLQueryParseResult res = manager.parseQuery(this.sql);
            //      QueryDeleteStatement qs = (QueryDeleteStatement) res.getQueryStatement();
            //
            //      String domain = qs.getTargetTable().getName();

            // TODO use patterns
            if (!this.sql.startsWith("alter table") || this.sql.indexOf(" drop ") < 0) { //$NON-NLS-1$
                throw new SQLException("unsupported alter table statement");
            }

            int pos = this.sql.indexOf(" ", "alter table ".length() + 1); //$NON-NLS-1$ //$NON-NLS-2$
            String domain = convertSQLIdentifierToCatalogFormat(this.sql.substring("alter table ".length(), pos).trim(), //$NON-NLS-1$
                    DELIMITED_IDENTIFIER_QUOTE);

            pos = this.sql.indexOf("drop "); //$NON-NLS-1$
            String attrName = convertSQLIdentifierToCatalogFormat(this.sql.substring(pos + "drop ".length()).trim(),
                    DELIMITED_IDENTIFIER_QUOTE);

            this.conn.removePendingColumn(domain, attrName);

            Attribute attr = new Attribute().withName(attrName).withValue(null);
            List<Attribute> attrs = new ArrayList<>();
            attrs.add(attr);

            this.sql = "select itemName from " + DELIMITED_IDENTIFIER_QUOTE + domain + DELIMITED_IDENTIFIER_QUOTE //$NON-NLS-1$
            + " where " + DELIMITED_IDENTIFIER_QUOTE + attrName + DELIMITED_IDENTIFIER_QUOTE + " is not null";

            ResultSet rs = executeQuery(this.sql);

            List<DeleteAttributesRequest> reqs = new ArrayList<>();
            while (rs.next()) {
                String item = rs.getString(1);
                DeleteAttributesRequest dar = new DeleteAttributesRequest().withDomainName(domain).withItemName(item);
                dar.setAttributes(attrs);
                reqs.add(dar);
            }

            return reqs;
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private String unwrapItemValue(final Object param) {
        String item;
        if (param instanceof SimpleDBItemName) {
            item = ((SimpleDBItemName) param).getItemName();
        } else if (param instanceof Collection) {
            item = ((Collection<String>) param).iterator().next();
        } else if (param instanceof String[]) {
            item = ((String[]) param)[0];
        } else {
            item = (String) param;
        }
        return item;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return this.resultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return -1; // we return ResultSet
    }

    @Override
    public void setCursorName(final String name) throws SQLException {
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.conn;
    }

    @Override
    public void cancel() throws SQLException {
        //    this.resultSet.checkOpen();
        this.cancel = true;
    }

    @Override
    public int getMaxRows() throws SQLException {
        return this.maxRows;
    }

    @Override
    public void setMaxRows(final int maxRows) throws SQLException {
        //    System.out.println("SETTING MAXROWS: " + maxRows);
        if (maxRows < 0) {
            throw new SQLException("max row count must be >= 0"); //$NON-NLS-1$
        }
        this.maxRows = maxRows;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(final int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("max field size " + max + " cannot be negative"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        return this.resultSet.getFetchSize();
    }

    @Override
    public void setFetchSize(final int r) throws SQLException {
        this.resultSet.setFetchSize(r);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return this.resultSet.getFetchDirection();
    }

    @Override
    public void setFetchDirection(final int d) throws SQLException {
        this.resultSet.setFetchDirection(d);
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return getMoreResults(0);
    }

    @Override
    public boolean getMoreResults(final int c) throws SQLException {
        //    checkOpen();
        close();
        return false;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return getResultSet().getConcurrency();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return getResultSet().getType();
    }

    @Override
    public void setEscapeProcessing(final boolean enable) {
    }

    // NOT SUPPORTED ////////////////////////////////////////////////////////////

    @Override
    public int getQueryTimeout() throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
        //    return this.timeout;
    }

    @Override
    public void setQueryTimeout(final int seconds) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
        //    if (seconds < 0) {
        //      throw new SQLException("query timeout must be >= 0");
        //    }
        //    this.timeout = seconds;
    }

    @Override
    public void addBatch(final String sql) throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public void clearBatch() throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLException("unsupported by SDB yet"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(final String sql, final int[] colinds) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(final String sql, final String[] colnames) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public int executeUpdate(final String sql, final int autoKeys) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public int executeUpdate(final String sql, final int[] colinds) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public int executeUpdate(final String sql, final String[] cols) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(final String sql, final int autokeys) throws SQLException {
        throw new SQLException("unsupported by SDB"); //$NON-NLS-1$
    }

    // INNER CLASSES ////////////////////////////////////////////////////////////

    /**
     * Collects item information in the format given by SimpleDB and aggregates it into a tabular format.
     */
    class RawData {

        // A list of rows, where each row is a map of columnIndex to values
        private List<Map<Integer, List<String>>> rows;

        // A list of column names (attributes)
        private List<String> columns;

        private List<Integer> itemNameColumn;

        /**
         * Constructor
         */
        public RawData() {
            this.rows = new ArrayList<>();
            this.columns = new ArrayList<>();
            this.itemNameColumn = new ArrayList<>();
        }

        /**
         * Returns the attribute values at the given row and column.
         *
         * @param row
         *          Corresponds to the n'th item in the result
         * @param column
         *          Corresponds to the n'th overall attribute. The attribute may or may not apply to this item.
         * @return A list of values or null if the attribute doesn't apply to this item.
         */
        public List<String> get(final int row, final int column) {
            if (this.rows.size() > row) {
                return this.rows.get(row).get(column);
            } else {
                return null;
            }
        }

        /**
         * A convenience method to return a delimited string of the attribute value at the specified row and column.
         *
         * @param row
         * @param column
         * @param delimiter
         * @return found value; multi-value separated by given delimiter
         * @see #get(int,int)
         */
        public String getString(final int row, final int column, final String delimiter) {
            return join(get(row, column), delimiter);
        }

        /**
         * Add the given value as an item name value at the given row. A new column is added to the table if there was no
         * such yet.
         *
         * @param value
         * @param rowNum
         */
        public void addItemName(final String value, final int rowNum) {
            ensureItemNameColumn(rowNum);
            int column = add(SimpleDBItemName.ITEM_HEADER, value, rowNum);
            this.itemNameColumn.set(rowNum, column);
        }

        public boolean isItemNameColumn(final int row, final int column) {
            if (row < 0 || row >= this.rows.size()) {
                List<String> attrs = getAttributes();
                return !attrs.isEmpty() && SimpleDBItemName.ITEM_HEADER.equals(attrs.get(column));
            }
            ensureItemNameColumn(row);
            Integer itemName = this.itemNameColumn.get(row);
            return itemName != null && itemName.intValue() == column;
        }

        public int getItemNameColumn(final int row) {
            if (row < 0 || row >= this.rows.size()) {
                List<String> attrs = getAttributes();
                return attrs.indexOf(SimpleDBItemName.ITEM_HEADER);
            }
            ensureItemNameColumn(row);
            Integer itemName = this.itemNameColumn.get(row);
            return itemName != null ? itemName.intValue() : -1;
        }

        private void ensureItemNameColumn(final int row) {
            for (int i = this.itemNameColumn.size() - 1; i < row; i++) {
                this.itemNameColumn.add(null);
            }
        }

        public int addAttribute(final String attribute) {
            int column = this.columns.indexOf(attribute);
            if (column < 0) {
                column = this.columns.size();
                this.columns.add(attribute);
            }
            return column;
        }

        /**
         * Add the given attribute/value pair at the given row. The attribute may already exist, in which case the value is
         * added to the list of existing attributes. Otherwise, a new column is added to the table.
         *
         * @param attribute
         * @param value
         * @param rowNum
         * @return index of the attribute
         */
        public int add(final String attribute, final String value, final int rowNum) {
            int column = this.columns.indexOf(attribute);
            if (column < 0) {
                column = this.columns.size();
                this.columns.add(attribute);
                JdbcStatement.this.conn.removePendingColumn(getDomainName(), attribute); // real data from SDB came, safe to remove the pending column
            }

            ensureRows(rowNum);
            Map<Integer, List<String>> row = this.rows.get(rowNum);

            List<String> values = row.get(column);
            if (values == null) {
                values = new ArrayList<>();
                row.put(column, values);
            }

            values.add(value);

            return column;
        }

        public void ensureRows(final int rowNum) {
            for (int i = this.rows.size() - 1; i < rowNum; i++) {
                this.rows.add(new HashMap<Integer, List<String>>());
            }
        }

        /**
         * @return The number of rows/items in the query
         */
        public int getRowNum() {
            return this.rows.size();
        }

        /**
         * @return The number of columns/attributes in the query
         */
        public int getColumnNum() {
            int size = this.columns.size();

            List<String> pendings = JdbcStatement.this.conn.getPendingColumns(JdbcStatement.this.getDomainName());
            if (pendings != null && !pendings.isEmpty() && this.columns.isEmpty()) {
                ++size; // +1 for ItemName - special case when there is just freshly added attributes and there is no content in the domain
            }
            if (pendings != null) {
                pendings = new ArrayList<>(pendings);
                pendings.removeAll(this.columns);
                size += pendings.size();
            }

            return size;
        }

        /**
         * @return The number of columns/attributes existing in the SDB, i.e. no pending columns
         */
        public int getPersistedColumnNum() {
            return this.columns.size();
        }

        /**
         * @return A list of attributes in the order as they exist in the table
         */
        public List<String> getAttributes() {
            ArrayList<String> attrs = new ArrayList<>(this.columns);

            List<String> pendings = JdbcStatement.this.conn.getPendingColumns(JdbcStatement.this.getDomainName());
            if (pendings != null && !pendings.isEmpty() && this.columns.isEmpty()) {
                attrs.add(SimpleDBItemName.ITEM_HEADER); // special case when there is just freshly added attributes and there is no content in the domain
            }
            if (pendings != null) {
                pendings = new ArrayList<>(pendings);
                pendings.removeAll(this.columns);
                attrs.addAll(pendings);
            }

            return attrs;
        }

        /*
         * Private interface
         */

        /*
         * Join the items in a Collection of Strings with the given delimiter
         */
        private String join(final Collection<String> s, final String delimiter) {
            if (s == null) {
                return ""; //$NON-NLS-1$
            }

            StringBuilder builder = new StringBuilder();
            Iterator<String> iter = s.iterator();
            while (iter.hasNext()) {
                builder.append(iter.next());
                if (iter.hasNext()) {
                    builder.append(delimiter);
                }
            }
            return builder.toString();
        }

        // TODO reimplement one day to use map
        /* @return index (starts from 0) of the attribute with the given name */
        public int findAttribute(final String name) throws SQLException {
            int c = -1;
            for (int i = 0; i < this.columns.size(); i++) {
                String cur = this.columns.get(i);
                if (name.equalsIgnoreCase(cur)
                        || (cur.toUpperCase().endsWith(name.toUpperCase()) && cur.charAt(cur.length() - name.length()) == '.')) {
                    if (c == -1) {
                        c = i;
                    } else {
                        throw new SQLException("ambiguous column: '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
            if (c == -1) {
                List<String> pendings = JdbcStatement.this.conn.getPendingColumns(JdbcStatement.this.getDomainName());
                if (pendings != null) {
                    pendings = new ArrayList<>(pendings);
                    pendings.removeAll(this.columns);

                    for (int i = 0; i < pendings.size(); i++) {
                        String cur = pendings.get(i);
                        if (name.equalsIgnoreCase(cur)
                                || (cur.toUpperCase().endsWith(name.toUpperCase()) && cur.charAt(cur.length() - name.length()) == '.')) {
                            if (c == -1) {
                                c = i + Math.max(this.columns.size(), 1); // 1 - special case when there is just freshly added attributes and there is no content in the domain
                            } else {
                                throw new SQLException("ambiguous column: '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                            }
                        }
                    }
                }
            }
            if (c == -1) {
                throw new SQLException("no such column: '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                return c;
            }
        }
    }

    /*
     * Bundle the NextToken and number of items fetched in the last query.
     */
    static class ExecutionResult {
        public ExecutionResult(final String nextToken, final int items) {
            this.nextToken = nextToken;
            this.items = items;
        }

        public final String nextToken;
        public final int items;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setPoolable(final boolean poolable) throws SQLException {
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
    public void closeOnCompletion() throws SQLException {
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }
}
