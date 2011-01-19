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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsClientUtils;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

/**
 * Factory for creating EC2 clients. Factory objects are aware of a user's
 * account preferences, and will always return a client configured with the
 * correct account info.
 */
public class Ec2ClientFactory {

	/**
	 * This constant is intended only for testing so that unit tests can
	 * override the Eclipse preference store implementation of AccountInfo.
	 */
    public static final String ACCOUNT_INFO_OVERRIDE_PROPERTY =
    	"com.amazonaws.eclipse.test.AccountInfoOverride";

	/**
	 * A shared account info object for accessing the user's account
	 * preferences.
	 */
	private static final AccountInfo accountInfo;

	/** EC2 clients organized by the name of the region they work with. */
	private static Map<String, AmazonEC2> clientsByEndpoint = new HashMap<String, AmazonEC2>();

	/**
	 * Adds a listener for the user's account info so that we can update the
	 * client when the account info changes.
	 */
	static {
		// Allow account info to be easily overridden for tests
		String accountInfoOverride = System.getProperty(ACCOUNT_INFO_OVERRIDE_PROPERTY);
		if (accountInfoOverride == null) {
			accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
		} else {
			try {
				Class<?> accountInfoOverrideClass = Class.forName(accountInfoOverride);
				accountInfo = (AccountInfo)accountInfoOverrideClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Unable to load and instantiate account info override class "
						+ accountInfoOverride);
			}
		}

		IPropertyChangeListener clientInvalidatingListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				invalidateClients();
			}};

		AwsToolkitCore.getDefault().getPreferenceStore().addPropertyChangeListener(clientInvalidatingListener);


		AwsToolkitCore plugin = AwsToolkitCore.getDefault();
		if (plugin != null) {
			plugin.getProxyService().addProxyChangeListener(new IProxyChangeListener() {
				public void proxyInfoChanged(IProxyChangeEvent event) {
					invalidateClients();
				}
			});
		}
	}

	/**
	 * Invalidates the current set of EC2 clients. This method is intended to be
	 * used after configuration changes are made so that the next time an EC2
	 * client is needed, a fresh instance will be created using the most recent
	 * configuration.
	 */
	private static void invalidateClients() {
		// Invalidate any region specific clients
		synchronized (clientsByEndpoint) {
			clientsByEndpoint.clear();
		}
	}

	/**
	 * Returns an AWS EC2 client, ready to access EC2 with the user's current
	 * account preferences, including the currently selected EC2 region.
	 *
	 * @return An AWS EC2 client ready to access EC2 with the user's current
	 *         account preferences.
	 */
	public AmazonEC2 getAwsClient() {
		/*
		 * If we're running in an OSGi container, and we have access to the user
		 * preferences on which region to use, then use that, otherwise use a
		 * sane default for things like tests that run outside of OSGi.
		 */
		String ec2Endpoint = "ec2.amazonaws.com";
		Ec2Plugin plugin = Ec2Plugin.getDefault();
		if (plugin != null) {
			ec2Endpoint = plugin.getPreferenceStore().getString(PreferenceConstants.P_EC2_REGION_ENDPOINT);
		}

		return getAwsClientByEndpoint(ec2Endpoint);
	}

	/**
	 * Returns an AWS EC2 client for the specified region endpoint
	 *
	 * @param endpoint
	 *            The endpoint that the returned client should work with.
	 *
	 * @return An AWS EC2 client configured to work with the specified region
	 *         endpoint.
	 */
	public AmazonEC2 getAwsClientByEndpoint(String endpoint) {
		synchronized (clientsByEndpoint) {
			if (clientsByEndpoint.get(endpoint) == null) {
				clientsByEndpoint.put(endpoint, createNewAwsClient(endpoint));
			}

			return clientsByEndpoint.get(endpoint);
		}
	}

	/**
	 * Returns a new AWS EC2 client configured to communicate with the specified
	 * EC2 endpoint.
	 *
	 * @param endpoint
	 *            The EC2 endpoint the returned client will be configured to
	 *            use.
	 *
	 * @return A new AWS EC2 client configured to communicate with the specified
	 *         EC2 endpoint.
	 */
	private static AmazonEC2 createNewAwsClient(String endpoint) {
	    AwsClientUtils clientUtils = new AwsClientUtils();

	    ClientConfiguration config = new ClientConfiguration();
		config.setUserAgent(clientUtils.formUserAgentString("EC2EclipsePlugin", Ec2Plugin.getDefault()));

		Ec2Plugin plugin = Ec2Plugin.getDefault();
		if (plugin != null) {
			IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
			if (proxyService.isProxiesEnabled()) {
				IProxyData proxyData = proxyService.getProxyDataForHost(endpoint, IProxyData.HTTPS_PROXY_TYPE);
				if (proxyData != null) {
					config.setProxyHost(proxyData.getHost());
					config.setProxyPort(proxyData.getPort());

					if (proxyData.isRequiresAuthentication()) {
						config.setProxyUsername(proxyData.getUserId());
						config.setProxyPassword(proxyData.getPassword());
					}
				}
			}
		}

		AmazonEC2Client client = new AmazonEC2Client(
		        new BasicAWSCredentials(accountInfo.getAccessKey(), accountInfo.getSecretKey()), config);
		client.setEndpoint("https://" + endpoint);

		return client;
	}

}
