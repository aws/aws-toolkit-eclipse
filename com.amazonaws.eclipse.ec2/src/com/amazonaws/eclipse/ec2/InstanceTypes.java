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
package com.amazonaws.eclipse.ec2;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class describes the available EC2 instance types and the default
 * instance type to use in UIs.
 *
 * Instance type descriptions are loaded from Amazon S3, or from local
 * descriptions if the S3 metadata isn't available.
 */
public class InstanceTypes {

    /** Location of the instance type description metadata */
    static final String INSTANCE_TYPES_METADATA_URL =
            "https://s3.amazonaws.com/aws-vs-toolkit/ServiceMeta/EC2ServiceMeta.xml";

    private static boolean initialized = false;

    private static List<InstanceType> instanceTypes;

    private static String defaultInstanceTypeId;
    private static InstanceType defaultInstanceType;


    /**
     * Returns the known Amazon EC2 instance types, attempting to load them from
     * Amazon S3 if they haven't been loaded yet.
     */
    public static List<InstanceType> getInstanceTypes() {
        initialize();
        return instanceTypes;
    }

    /**
     * Returns the default Amazon EC2 instance type, attempting to load instance
     * type descriptions from Amazon S3 if they haven't been loaded yet.
     */
    public static InstanceType getDefaultInstanceType() {
        if (defaultInstanceType != null) return defaultInstanceType;

        if (defaultInstanceTypeId == null) {
            Ec2Plugin.log(new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID, "No default instance type available"));
        }

        for (InstanceType type : getInstanceTypes()) {
            if (type.id.equals(defaultInstanceTypeId)) {
                defaultInstanceType = type;
                return defaultInstanceType;
            }
        }

        Ec2Plugin.log(new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID, "No instance type found with default type id: " + defaultInstanceTypeId));
        return new InstanceType("Small", "m1.small", "N/A", "N/A", 1, "32/64", false, false);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        // Attempt to load the latest file from S3
        try {
            loadInstanceTypes(newURL(INSTANCE_TYPES_METADATA_URL));
            return;
        } catch (IOException e) {
            Ec2Plugin.log(new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID, "Unable to load Amazon EC2 instance type descriptions from Amazon S3", e));
        }

        // If it fails, then fallback to the file we ship with
        try {
            loadInstanceTypes(Ec2Plugin.getDefault().getBundle().getEntry("etc/InstanceTypes.xml"));
        } catch (IOException e) {
            Ec2Plugin.log(new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID, "Unable to load Amazon EC2 instance type descriptions from local metadata", e));
        }
    }

    private static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create URL: " + url, e);
        }
    }

    private static void loadInstanceTypes(URL url) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();

            InstanceTypesParser parser = new InstanceTypesParser(inputStream);
            instanceTypes = parser.parseInstanceTypes();
            defaultInstanceTypeId = parser.parseDefaultInstanceTypeId();
        } finally {
            try {inputStream.close();} catch (Exception e) {}
        }
    }
}