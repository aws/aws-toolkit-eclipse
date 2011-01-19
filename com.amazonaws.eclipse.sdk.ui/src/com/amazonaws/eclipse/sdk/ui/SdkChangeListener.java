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
package com.amazonaws.eclipse.sdk.ui;

/**
 * An interface to be implemented by classes which want to be notified when the underlying
 * AWS SDK for Java is changed to a different version.
 */
public interface SdkChangeListener {

    /**
     * This method will be called once for each registered SdkChangeListener whenever
     * the version of the AWS SDK for Java for a particular project is changed.
     *
     * @param newSdk The new version of the AWS SDK for Java being used.
     */
    public void sdkChanged(SdkInstall newSdk);
}
