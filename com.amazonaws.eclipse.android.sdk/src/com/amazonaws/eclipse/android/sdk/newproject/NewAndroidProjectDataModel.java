/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.android.sdk.newproject;

import com.android.sdklib.IAndroidTarget;

public class NewAndroidProjectDataModel {
    private String projectName;
    private String packageName;
    private boolean sampleCodeIncluded = true;
    private IAndroidTarget androidTarget;


    public boolean isSampleCodeIncluded() {
        return sampleCodeIncluded;
    }

    public void setSampleCodeIncluded(boolean includeSampleCode) {
        this.sampleCodeIncluded = includeSampleCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public IAndroidTarget getAndroidTarget() {
        return androidTarget;
    }

    public void setAndroidTarget(IAndroidTarget androidTarget) {
        this.androidTarget = androidTarget;
    }

}