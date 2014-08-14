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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.datatools.sqltools.data.internal.core.common.DefaultColumnDataAccessor;
import org.eclipse.datatools.sqltools.data.internal.core.common.data.PreparedStatementWriter;
import org.eclipse.datatools.sqltools.result.ui.ResultsViewUIPlugin;
import org.eclipse.jface.preference.IPreferenceStore;

public class SimpleDBDataAccessor extends DefaultColumnDataAccessor {

    /**
     * If val is String[] or Collection returns true, otherwise returns super.isSnippet(...) This is to disable in cell
     * editing for multiple attribute columns.
     *
     * @see org.eclipse.datatools.sqltools.data.internal.core.common.DefaultColumnDataAccessor#isSnippet(java.lang.Object,
     *      int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isSnippet(final Object val, final int type) {
        if (val == null) {
            return false;
        }

        if (val instanceof Collection) {
            return true;
        }

        if (val instanceof String[]) {
            return true;
        }

        return super.isSnippet(val, type);
    }

    /**
     * Converts val to readable string if val is instance of String[] otherwise returns super.getLabel(...)
     *
     * @see org.eclipse.datatools.sqltools.data.internal.core.common.DefaultColumnDataAccessor#getLabel(java.lang.Object,
     *      int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getLabel(final Object val, final int type) {
        if (val == null) {
            IPreferenceStore store = ResultsViewUIPlugin.getDefault().getPreferenceStore();
            return store.getString("org.eclipse.datatools.sqltools.result.preferences.display.nulldisplaystr"); // org.eclipse.datatools.sqltools.result.internal.ui.PreferenceConstants.SQL_RESULTS_VIEW_NULL_STRING //$NON-NLS-1$
        }

        if (val instanceof String[]) {
            return Arrays.toString((String[]) val);
        }
        if (val instanceof LinkedList && ((List) val).size() == 1) { // ID - single name
            List<String> values = (List<String>) val;
            return values.get(0);
        }
        if (val instanceof ArrayList) { // multi-value column - draw in [] brackets
            return val.toString();
        }
        return super.getLabel(val, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String[] writeSetAssArgs(final PreparedStatement pst, final int start, Object val, final int type)
    throws SQLException, IOException {
        if (val instanceof List) {
            List<String> values = (List<String>) val;
            val = values.toArray(new String[values.size()]);
        }
        if (val instanceof String[]) {
            String[] values = (String[]) val;
            String[] result = new String[values.length];
            int tally = 0;
            for (String singleVal : values) {
                PreparedStatementWriter.write(pst, start + tally, type, singleVal);
                result[tally++] = argString(getLabel(singleVal, type), type);
            }
            return result;
        } else {
            return super.writeSetAssArgs(pst, start, val, type);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String[] writeWhereCondArgs(final PreparedStatement pst, final int start, final Object val, final int type)
    throws SQLException, IOException {
        if (val != null) {
            Object v = null;
            if (val instanceof List && ((List<?>) val).size() == 1) {
                List<?> values = (List<?>) val;
                v = values.get(0);
            } else if (val instanceof String[] && ((String[]) val).length == 1) {
                String[] values = (String[]) val;
                v = values[0];
            } else {
                v = val;
            }
            PreparedStatementWriter.write(pst, start, type, v);
            return new String[] { argString(getLabel(v, type), type) };
        } else {
            return new String[] {};
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getSetAss(Object val) {
        if (val instanceof List) {
            List<String> values = (List<String>) val;
            val = values.toArray(new String[values.size()]);
        }
        if (val instanceof String[]) {
            String[] values = (String[]) val;
            String quotedColumnName = getQuotedColumnName();
            StringBuffer buf = new StringBuffer((quotedColumnName.length() + 1) * values.length);
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    buf.append(","); //$NON-NLS-1$
                }
                buf.append(quotedColumnName).append("=?"); //$NON-NLS-1$
            }
            return buf.toString();
        } else {
            return super.getSetAss(val);
        }
    }
}
