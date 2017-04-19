/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codestar.arn;

import com.amazonaws.util.StringUtils;

public class ARN {

    // An ARN's utf8 representation takes at most 1600 bytes.
    public static final int MAX_BYTES = 1600;

    private final String vendor;
    private final String region;
    private final String namespace;
    private final String relativeId;
    private final String partition;
    /**
     * Constructs an ARN with vendor, region, namespace, and relativeId. All except region and namespace must be non-null. If
     * region is null it will be converted to the empty string. May throw an ARNSyntaxException if the resulting
     * stringified ARN will have a UTF8 representation longer than MAX_BYTES.
     */
    private ARN(String partition, String vendor, String region, String namespace, String relativeId) throws ARNSyntaxException {
        if (vendor == null || relativeId == null || partition == null) {
            throw new NullPointerException();
        }

        this.vendor = vendor;
        this.relativeId = relativeId;
        this.partition = partition;

        if (region == null) {
            this.region = "";
        } else {
            this.region = region;
        }

        if (namespace == null) {
            this.namespace = "";
        } else {
            this.namespace = namespace;
        }
    }

    /**
     * Returns an ARN parsed from arnString. Throws ARNSyntaxException if things look bad. A valid ARN could look like this:
     * <b>arn:aws:codecommit:us-west-2:012345678901:resource-id</b>, and it could be parsed as:
     * <p><ul>
     * <li>partition: aws
     * <li>vendor: codecommit
     * <li>region: us-west-2
     * <li>namespace: 012345678901
     * <li>relativeId: resource-id
     * </ul></p>
     *
     * @param arnString
     * @throws ARNSyntaxException
     */
    public static ARN fromString(String arnString) throws ARNSyntaxException {
        if (StringUtils.isNullOrEmpty(arnString)) {
            throw new IllegalArgumentException("The provided ARN is null or empty!");
        }

        // If the ultimate ARN is going to be too long, fail early.
        validateARNLength(arnString);

        String prefix = "arn:";
        if (!arnString.startsWith(prefix)) {
            throw new ARNSyntaxException(arnString, "ARNs must start with '" + prefix + "'");
        }

        //Partition
        int oldIndex = prefix.length();
        int newIndex = arnString.indexOf(':', oldIndex);
        if (newIndex < 0) {
            throw new ARNSyntaxException(arnString, "Second colon parition not found");
        }
        if (newIndex == oldIndex) {
            throw new ARNSyntaxException(arnString, "Partition must be non-empty");
        }
        String partition = arnString.substring(oldIndex, newIndex);

        //Vendor
        oldIndex = newIndex + 1;
        newIndex = arnString.indexOf(':', oldIndex);
        if (newIndex < 0) {
            throw new ARNSyntaxException(arnString, "Third colon vendor not found");
        }
        if(newIndex == oldIndex) {
            throw new ARNSyntaxException(arnString, "Vendor must be non-empty");
        }

        String vendor = arnString.substring(oldIndex, newIndex);

        // Region
        oldIndex = newIndex + 1;
        newIndex = arnString.indexOf(':', oldIndex);
        if (newIndex < 0) {
            throw new ARNSyntaxException(arnString, "Fourth colon (region/namespace delimiter) not found");
        }
        String region = arnString.substring(oldIndex, newIndex);

        // Namespace
        oldIndex = newIndex + 1;
        newIndex = arnString.indexOf(':', oldIndex);
        if (newIndex < 0) {
            throw new ARNSyntaxException(arnString, "Fifth colon (namespace/relative-id delimiter) not found");
        }
        String namespace = arnString.substring(oldIndex, newIndex);

        // Relative Id
        String relativeId = arnString.substring(newIndex + 1);
        if (relativeId.length() == 0) {
            throw new ARNSyntaxException(arnString, "The Relative Id must be non-empty");
        }

        return new ARN(partition, vendor, region, namespace, relativeId);
    }

    /**
     * Returns an ARN parsed from arnString. Assumes that the string is well-formed. If it's
     * not a RuntimeException will be thrown.
     * @param arnString
     */
    public static ARN fromSafeString(String arnString) {
        try {
            return fromString(arnString);
        } catch (ARNSyntaxException e) {
            throw new RuntimeException("Caught unexpected syntax violation. Consider using ARN.fromString().", e);
        }
    }

    /**
     * Returns the ARN's vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Returns the ARN's region. May be the empty string if the ARN is
     * regionless.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the ARN's namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the ARN's relative id
     */
    public String getRelativeId() {
        return relativeId;
    }

    /**
     * Returns the ARN's partition
     */
    public String getPartition() {
        return partition;
    }

    /**
     * Throws ARNSyntaxException if arnString is more than MAX_BYTES in its UTF-8 representation
     * @param arnString
     * @throws ARNSyntaxException
     */
    private static void validateARNLength(String arnString) throws ARNSyntaxException {
        byte[] bytes = arnString.getBytes(StringUtils.UTF8);
        if (bytes.length > MAX_BYTES) {
            throw new ARNSyntaxException(arnString, "ARNs must be at most " + MAX_BYTES);
        }
    }
}