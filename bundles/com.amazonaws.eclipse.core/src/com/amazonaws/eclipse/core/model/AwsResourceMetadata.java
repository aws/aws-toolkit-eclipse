/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.model;

import java.util.List;

/**
 * Metadata for a specific AWS resource type.
 */
public interface AwsResourceMetadata<T, P extends AbstractAwsResourceScopeParam<P>> {
    // Fake resource items indicating the current loading status to be used in the Combo box.
    T getLoadingItem();
    T getNotFoundItem();
    T getErrorItem();

    // Resource type e.g Bucket or Stack to be used in the information message.
    String getResourceType();

    // Default resource name to be used when creating a new resource.
    String getDefaultResourceName();

    // Load AWS resources given the provided parameters
    List<T> loadAwsResources(P param);

    // The preferred resource name denoting this resource.
    String getResourceName(T resource);

    default T findResourceByName(List<T> resources, String name) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }

        return resources.stream()
                .filter(resource -> getResourceName(resource).equals(name))
                .findAny().orElse(null);
    }
}