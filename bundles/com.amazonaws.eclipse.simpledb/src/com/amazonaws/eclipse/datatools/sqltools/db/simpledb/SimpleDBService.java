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
package com.amazonaws.eclipse.datatools.sqltools.db.simpledb;

import java.util.ArrayList;

import org.eclipse.datatools.sqltools.db.generic.service.GenericSQLService;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class SimpleDBService extends GenericSQLService {

    /**
     * This is a temporary solution until the SimpleDB specific parsers are completed.
     *
     * @see org.eclipse.datatools.sqltools.core.services.SQLService#splitSQLByTerminatorLine(java.lang.String,
     *      java.lang.String[])
     */
    @Override
    public String[] splitSQLByTerminatorLine(final String sql, final String[] terminators) {
        IDocument doc = new Document(sql);
        ArrayList<String> groups = new ArrayList<>();
        //the start position for current group
        int index = 0;
        int numberOfLines = doc.getNumberOfLines();
        try {
            for (int i = 0; i < numberOfLines; i++) {
                IRegion r = doc.getLineInformation(i);
                String line = doc.get(r.getOffset(), r.getLength());
                for (int j = 0; j < terminators.length; j++) {
                    if (line.trim().equalsIgnoreCase(terminators[j])) {
                        String string = doc.get(index, r.getOffset() - index);
                        if (string.trim().length() > 0) {
                            groups.add(string);
                        }
                        index = r.getOffset() + doc.getLineLength(i);
                        break;
                    }
                }
            }
            if (index < doc.getLength() - 1) {
                String string = doc.get(index, doc.getLength() - index);
                if (string.trim().length() > 0) {
                    groups.add(string);
                }
            }
        } catch (Exception e) {
            //parse error, simply return
            return new String[] { sql };
        }
        return groups.toArray(new String[groups.size()]);
    }
}
