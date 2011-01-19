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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Utilities for accessing web services from checkip.amazonaws.com.
 */
public class CheckIpUtils {

	/** The domain name of the checkip service */
	private static final String CHECKIP_DOMAIN_NAME = "checkip.amazonaws.com";
	
	/** The URL for checkip block service at amazonaws.com */
	private static final String CHECKIP_BLOCK_URL = "http://checkip.amazonaws.com/block";

	/** Shared HttpClient object */
	private static final HttpClient client = new HttpClient();
	
	/** Default, wide open netmask */
	private static final String DEFAULT_NETMASK = "0.0.0.0/0";

	/**
	 * Returns the most restrictive netmask that will still allow the current
	 * host to connect to EC2 resources.  If this method detects that an HTTP
	 * proxy is required to reach checkip.amazonaws.com then it just returns
	 * a wide open netmask in case the HTTP proxy IP/subnet differs from the
	 * IP/subnet required to reach non-HTTP resources in EC2 (ex: ssh). 
	 * 
	 * @return The most restrictive netmask that will still allow the current
	 *         host to connect to EC2 resources.
	 * 
	 * @throws IOException If any problems are encountered 
	 */
	public String lookupNetmask() throws IOException {
		IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
		if (proxyService.isProxiesEnabled()) {
			IProxyData proxyData = proxyService.getProxyDataForHost(CHECKIP_DOMAIN_NAME, IProxyData.HTTP_PROXY_TYPE);
			if (proxyData != null) {
				return DEFAULT_NETMASK;
			}
		}
		
		GetMethod method = new GetMethod(CHECKIP_BLOCK_URL);
		int httpStatusCode = client.executeMethod(method);

		if (httpStatusCode != HttpStatus.SC_OK) {
			throw new IOException("Unable to query " + CHECKIP_BLOCK_URL 
								+ " (HTTP error: " + httpStatusCode + ")");
		}

		BufferedReader bufferedReader = null;
		try {
			InputStream input = method.getResponseBodyAsStream();
			InputStreamReader reader = new InputStreamReader(input);
			bufferedReader = new BufferedReader(reader);
			
			return bufferedReader.readLine();
		} finally {
			try {bufferedReader.close();} catch (Exception e) {}
		}
	}

}
