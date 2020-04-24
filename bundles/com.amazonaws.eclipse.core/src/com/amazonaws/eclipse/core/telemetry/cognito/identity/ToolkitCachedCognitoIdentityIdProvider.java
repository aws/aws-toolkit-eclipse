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
package com.amazonaws.eclipse.core.telemetry.cognito.identity;

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;

/**
 * Provide Cognito identity id by making unauthenticated API calls to the
 * Cognito Identity Service. The returned identity id will be cached in the
 * toolkit preference store for subsequent {@link #getIdentityId()} calls.
 */
public class ToolkitCachedCognitoIdentityIdProvider implements AWSCognitoIdentityIdProvider {
    public static final ToolkitCachedCognitoIdentityIdProvider TEST_PROVIDER = new ToolkitCachedCognitoIdentityIdProvider(
            Constants.COGNITO_IDENTITY_POOL_ID_TEST, AwsToolkitCore
                    .getDefault().getPreferenceStore());
    
    public static final ToolkitCachedCognitoIdentityIdProvider V2_PROVIDER = new ToolkitCachedCognitoIdentityIdProvider(
            Constants.COGNITO_IDENTITY_POOL_ID_PROD_V2, AwsToolkitCore
                    .getDefault().getPreferenceStore());

    private final String identityPoolId;
    private final IPreferenceStore prefStore;
    private final AmazonCognitoIdentity cognitoIdentityClient;

    /** The current identity id */
    private volatile String identityId;

    /**
     * @param identityPoolId
     *            id of the identity pool where the identity id will be
     *            requested from. The cached identity id will be invalidated if
     *            this pool id does not match the value persisted in the
     *            preference store.
     * @param prefStore
     *            the preference store for caching the requested identity id,
     *            and the pool id where the cached identity was originally
     *            requested from.
     */
    public ToolkitCachedCognitoIdentityIdProvider(String identityPoolId,
            IPreferenceStore prefStore) {
        this.identityPoolId = identityPoolId;
        this.prefStore = prefStore;

        this.cognitoIdentityClient = AmazonCognitoIdentityClient.builder()
                .withRegion(Constants.COGNITO_IDENTITY_SERVICE_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                .build();
    }

    @Override
    public String getIdentityId() {
        if (identityId == null) {
            if (isPersistedIdentityIdValid()) {
                identityId = loadIdentityIdFromPrefStore();
                AwsToolkitCore.getDefault().logInfo(
                        "Found valid identity id cache " + identityId);
            } else {
                identityId = requestAndCacheNewIdentityId();
                AwsToolkitCore.getDefault().logInfo(
                        "Initialized a new Cognito identity " + identityId);
            }
        }
        return identityId;
    }

    private boolean isPersistedIdentityIdValid() {
        String cachedId = prefStore
                .getString(Constants.COGNITO_IDENTITY_ID_PREF_STORE_KEY);
        String cachedPoolId = prefStore
                .getString(Constants.COGNITO_IDENTITY_POOL_ID_PREF_STORE_KEY);

        return !isEmpty(cachedId) && identityPoolId.equals(cachedPoolId);
    }

    private String requestAndCacheNewIdentityId() {
        String newId = requestIdentityId();
        prefStore.setValue(Constants.COGNITO_IDENTITY_ID_PREF_STORE_KEY, newId);
        prefStore.setValue(Constants.COGNITO_IDENTITY_POOL_ID_PREF_STORE_KEY, identityPoolId);
        return newId;
    }

    private String loadIdentityIdFromPrefStore() {
        return prefStore
                .getString(Constants.COGNITO_IDENTITY_ID_PREF_STORE_KEY);
    }

    private String requestIdentityId() {
        String identityId = cognitoIdentityClient.getId(
                new GetIdRequest().withIdentityPoolId(identityPoolId))
                .getIdentityId();
        return identityId;
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
