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
package com.amazonaws.eclipse.identitymanagement.group;

public class CreateGroupWizardDataModel {
    private String groupName;
    // Whether grant permission to the group to create.
    private boolean grantPermission;
    private String policyName;
    private String policyDoc;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean getGrantPermission() {
        return grantPermission;
    }

    public void setGrantPermission(boolean grantPermission) {
        this.grantPermission = grantPermission;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyDoc() {
        return policyDoc;
    }

    public void setPolicyDoc(String policyDoc) {
        this.policyDoc = policyDoc;
    }
}
