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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;

public class CognitoProviderV2 implements AwsCredentialsProvider {
	private CognitoIdentityClient cognitoClient;
	private String identityPool;
	
	public CognitoProviderV2(String identityPool, CognitoIdentityClient cognitoClient) {
		this.identityPool = identityPool;
		this.cognitoClient = cognitoClient;
	}
	
	private final AwsCognitoIdentityProvider identityIdProvider = new AwsCognitoIdentityProvider(cognitoClient, identityPool);

	private CachedSupplier<AwsSessionCredentials> cacheSupplier = CachedSupplier.builder(this::updateCognitoCredentials)
		    .prefetchStrategy(new NonBlocking("Cognito Identity Credential Refresh"))
		    .build();

	private RefreshResult<AwsSessionCredentials> updateCognitoCredentials() {
		        final Credentials credentialsForIdentity = credentialsForIdentity();
		        final AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
		            credentialsForIdentity.accessKeyId(),
		            credentialsForIdentity.secretKey(),
		            credentialsForIdentity.sessionToken()
		        );
		        final Instant actualExpiration = credentialsForIdentity.expiration();

		        return RefreshResult.builder(sessionCredentials)
		            .staleTime(actualExpiration.minus(1, ChronoUnit.MINUTES))
		            .prefetchTime(actualExpiration.minus(5, ChronoUnit.MINUTES))
		            .build();
		    }

	private Credentials credentialsForIdentity() {
        String identityId = identityIdProvider.identityId();
        GetCredentialsForIdentityRequest request = GetCredentialsForIdentityRequest.builder().identityId(identityId).build();

        return cognitoClient.getCredentialsForIdentity(request).credentials();
    }

	@Override
	public AwsCredentials resolveCredentials() {
		return cacheSupplier.get();
	}

	private class AwsCognitoIdentityProvider {
		private CognitoIdentityClient cognitoClient;
		private String identityPoolId;

		public AwsCognitoIdentityProvider(CognitoIdentityClient cognitoClient, String identitypoolId) {
			this.cognitoClient = cognitoClient;
			this.identityPoolId = identitypoolId;
		}

		private String identityId() {
		    GetIdRequest request = GetIdRequest.builder().identityPoolId(identityPoolId).build();
		    String newIdentityId = cognitoClient.getId(request).identityId();

		    return newIdentityId;
		}
	}
}

