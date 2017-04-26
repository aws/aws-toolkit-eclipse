/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.regions;

/**
 * Details about a service available in a region.
 */
public class Service {
    /** The abbreviated name of this service - see {@link ServiceAbbreviations} */
    private final String serviceName;

    /** The ID of this service, as used in AWS Signature V4 request signing. */
    private final String serviceId;

    /** The URL at which this service can be reached. */
    private final String endpoint;

    private final String signerOverride;

    public Service(String serviceName,
                   String serviceId,
                   String endpoint,
                   String signerOverride) {
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.endpoint = endpoint;
        this.signerOverride = signerOverride;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getSignerOverride() {
        return signerOverride;
    }
}