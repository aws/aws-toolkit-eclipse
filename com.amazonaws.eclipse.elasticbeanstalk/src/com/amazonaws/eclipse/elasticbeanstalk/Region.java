/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk;

/**
 * Enumeration of the available AWS Elastic Beanstalk regions and associated endpoints.
 */
public enum Region {
    US_EAST("US-East (Northern Virginia)", "https://elasticbeanstalk.us-east-1.amazonaws.com"),
    ;

    public static final Region DEFAULT = US_EAST;

    private final String name;
    private final String endpoint;

    private Region(String name, String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public static Region findByEndpoint(String lastUsedRegionEndpoint) {
        if (lastUsedRegionEndpoint == null) return null;

        for (Region region : values()) {
            if (region.getEndpoint().equals(lastUsedRegionEndpoint)) return region;
        }

        return null;
    }
}
