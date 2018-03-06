/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;

public class AwsToolkitHttpClient {
    private final HttpClient httpClient;

    private AwsToolkitHttpClient(
            Integer connectionTimeout,
            Integer socketTimeout,
            String userAgent,
            HttpHost proxy,
            CredentialsProvider credentialsProvider) {

        Builder requestConfigBuilder = RequestConfig.custom();
        if (connectionTimeout != null) {
            requestConfigBuilder.setConnectionRequestTimeout(connectionTimeout);
        }
        if (socketTimeout != null) {
            requestConfigBuilder.setSocketTimeout(socketTimeout);
        }

        HttpClientBuilder builder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .setUserAgent(userAgent);
        if (proxy != null) {
            builder.setProxy(proxy);
            if (credentialsProvider != null) {
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }
        httpClient = builder
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }

    /**
     * Head the given URL for the last modified date.
     *
     * @param url The target URL resource.
     * @return The last modified date give the URL, or null if the date cannot be retrieved
     * @throws ClientProtocolException
     * @throws IOException
     */
    public Date getLastModifiedDate(String url) throws ClientProtocolException, IOException {
        HttpHead headMethod = new HttpHead(url);
        HttpResponse response = httpClient.execute(headMethod);
        Header header = response.getFirstHeader("Last-Modified");
        if (header != null) {
            String lastModifiedDateString = header.getValue();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            try {
                return dateFormat.parse(lastModifiedDateString);
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Fetch the content of the target URL and redirect to the output stream.
     * If no content is fetched, do nothing.
     *
     * @param url The target URL
     * @param output The OutputStream
     * @throws ClientProtocolException
     * @throws IOException
     */
    public void outputEntityContent(String url, OutputStream output) throws ClientProtocolException, IOException {
        try (InputStream inputStream = getEntityContent(url)) {
            if (inputStream == null) {
                return;
            }
            int length;
            byte[] buffer = new byte[2048];
            while ((length = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }
    }

    /**
     * Return the input stream of the underlying resource in the target URL.
     * @param url The target URL
     * @return The underlying input stream, or null if it doesn't have entity
     * @throws ClientProtocolException
     * @throws IOException
     */
    public InputStream getEntityContent(String url) throws ClientProtocolException, IOException {
        HttpGet getMethod = new HttpGet(url);
        HttpResponse response = httpClient.execute(getMethod);
        HttpEntity entity = response.getEntity();
        return entity == null ? null : entity.getContent();
    }

    static AwsToolkitHttpClientBuilder builder() {
        return new AwsToolkitHttpClientBuilder();
    }

    static final class AwsToolkitHttpClientBuilder {
        private Integer connectionTimeout;
        private Integer socketTimeout;
        private String userAgent;
        private HttpHost proxy;
        private CredentialsProvider credentialsProvider;

        private AwsToolkitHttpClientBuilder() {}

        public AwsToolkitHttpClient build() {
            return new AwsToolkitHttpClient(connectionTimeout, socketTimeout, userAgent, proxy, credentialsProvider);
        }

        public Integer getConnectionTimeout() {
            return connectionTimeout;
        }

        public AwsToolkitHttpClientBuilder setConnectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Integer getSocketTimeout() {
            return socketTimeout;
        }

        public AwsToolkitHttpClientBuilder setSocketTimeout(Integer socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public AwsToolkitHttpClientBuilder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public HttpHost getProxy() {
            return proxy;
        }

        public AwsToolkitHttpClientBuilder setProxy(HttpHost proxy) {
            this.proxy = proxy;
            return this;
        }

        public CredentialsProvider getCredentialsProvider() {
            return credentialsProvider;
        }

        public AwsToolkitHttpClientBuilder setCredentialsProvider(CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }
    }
}
