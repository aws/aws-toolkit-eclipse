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

package com.amazonaws.eclipse.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Unit tests for the AmiToolsVersion class.
 */
public class AmiToolsVersionTest {

    /**
     * Tests that known version numbers are parsed correctly.
     */
    @Test
    public void testParsing() throws Exception {
        AmiToolsVersion version = new AmiToolsVersion("1.3-123456");
        assertEquals(1, version.getMajorVersion());
        assertEquals(3, version.getMinorVersion());
        assertEquals(123456, version.getPatch());

        version = new AmiToolsVersion("1.3-20041 20071010\n");
        assertEquals(1, version.getMajorVersion());
        assertEquals(3, version.getMinorVersion());
        assertEquals(20041, version.getPatch());
    }

    /**
     * Tests that a version number with non-numeric components will correctly
     * cause a ParseException to be thrown.
     */
    @Test(expected = java.text.ParseException.class)
    public void testParseExceptionNonNumeric() throws Exception {
        new AmiToolsVersion("1.X-12345");
    }

    /**
     * Tests that a version number in the wrong format will correctly cause a
     * ParseException to be thrown.
     */
    @Test(expected = java.text.ParseException.class)
    public void testParseExceptionWrongFormat() throws Exception {
        new AmiToolsVersion("1.2.3.45");
    }

    /**
     * Tests that the isGreaterThan method correctly compares versions with each
     * other.
     */
    @Test
    public void testIsGreaterThan() throws Exception {
        AmiToolsVersion a = new AmiToolsVersion("1.0-99999999");
        AmiToolsVersion b = new AmiToolsVersion("1.3-99999999");
        AmiToolsVersion c = new AmiToolsVersion("2.0-1");

        assertTrue(c.isGreaterThan(b));
        assertTrue(b.isGreaterThan(a));

        assertFalse(a.isGreaterThan(b));
        assertFalse(b.isGreaterThan(c));
    }

}
