/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.datatools.sqltools.data.internal.core.common.DefaultColumnDataAccessor;

import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;

public class SimpleDBIDDataAccessor extends DefaultColumnDataAccessor {
    @Override
    public boolean supportsInlineEdit() {
        return false;
    }

    @Override
    public Object read(final ResultSet rs, final int col, final int type, final boolean snippet) throws SQLException,
    IOException {
        return rs.getObject(col + 1);
    }

    @Override
    public Object deserialize(final String val, final int type) {
        return new SimpleDBItemName(val);
    }

    @Override
    public String[] writeValuesExprArgs(final PreparedStatement pst, final int start, final Object val, final int type)
    throws SQLException, IOException {
        pst.setObject(start + 1, val);
        return new String[] { argString(getLabel(val, type), type) };
    }
}
