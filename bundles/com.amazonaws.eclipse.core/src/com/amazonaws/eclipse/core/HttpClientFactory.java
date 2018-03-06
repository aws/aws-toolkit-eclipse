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
package com.amazonaws.eclipse.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * An HttpClient factory that sets up timeouts and proxy settings. You should
 * never create an HttpClient of your own, you should always use this factory.
 */
public final class HttpClientFactory {

    public static AwsToolkitHttpClient create(Plugin plugin, String url) {
        IPreferenceStore preferences = AwsToolkitCore.getDefault().getPreferenceStore();
        int connectionTimeout = preferences.getInt(PreferenceConstants.P_CONNECTION_TIMEOUT);
        int socketTimeout = preferences.getInt(PreferenceConstants.P_SOCKET_TIMEOUT);

        IProxyData data = getEclipseProxyData(url);
        HttpHost httpHost = null;
        BasicCredentialsProvider credentialsProvider = null;
        if (data != null) {
            httpHost = new HttpHost(data.getHost(), data.getPort());
            if (data.isRequiresAuthentication()) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(httpHost),
                        new NTCredentials(data.getUserId(), data.getPassword(), null, null));
            }
        }

        return AwsToolkitHttpClient.builder()
                .setConnectionTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .setUserAgent(AwsClientUtils.formatUserAgentString("AWS-Toolkit-For-Eclipse", plugin))
                .setProxy(httpHost)
                .setCredentialsProvider(credentialsProvider)
                .build();
    }

    private static IProxyData getEclipseProxyData(String url) {
        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if (plugin != null) {
            IProxyService proxyService =
                AwsToolkitCore.getDefault().getProxyService();

            if (proxyService.isProxiesEnabled()) {
                try {
                    IProxyData[] proxyData = proxyService.select(new URI(url));
                    if (proxyData.length > 0) {
                        return proxyData[0];
                    }
                } catch (URISyntaxException e) {
                    plugin.logError(e.getMessage(), e);
                }
            }
        }

        return null;
    }

    private HttpClientFactory() {
    }
}
