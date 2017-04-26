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
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
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

    public static DefaultHttpClient create(Plugin plugin, String url) {
        HttpParams httpClientParams = new BasicHttpParams();

        IPreferenceStore preferences = AwsToolkitCore.getDefault().getPreferenceStore();

        int connectionTimeout = preferences.getInt(PreferenceConstants.P_CONNECTION_TIMEOUT);
        int socketTimeout = preferences.getInt(PreferenceConstants.P_SOCKET_TIMEOUT);

        HttpConnectionParams.setConnectionTimeout(httpClientParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(httpClientParams, socketTimeout);

        HttpProtocolParams.setUserAgent(httpClientParams,
                AwsClientUtils
                    .formatUserAgentString("AWS-Toolkit-For-Eclipse", plugin));

        DefaultHttpClient httpclient = new DefaultHttpClient(httpClientParams);
        configureProxy(httpclient, url);

        return httpclient;
    }

    private static void configureProxy(DefaultHttpClient client, String url) {
        AwsToolkitCore plugin = AwsToolkitCore.getDefault();
        if (plugin != null) {
            IProxyService proxyService =
                AwsToolkitCore.getDefault().getProxyService();

            if (proxyService.isProxiesEnabled()) {
                try {
                    IProxyData[] proxyData;
                    proxyData = proxyService.select(new URI(url));
                    if (proxyData.length > 0) {

                        IProxyData data = proxyData[0];
                        client.getParams().setParameter(
                            ConnRoutePNames.DEFAULT_PROXY,
                            new HttpHost(data.getHost(), data.getPort()));

                        if (data.isRequiresAuthentication()) {
                            client.getCredentialsProvider().setCredentials(
                                new AuthScope(data.getHost(), data.getPort()),
                                new NTCredentials(data.getUserId(), data.getPassword(), null, null));
                        }
                    }
                } catch (URISyntaxException e) {
                    plugin.logError(e.getMessage(), e);
                }
            }
        }
    }

    private HttpClientFactory() {
    }
}
