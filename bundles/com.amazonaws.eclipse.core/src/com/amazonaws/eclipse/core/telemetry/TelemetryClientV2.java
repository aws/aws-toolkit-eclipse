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

import java.net.URI;
import java.util.Collection;

import com.amazonaws.eclipse.core.mobileanalytics.ToolkitEvent;
import com.amazonaws.eclipse.core.mobileanalytics.cognito.AWSCognitoCredentialsProvider;
import com.amazonaws.regions.Regions;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryAsyncClient;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;

public class TelemetryClientV2 {
	private ToolkitTelemetryAsyncClient client;
	public TelemetryClientV2() {
		try {
	     	this.client = getTelemetryClient();
		} catch(Throwable e) {
			this.client = null;
		}
	}
	
    public void publish(Collection<MetricDatum> event) {
    	if(client == null || event == null) {
    		return;
    	}
    	
    	client.postMetrics(PostMetricsRequest
    			.builder()
    			.awsProduct("AWS Toolkit for Eclipse")
    			.metricData(event)
    			.build());
    }

    private ToolkitTelemetryAsyncClient getTelemetryClient() throws Exception {
		final CognitoIdentityClient client = CognitoIdentityClient.builder()
				.credentialsProvider(AnonymousCredentialsProvider.create()).region(Region.US_EAST_1).build();
    	final CognitoProviderV2 cognito = new CognitoProviderV2("us-east-1:820fd6d1-95c0-4ca4-bffb-3f01d32da842", client);
    	return ToolkitTelemetryAsyncClient
    			.builder()
    			.endpointOverride(new URI("https://client-telemetry.us-east-1.amazonaws.com"))
    			.region(software.amazon.awssdk.regions.Region.US_EAST_1)
    			.credentialsProvider(cognito)
    			.build();
    }
}
