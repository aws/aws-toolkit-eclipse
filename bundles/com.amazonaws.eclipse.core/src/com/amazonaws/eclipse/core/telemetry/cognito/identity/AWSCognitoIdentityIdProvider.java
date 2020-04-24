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

/**
 * Responsible for establishing a Cognito identity and providing the identity id
 * for the current user.
 *
 * @see http://docs.aws.amazon.com/cognito/devguide/identity/concepts/authentication-flow/
 */
public interface AWSCognitoIdentityIdProvider {

    /**
     * @return the identity id for the current user.
     */
    String getIdentityId();

}
