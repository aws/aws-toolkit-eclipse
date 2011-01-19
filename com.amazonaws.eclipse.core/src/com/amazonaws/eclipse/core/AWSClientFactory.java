/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Status;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

/**
 * Factory for creating AWS clients.
 */
public class AWSClientFactory {

    /**
     * This constant is intended only for testing so that unit tests can
     * override the Eclipse preference store implementation of AccountInfo.
     */
    public static final String ACCOUNT_INFO_OVERRIDE_PROPERTY = "com.amazonaws.eclipse.test.AccountInfoOverride";

    /**
     * A shared account info object for accessing the user's account
     * preferences.
     */
    private static final AccountInfo accountInfo;

    /** clients organized by the name of the region they work with. */
    private static final HashMap<String, AmazonS3> s3ClientsByEndpoint = new HashMap<String, AmazonS3>();
    private static final HashMap<String, AmazonRDS> rdsClientsByEndpoint = new HashMap<String, AmazonRDS>();
    private static final HashMap<String, AmazonSimpleDB> sdbClientsByEndpoint = new HashMap<String, AmazonSimpleDB>();
    private static HashMap<String, AWSElasticBeanstalk> elasticBeanstalkClientsByEndpoint = new HashMap<String, AWSElasticBeanstalk>();

    /**
     * Adds a listener for the user's account info so that we can update the
     * client when the account info changes.
     */
    static {
        // Allow account info to be easily overridden for tests
        String accountInfoOverride = System.getProperty(ACCOUNT_INFO_OVERRIDE_PROPERTY);
        if ( accountInfoOverride == null ) {
            accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
        } else {
            try {
                Class<?> accountInfoOverrideClass = Class.forName(accountInfoOverride);
                accountInfo = (AccountInfo) accountInfoOverrideClass.newInstance();
            } catch ( Exception e ) {
                throw new RuntimeException("Unable to load and instantiate account info override class "
                        + accountInfoOverride);
            }
        }

        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if ( plugin != null ) {
            plugin.getProxyService().addProxyChangeListener(new IProxyChangeListener() {
                public void proxyInfoChanged(IProxyChangeEvent event) {
                    invalidateClients();
                }
            });

            plugin.addAccountInfoChangeListener(new AccountInfoChangeListener() {
                public void currentAccountChanged() {
                    invalidateClients();
                }
            });
        }
    }

    /**
     * Invalidates the current set of clients. This method is intended to be
     * used after configuration changes are made so that the next time a
     * client is needed, a fresh instance will be created using the most recent
     * configuration.
     */
    private static void invalidateClients() {
        // Invalidate any region specific clients
        synchronized (s3ClientsByEndpoint) {s3ClientsByEndpoint.clear();}
        synchronized (rdsClientsByEndpoint) {rdsClientsByEndpoint.clear();}
        synchronized (sdbClientsByEndpoint) {sdbClientsByEndpoint.clear();}
        synchronized (elasticBeanstalkClientsByEndpoint) {elasticBeanstalkClientsByEndpoint.clear();}
    }

    /**
     * Returns an S3 client configured with the user's preferences, including
     * the currently selected endpoint.
     */
    public AmazonS3 getS3Client() {
        return getS3ClientByEndpoint("s3.amazonaws.com");
    }

    /**
     * Returns an S3 client configured with the user's preferences, including
     * the currently selected endpoint.
     */
    public AmazonSimpleDB getSimpleDBClient() {
        return getSimpleDBClientByEndpoints("sdb.amazonaws.com");
    }

    /**
     * Returns an S3 client for the specified region endpoint
     *
     * @param endpoint
     *            The endpoint that the returned client should work with.
     */
    public AmazonS3 getS3ClientByEndpoint(String endpoint) {
        synchronized (s3ClientsByEndpoint) {
            if ( s3ClientsByEndpoint.get(endpoint) == null ) {
                s3ClientsByEndpoint.put(endpoint, createNewS3Client(endpoint));
            }

            return s3ClientsByEndpoint.get(endpoint);
        }
    }

    public AmazonSimpleDB getSimpleDBClientByEndpoints(final String endpoint) {
        synchronized (sdbClientsByEndpoint) {
            if ( sdbClientsByEndpoint.get(endpoint) == null ) {
                sdbClientsByEndpoint.put(endpoint, createNewSDBClient(endpoint));
            }

            return sdbClientsByEndpoint.get(endpoint);
        }
    }

    public AmazonRDS getRDSClientByEndpoint(final String endpoint) {
        synchronized (rdsClientsByEndpoint) {
            if ( rdsClientsByEndpoint.get(endpoint) == null) {
                rdsClientsByEndpoint.put(endpoint, createNewRDSClient(endpoint));
            }

            return rdsClientsByEndpoint.get(endpoint);
        }
    }

    /**
     * Returns a new SimpleDB client configured to communicate with the specified endpoint.
     */
    private AmazonSimpleDB createNewSDBClient(String endpoint) {
        ClientConfiguration config = createClientConfiguration(endpoint);

        AmazonSimpleDB client = new AmazonSimpleDBClient(new BasicAWSCredentials(accountInfo.getAccessKey(),
                accountInfo.getSecretKey()), config);
        client.setEndpoint(endpoint);

        return client;
    }

    /**
     * Returns an AWS Elastic Beanstalk client for the specified region endpoint
     *
     * @param endpoint
     *            The endpoint that the returned client should work with.
     */
    public AWSElasticBeanstalk getElasticBeanstalkClientByEndpoint(String endpoint) {
        synchronized (elasticBeanstalkClientsByEndpoint) {
            if ( elasticBeanstalkClientsByEndpoint.get(endpoint) == null ) {
                elasticBeanstalkClientsByEndpoint.put(endpoint, createNewElasticBeanstalkClient(endpoint));
            }

            return elasticBeanstalkClientsByEndpoint.get(endpoint);
        }
    }

    /**
     * Returns a new S3 client configured to communicate with the specified
     * endpoint.
     */
    private AmazonS3 createNewS3Client(String endpoint) {
        ClientConfiguration config = createClientConfiguration(endpoint);

        AmazonS3 client = new AmazonS3Client(new BasicAWSCredentials(accountInfo.getAccessKey(),
                accountInfo.getSecretKey()), config);
        client.setEndpoint(endpoint);

        return client;
    }

    private AmazonRDS createNewRDSClient(String endpoint) {
        ClientConfiguration config = createClientConfiguration(endpoint);

        AmazonRDS client = new AmazonRDSClient(new BasicAWSCredentials(
            accountInfo.getAccessKey(), accountInfo.getSecretKey()), config);
        client.setEndpoint(endpoint);

        return client;
    }

    /**
     * Returns a new AWS Elastic Beanstalk client configured to communicate with the
     * specified endpoint.
     */
    private static AWSElasticBeanstalk createNewElasticBeanstalkClient(String endpoint) {
        ClientConfiguration config = createClientConfiguration(endpoint);

        AWSElasticBeanstalk client = new AWSElasticBeanstalkClient(new BasicAWSCredentials(
                accountInfo.getAccessKey(),
                accountInfo.getSecretKey()), config);
        client.setEndpoint(endpoint);

        return client;
    }

    private static ClientConfiguration createClientConfiguration(String secureEndpoint) {
        ClientConfiguration config = new ClientConfiguration();
        AwsClientUtils clientUtils = new AwsClientUtils();

        config.setUserAgent(clientUtils.formUserAgentString("AWS-Toolkit-For-Eclipse", AwsToolkitCore.getDefault()));

        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if ( plugin != null ) {
            IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
            if ( proxyService.isProxiesEnabled() ) {
                try {
                    IProxyData[] proxyData;
                    proxyData = proxyService.select(new URI(secureEndpoint));
                    if ( proxyData.length > 0 ) {
                        config.setProxyHost(proxyData[0].getHost());
                        config.setProxyPort(proxyData[0].getPort());

                        if ( proxyData[0].isRequiresAuthentication() ) {
                            config.setProxyUsername(proxyData[0].getUserId());
                            config.setProxyPassword(proxyData[0].getPassword());
                        }
                    }
                } catch ( URISyntaxException e ) {
                    plugin.getLog().log(new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, e.getMessage(), e));
                }
            }
        }
        return config;
    }

}
