/*
 * Copyright 2008-2011 Amazon Technologies, Inc. 
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Integration tests for the CheckIpUtils class.
 */
public class CheckIpUtilsIntegrationTest {

	/** Shared CheckIpUtils object for all tests to use */
	private final CheckIpUtils checkIpUtils = new CheckIpUtils();

	/**
	 * Simple test to exercise the lookupNetmask method and verify that
	 * it's returning reasonable data.
	 */
	@Test
	public void testLookupNetmask() throws Exception {
		String netmask = checkIpUtils.lookupNetmask();
		System.out.println("Identified netmask: " + netmask);
		
		assertNotNull(netmask);
		assertTrue(netmask.contains("/"));
	}
}
