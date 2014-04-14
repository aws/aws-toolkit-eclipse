/*
 * Copyright 2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.rds.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class CheckIpUtilIntegrationTest {

    public static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$");

    /** Tests that we can find our outgoing IP address */
    @Test
    public void testCheckIp() throws Exception {
        String ip = CheckIpUtil.checkIp();

        assertNotNull(ip);
        assertTrue(IP_ADDRESS_PATTERN.matcher(ip).matches());
    }
}
