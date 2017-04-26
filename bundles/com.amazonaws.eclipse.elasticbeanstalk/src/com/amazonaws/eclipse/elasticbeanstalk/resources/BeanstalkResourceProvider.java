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
package com.amazonaws.eclipse.elasticbeanstalk.resources;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.util.IOUtils;

/**
 * Provides access to resources on the class path. Only handles text files with absolute resource
 * paths currently.
 */
public class BeanstalkResourceProvider {

    private static final String RESOURCES_PATH = "/com/amazonaws/eclipse/elasticbeanstalk/resources";

    private static BeanstalkResource SERVICE_ROLE_PERMISSIONS_POLICY = new BeanstalkResource(RESOURCES_PATH
            + "/service-role-permissions-policy.json");

    private static BeanstalkResource SERVICE_ROLE_TRUST_POLICY = new BeanstalkResource(RESOURCES_PATH
            + "/service-role-trust-policy.json");

    private static BeanstalkResource INSTANCE_PROFILE_PERMISSIONS_POLICY = new BeanstalkResource(RESOURCES_PATH
            + "/instance-profile-permissions-policy.json");

    private static BeanstalkResource INSTANCE_PROFILE_TRUST_POLICY = new BeanstalkResource(RESOURCES_PATH
            + "/instance-profile-trust-policy.json");

    private static BeanstalkResource MINIMUM_IAM_PERMISSIONS_POLICY = new BeanstalkResource(RESOURCES_PATH
            + "/minimum-iam-permissions-policy.json");

    public BeanstalkResource getServiceRolePermissionsPolicy() {
        return SERVICE_ROLE_PERMISSIONS_POLICY;
    }

    public BeanstalkResource getServiceRoleTrustPolicy() {
        return SERVICE_ROLE_TRUST_POLICY;
    }

    public BeanstalkResource getInstanceProfilePermissionsPolicy() {
        return INSTANCE_PROFILE_PERMISSIONS_POLICY;
    }

    public BeanstalkResource getInstanceProfileTrustPolicy() {
        return INSTANCE_PROFILE_TRUST_POLICY;
    }

    public BeanstalkResource getMinimumIamPermissionsPolicy() {
        return MINIMUM_IAM_PERMISSIONS_POLICY;
    }

    /**
     * Represents a resource on the classpath and provides utility methods for accessing it's data
     */
    public static class BeanstalkResource {

        private final String resourcePath;

        public BeanstalkResource(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        public String asString() {
            return getResourceAsString(resourcePath);
        }

        public InputStream asStream() {
            return BeanstalkResourceProvider.class.getResourceAsStream(resourcePath);
        }

        private String getResourceAsString(String resourcePath) {
            return BeanstalkResourceProvider.toString(asStream());
        }
    }

    private static String toString(InputStream is) {
        if (is != null) {
            try {
                String content = IOUtils.toString(is);
                IOUtils.closeQuietly(is, null);
                return content;
            } catch (IOException e) {
                IOUtils.closeQuietly(is, null);
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
