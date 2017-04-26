/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.sdk.ui;

import org.eclipse.core.runtime.IPath;

/**
 * Represents a sample included with a version of the AWS SDK for Java.
 */
public class SdkSample {
    private final String name;
    private final String description;
    private final IPath path;

    /**
     * Constructs a new SDK sample object with the given name, description and
     * location.
     *
     * @param sampleName
     *            The name of this sample.
     * @param description
     *            The description of this sample.
     * @param samplePath
     *            The location of this sample.
     */
    public SdkSample(String sampleName, String description, IPath samplePath) {
        this.name = sampleName;
        this.description = description;
        this.path = samplePath;
    }

    /**
     * Returns the location of the files for this sample.
     *
     * @return The location of the files included in this sample.
     */
    public IPath getPath() {
        return path;
    }

    /**
     * Returns the name of this sample.
     *
     * @return The name of this sample.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of this sample.
     *
     * @return The description of this sample.
     */
    public String getDescription() {
        return description;
    }

}
