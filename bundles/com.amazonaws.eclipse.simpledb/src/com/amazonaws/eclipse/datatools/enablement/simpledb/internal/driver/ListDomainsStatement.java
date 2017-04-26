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

import java.sql.SQLException;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.JdbcConnection;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;

/**
 * Fetches domain names from the Amazon SimpleDB belonging to the logged in user.
 */
public class ListDomainsStatement extends JdbcPreparedStatement {

    public ListDomainsStatement(final JdbcConnection conn, final String sql) {
        super(conn, sql);
    }

    /*
     * Collect as many items as SimpleDB allows and return the NextToken, which
     * is used to continue the query in a subsequent call to SimpleDB.
     */
    @Override
    ExecutionResult execute(final String queryText, final int startingRow, final int maxRows, final int requestSize,
            final String nextToken) throws SQLException {
        ListDomainsRequest request = new ListDomainsRequest();

        if (maxRows > 0) {
            request.setMaxNumberOfDomains(Math.min(maxRows, 100));
        }

        if (nextToken != null) {
            request.setNextToken(nextToken);
        }

        ListDomainsResult queryResult;
        try {
            queryResult = this.conn.getClient().listDomains(request);
        } catch (Exception e) {
            throw wrapIntoSqlException(e);
        }

        String domainFilter = null;
        int pos = this.sql.lastIndexOf('\'');
        if (pos >= 0) {
            int pos2 = this.sql.lastIndexOf('\'', pos - 1);
            if (pos2 >= 0) {
                domainFilter = this.sql.substring(pos2 + 1, pos);
                if ("%".equals(domainFilter)) { //$NON-NLS-1$
                    domainFilter = null;
                }
            }
        }

        int row = startingRow;
        for (String domain : queryResult.getDomainNames()) {
            if (this.cancel) {
                break;
            }

            if (domainFilter != null && !domain.equalsIgnoreCase(domainFilter)) {
                continue;
            }

            //        *  <LI><B>TABLE_CAT</B> String => table catalog (may be <code>null</code>)
            //        *  <LI><B>TABLE_SCHEM</B> String => table schema (may be <code>null</code>)
            //        *  <LI><B>TABLE_NAME</B> String => table name
            //        *  <LI><B>TABLE_TYPE</B> String => table type.  Typical types are "TABLE",
            //        *      "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
            //        *      "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
            //        *  <LI><B>REMARKS</B> String => explanatory comment on the table
            //        *  <LI><B>TYPE_CAT</B> String => the types catalog (may be <code>null</code>)
            //        *  <LI><B>TYPE_SCHEM</B> String => the types schema (may be <code>null</code>)
            //        *  <LI><B>TYPE_NAME</B> String => type name (may be <code>null</code>)
            //        *  <LI><B>SELF_REFERENCING_COL_NAME</B> String => name of the designated
            //        *                  "identifier" column of a typed table (may be <code>null</code>)
            //        *  <LI><B>REF_GENERATION</B> String => specifies how values in
            //        *                  SELF_REFERENCING_COL_NAME are created. Values are
            //        *                  "SYSTEM", "USER", "DERIVED". (may be <code>null</code>)
            //        *  </OL>

            this.data.add("TABLE_CAT", "", row); //$NON-NLS-1$ //$NON-NLS-2$
            this.data.add("TABLE_SCHEM", "", row); //$NON-NLS-1$ //$NON-NLS-2$
            this.data.add("TABLE_NAME", domain, row); //$NON-NLS-1$
            this.data.add("TABLE_TYPE", "TABLE", row); //$NON-NLS-1$ //$NON-NLS-2$
            this.data.add("REMARKS", null, row); //$NON-NLS-1$
            this.data.add("TYPE_CAT", null, row); //$NON-NLS-1$
            this.data.add("TYPE_SCHEM", null, row); //$NON-NLS-1$
            this.data.add("TYPE_NAME", null, row); //$NON-NLS-1$
            this.data.add("SELF_REFERENCING_COL_NAME", null, row); //$NON-NLS-1$
            this.data.add("REF_GENERATION", "USER", row); //$NON-NLS-1$ //$NON-NLS-2$

            row++;
        }

        return new ExecutionResult(queryResult.getNextToken(), row - startingRow);

    }
}
