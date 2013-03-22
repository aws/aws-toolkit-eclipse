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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jmock.integration.junit3.MockObjectTestCase;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;


public class JdbcStatementTest extends MockObjectTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSimpleUpdate() throws Exception {
        String[] params = new String[] { "222", "myitem" };
        String[][] attrs = new String[][] { new String[] { "111", "222" } };
        assertUpdate("update mydomain set `111`=? where `" + SimpleDBItemName.ITEM_HEADER + "`=?", params, "mydomain",
                "myitem", attrs);
    }

    public void testComplexUpdate() throws Exception {
        String[] params = new String[] { "1111", "2222", "3333", "myitem" };
        String[][] attrs = new String[][] { new String[] { "111", "1111" }, new String[] { "222", "2222" },
                new String[] { "333", "3333" } };
        assertUpdate("update mydomain set `111`=?, `222`=?, `333`=? where `" + SimpleDBItemName.ITEM_HEADER + "`=?",
                params, "mydomain", "myitem", attrs);
    }

    public void testNonPreparedUpdate() throws Exception {
        try {
            String[][] attrs = new String[][] { new String[] { "111", "1111" }, new String[] { "222", "2222" } };
            assertUpdate("update mydomain set `111`='1111', `222`='2222' where `" + SimpleDBItemName.ITEM_HEADER
                    + "`='myitem'", null, "mydomain", "myitem", attrs);
            fail();
        } catch (SQLException e) {
            // ok, simple statements not supported yet
        }
    }

    public void testDeletingAttributes() throws Exception {
        String[] params = new String[] { "1111", null, "myitem" };
        String[][] attrs = new String[][] { new String[] { "111", "1111" } };
        List<Object> reqs = assertUpdate("update mydomain set `111`=?, `222`=? where `" + SimpleDBItemName.ITEM_HEADER
                + "`=?", params, "mydomain", "myitem", attrs);
        assertEquals(2, reqs.size());
        Object req1 = reqs.get(1);
        assertTrue(req1 instanceof DeleteAttributesRequest);
        assertEquals(1, ((DeleteAttributesRequest) req1).getAttributes().size());
        assertEquals("222", ((DeleteAttributesRequest) req1).getAttributes().get(0).getName());
    }

    public void testSimpleInsert() throws Exception {
        String[] params = new String[] { "myitem", "222" };
        String[][] attrs = new String[][] { new String[] { "111", "222" } };
        assertUpdate("insert into `mydomain` (`" + SimpleDBItemName.ITEM_HEADER + "`, `111`) values(?, ?)", params,
                "mydomain", "myitem", attrs);
    }

    public void testDeleteRow() throws Exception {
        String[] params = new String[] { "myitem" };
        List<Object> reqs = assertUpdate("delete from `mydomain` where `" + SimpleDBItemName.ITEM_HEADER + "`=?", params,
                "mydomain", "myitem", null);
        assertEquals(1, reqs.size());
        Object req = reqs.get(0);
        assertTrue(req instanceof DeleteAttributesRequest);
        List<Attribute> attribute = ((DeleteAttributesRequest) req).getAttributes();
        assertTrue(attribute == null || attribute.isEmpty());
    }

    private List<Object> assertUpdate(final String sql, final String[] params, final String domain, final String item,
            final String[][] setAttrs) throws SQLException {
        final List<Object> reqHolder = new ArrayList<Object>();
        JdbcStatement stmt;
        if (params != null) {
            stmt = new JdbcPreparedStatement(null, sql) {
                @Override
                int executeSDBRequest(final Object req) throws SQLException {
                    if (req instanceof Collection) {
                        return super.executeSDBRequest(req);
                    }
                    reqHolder.add(req);
                    return 0;
                }
            };
            int tally = 1;
            for (String param : params) {
                ((JdbcPreparedStatement) stmt).setObject(tally++, param);
            }
        } else {
            stmt = new JdbcStatement(null) {
                @Override
                int executeSDBRequest(final Object req) throws SQLException {
                    if (req instanceof Collection) {
                        return super.executeSDBRequest(req);
                    }
                    reqHolder.add(req);
                    return 0;
                }
            };
        }
        stmt.executeUpdate(sql);

        for (Object req : reqHolder) {
            if (req instanceof PutAttributesRequest) {
                PutAttributesRequest pareq = (PutAttributesRequest) req;

                assertEquals(domain, pareq.getDomainName().toLowerCase());
                assertEquals(item, pareq.getItemName());
                assertEquals(setAttrs.length, pareq.getAttributes().size());
                int tally = 0;
                for (String[] attr : setAttrs) {
                    assertEquals(attr[0], pareq.getAttributes().get(tally).getName());
                    assertEquals(attr[1], pareq.getAttributes().get(tally).getValue());
                    ++tally;
                }
            }
        }

        return reqHolder;
    }
}
