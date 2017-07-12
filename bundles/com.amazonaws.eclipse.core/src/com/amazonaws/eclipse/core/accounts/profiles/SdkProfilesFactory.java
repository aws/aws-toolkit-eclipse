/*
 * Copyright 2016 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.accounts.profiles;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.auth.profile.internal.Profile;
import com.amazonaws.auth.profile.internal.ProfileKeyConstants;
import com.amazonaws.internal.StaticCredentialsProvider;

/**
 * Profile factory class to dispatch commonly used profiles.
 */
public class SdkProfilesFactory {

    /**
     * Dispatch a BasicProfile instance with the provided profileName, and empty access key and secret key.
     */
    public static BasicProfile newEmptyBasicProfile(String profileName) {
        return newBasicProfile(profileName, "", "", null);
    }

    /**
     * Dispatch a BasicProfile instance with the provided parameters;
     */
    public static BasicProfile newBasicProfile(String profileName, String accessKey, String secretKey, String sessionToken) {
        Map<String, String> properties = new HashMap<>();
        properties.put(ProfileKeyConstants.AWS_ACCESS_KEY_ID, accessKey);
        properties.put(ProfileKeyConstants.AWS_SECRET_ACCESS_KEY, secretKey);
        if (sessionToken != null) {
            properties.put(ProfileKeyConstants.AWS_SESSION_TOKEN, sessionToken);
        }
        return new BasicProfile(profileName, properties);
    }

    /**
     * Convert a BasicProfile instance to the legacy Profile class.
     */
    public static Profile convert(BasicProfile profile) {
        if (profile == null) return null;
        AWSCredentials credentials;
        if (profile.getAwsSessionToken() != null) {
            credentials = new BasicSessionCredentials(
                    profile.getAwsAccessIdKey(), 
                    profile.getAwsSecretAccessKey(),
                    profile.getAwsSessionToken());
        } else {
            credentials = new BasicAWSCredentials(
                    profile.getAwsAccessIdKey(),
                    profile.getAwsSecretAccessKey());
        }
        Profile legacyProfile = new Profile(profile.getProfileName(), profile.getProperties(),
                new StaticCredentialsProvider(credentials));
        
        return legacyProfile;
    }

}
