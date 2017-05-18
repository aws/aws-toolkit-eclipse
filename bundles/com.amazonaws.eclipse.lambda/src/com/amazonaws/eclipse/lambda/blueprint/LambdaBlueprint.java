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
package com.amazonaws.eclipse.lambda.blueprint;

import com.amazonaws.eclipse.core.model.ComboBoxItemData;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class LambdaBlueprint implements ComboBoxItemData {
    private String baseDir;
    private String displayName;
    private String description;
    private String testJsonFile;
    private String testReturnedValue;

    public String getBaseDir() {
        return baseDir;
    }
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getTestJsonFile() {
        return testJsonFile;
    }
    public void setTestJsonFile(String testJsonFile) {
        this.testJsonFile = testJsonFile;
    }
    public String getTestReturnedValue() {
        return testReturnedValue;
    }
    public void setTestReturnedValue(String testReturnedValue) {
        this.testReturnedValue = testReturnedValue;
    }

    @JsonIgnore
    @Override
    public String getComboBoxItemLabel() {
        return getDisplayName();
    }
}
