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

import java.util.Arrays;

import junit.framework.TestCase;

public class SDBDataAccessorTest extends TestCase {

    private SimpleDBDataAccessor sdb;

    @Override
    protected void setUp() throws Exception {
        this.sdb = new SimpleDBDataAccessor();
    }

    public void testIsSnippet() {
        assertNotNull(this.sdb);

        assertFalse("Null is not a snippet", this.sdb.isSnippet(null, -1));
        assertFalse("String is not a snippet", this.sdb.isSnippet("String value", -1));
        assertTrue("Empty String array is snippet", this.sdb.isSnippet(new String[] {}, -1));
        assertTrue("String array with empty string is snippet", this.sdb.isSnippet(new String[] { "" }, -1));
        assertTrue("String array is snippet", this.sdb.isSnippet(new String[] { "String value", "String value" }, -1));

    }

    public void testGetLabel() {
        assertNotNull(this.sdb);

        assertEquals("NULL", this.sdb.getLabel(null, -1));
        assertEquals("", this.sdb.getLabel("", -1));
        assertEquals("String value", this.sdb.getLabel("String value", -1));
        assertEquals(Arrays.toString(new String[] { "1", "2" }), this.sdb.getLabel(new String[] { "1", "2" }, -1));

    }

}
