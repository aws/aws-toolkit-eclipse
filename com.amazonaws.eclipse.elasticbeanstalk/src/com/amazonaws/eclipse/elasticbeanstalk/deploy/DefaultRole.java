/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.deploy;

import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPublishingUtils;
import com.amazonaws.services.identitymanagement.model.Role;

public class DefaultRole extends Role {

    private static final long serialVersionUID = 1L;

    private final boolean isToBeCreated;

    public DefaultRole(boolean isToBeCreated) {
        this.isToBeCreated = isToBeCreated;
        setRoleName(ElasticBeanstalkPublishingUtils.DEFAULT_ROLE_NAME);
    }

    /**
     * True if this default role doesn't actually exist and must be created
     * before configured as the instance role
     */
    public boolean isToBeCreated() {
        return isToBeCreated;
    }
}