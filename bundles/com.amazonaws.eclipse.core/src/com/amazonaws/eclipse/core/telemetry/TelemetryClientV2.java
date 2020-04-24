/*
 * Copyright 2020 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.core.telemetry;

import java.util.Collection;

import com.amazonaws.auth.AWSCredentialsProvider;

import software.amazon.awssdk.services.toolkittelemetry.TelemetryClient;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;

public class TelemetryClientV2 {
    private TelemetryClient client;
    private ClientContextConfig config;

    public TelemetryClientV2(AWSCredentialsProvider credentialsProvider, ClientContextConfig clientContextConfig) {
        try {
            this.client = getTelemetryClient(credentialsProvider);
            this.config = clientContextConfig;
        } catch (Throwable e) {
            this.client = null;
        }
    }

    public void publish(Collection<MetricDatum> event) {
        if (client == null || event == null) {
            return;
        }

        final PostMetricsRequest request = new PostMetricsRequest().aWSProduct("AWS Toolkit For Eclipse").parentProduct("Eclipse")
                .parentProductVersion(config.getEclipseVersion()).clientID(config.getClientId()).metricData(event).oSVersion(config.getEnvPlatformVersion())
                .oS(config.getEnvPlatformName()).aWSProductVersion(config.getVersion());

        client.postMetrics(request);
    }

    private TelemetryClient getTelemetryClient(AWSCredentialsProvider credentialsProvider) throws Exception {
        return TelemetryClient.builder().endpoint("https://client-telemetry.us-east-1.amazonaws.com").iamCredentials(credentialsProvider).build();
    }
}
