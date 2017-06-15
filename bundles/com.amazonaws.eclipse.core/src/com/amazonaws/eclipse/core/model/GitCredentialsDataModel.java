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
package com.amazonaws.eclipse.core.model;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class GitCredentialsDataModel {

    public static final String P_USERNAME = "username";
    public static final String P_PASSWORD = "password";
    public static final String P_SHOW_PASSWORD = "showPassword";

    private String username;
    private String password;
    private boolean showPassword;

    private String userAccount = AwsToolkitCore.getDefault().getCurrentAccountId();
    private String regionId = RegionUtils.getCurrentRegion().getId();

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isShowPassword() {
        return showPassword;
    }
    public void setShowPassword(boolean showPassword) {
        this.showPassword = showPassword;
    }
    public String getUserAccount() {
        return userAccount;
    }
    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }
    public String getRegionId() {
        return regionId;
    }
    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public AmazonIdentityManagement getIamClient() {
        if (userAccount != null && regionId != null) {
            return AwsToolkitCore.getClientFactory(userAccount)
                    .getIAMClientByRegion(regionId);
        }
        return null;
    }
}
