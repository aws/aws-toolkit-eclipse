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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Utility to find a client's internet routable outgoing IP address using
 * checkip.amazonaws.com
 */
public final class CheckIpUtil {
    private static final int CONNECTION_TIMEOUT_MILLIS = 4000;
    private static final String CHECKIP_URL = "http://checkip.amazonaws.com/";

    public static final String checkIp() throws IOException {
        URLConnection connection = null;

        try {
            connection = openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return reader.readLine();
        } finally {
            try {
                if (connection != null) connection.getInputStream().close();
            } catch (IOException e) {}
        }
    }

    private static URLConnection openConnection() throws IOException {
        URL url = new URL(CHECKIP_URL);
        URLConnection connection = url.openConnection(createProxy());

        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);

        return connection;
    }

    /**
     * Creates a Proxy to use when opening a URLConnection, otherwise, it
     * returns <code>Proxy.NO_PROXY</code>.
     *
     * @return A proxy configured with the settings the user has entered in
     *         Eclipse; otherwise, returns <code>Proxy.NO_PROXY</code>.
     */
    private static Proxy createProxy() {
        IProxyService proxyService = AwsToolkitCore.getDefault().getProxyService();
        if ( proxyService.isProxiesEnabled() ) {
            IProxyData[] proxyData = proxyService.select(URI.create(CHECKIP_URL));
            if ( proxyData.length > 0 ) {
                // NOTE: For proxy authentication support in this class, we should switch
                //       to HttpClient since java.net.Proxy doesn't allow you to configure
                //       per-instance auth settings, and instead, we'd have to use
                //       java.net.Authenticator#setDefault to set JVM-wide auth settings.
                InetSocketAddress proxyAddress = new InetSocketAddress(proxyData[0].getHost(),
                                                                       proxyData[0].getPort());
                return new Proxy(Proxy.Type.HTTP, proxyAddress);
            }
        }

        return Proxy.NO_PROXY;
    }
}