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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import java.util.List;

/**
 * This interface is used as a callback when loading a list of
 * resources such as IAM Roles, or VPCs (Virtual Private Cloud).
 */
public interface LoadResourcesCallback<Resource> {

    /**
     * Called when resources are successfully loaded from service
     *
     * @param resources
     *            List of resources in user's account
     */
    void onSuccess(List<Resource> resources);

    /**
     * Called when we are unable to get the list of resources in the user's account for reasons
     * other then a permissions issue.
     */
    void onFailure();

    /**
     * Called when the currently configured user does not have permissions to list IAM roles.
     */
    void onInsufficientPermissions();
}
