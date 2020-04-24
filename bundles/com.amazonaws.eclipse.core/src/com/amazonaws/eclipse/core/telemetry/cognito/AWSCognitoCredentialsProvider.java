/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.telemetry.cognito;

import java.util.Date;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.eclipse.core.telemetry.cognito.identity.AWSCognitoIdentityIdProvider;
import com.amazonaws.eclipse.core.telemetry.cognito.identity.ToolkitCachedCognitoIdentityIdProvider;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityRequest;

/**
 * AWSCredentialsProvider implementation that uses the Amazon Cognito Identity
 * service to create temporary, short-lived sessions to use for authentication
 */
public class AWSCognitoCredentialsProvider implements AWSCredentialsProvider {

    public static final AWSCognitoCredentialsProvider TEST_PROVIDER = new AWSCognitoCredentialsProvider(
            ToolkitCachedCognitoIdentityIdProvider.TEST_PROVIDER);
    
    public static final AWSCognitoCredentialsProvider V2_PROVIDER = new AWSCognitoCredentialsProvider(
            ToolkitCachedCognitoIdentityIdProvider.V2_PROVIDER);

    private final AWSCognitoIdentityIdProvider identityIdProvider;

    /** The Cognito Identity Service client for requesting session credentials */
    private final AmazonCognitoIdentity cognitoIdentityClient;

    /** The current session credentials */
    private volatile AWSSessionCredentials sessionCredentials;

    /** The expiration time for the current session credentials */
    private volatile Date sessionCredentialsExpiration;

    public AWSCognitoCredentialsProvider(
            AWSCognitoIdentityIdProvider identityIdProvider, Regions region) {
        this.identityIdProvider = identityIdProvider;
        this.cognitoIdentityClient = AmazonCognitoIdentityClient.builder()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();
    }

    public AWSCognitoCredentialsProvider(
            AWSCognitoIdentityIdProvider identityIdProvider) {
        this(identityIdProvider, Constants.COGNITO_IDENTITY_SERVICE_REGION);
    }

    /**
     * If the current session has expired/credentials are invalid, a new session
     * is started, establishing the credentials. In either case, those
     * credentials are returned
     */
    @Override
    public AWSSessionCredentials getCredentials() {
        if (needsNewSession()) {
            startSession();
        }
        return sessionCredentials;
    }

    @Override
    public void refresh() {
        startSession();
    }


    /** threshold for refreshing session credentials */
    private static final int CREDS_REFRESH_THRESHOLD_SECONDS = 500;

    private boolean needsNewSession() {
        if (sessionCredentials == null) {
            return true;
        }
        long timeRemaining = sessionCredentialsExpiration.getTime()
                - System.currentTimeMillis();
        return timeRemaining < (CREDS_REFRESH_THRESHOLD_SECONDS * 1000);
    }

    private void startSession() {
        String identityId = identityIdProvider.getIdentityId();

        com.amazonaws.services.cognitoidentity.model.Credentials credentials = cognitoIdentityClient
                .getCredentialsForIdentity(
                        new GetCredentialsForIdentityRequest()
                                .withIdentityId(identityId)).getCredentials();

        sessionCredentials = new BasicSessionCredentials(credentials.getAccessKeyId(),
                credentials.getSecretKey(), credentials.getSessionToken());
        sessionCredentialsExpiration = credentials.getExpiration();
    }

}
