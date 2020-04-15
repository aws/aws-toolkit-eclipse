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

import com.amazonaws.eclipse.core.mobileanalytics.cognito.AWSCognitoCredentialsProvider;
import com.amazonaws.regions.Regions;

import software.amazon.awssdk.services.toolkittelemetry.ToolkitTelemetryAsyncClient;

public class TelemetryClientV2 {
	
    private ToolkitTelemetryAsyncClient getTelemetryClient() throws Exception {
    	final AWSCognitoCredentialsProvider cognito = new AWSCognitoCredentialsProvider("us-east-1:820fd6d1-95c0-4ca4-bffb-3f01d32da842", Regions.US_EAST_1)
    	return ToolkitTelemetryAsyncClient
    			.builder()
    			.endpointOverride(new URI("https://client-telemetry.us-east-1.amazonaws.com"))
    			.region(software.amazon.awssdk.regions.Region.US_EAST_1)
    			.credentialsProvider(cognito)
    			.build();
    }
}
