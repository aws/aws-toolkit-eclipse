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
package com.amazonaws.eclipse.identitymanagement.role;


public class CreateRoleWizardDataModel {
    private String roleName;
    // The service name for the service role
    private String service;
    private String accountId;
    private String internalAccountId;
    private String externalAccountId;
    private String webProvider;
    private String applicationId;
    private String policyName;
    private String policyDoc;
    private boolean grantPermission;
    // Whether to create a service role
    private boolean serviceRoles;
    // Whether to create a role using your own AWS account
    private boolean accountRoles;
    // Whether to create a role using third party AWS account
    private boolean thirdPartyRoles;
 // Whether to create a role using web federation
    private boolean webProviderRoles;


    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setInternalAccountId(String internalAccountId) {
        this.internalAccountId = internalAccountId;
    }

    public String getInternalAccountId() {
        return internalAccountId;
    }

    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyDoc(String policyDoc) {
        this.policyDoc = policyDoc;
    }

    public String getPolicyDoc() {
        return policyDoc;
    }

    public boolean getGrantPermission() {
        return grantPermission;
    }

    public void setGrantPermission(boolean grantPermission) {
        this.grantPermission = grantPermission;
    }

    public void setServiceRoles(boolean serviceRoles) {
        this.serviceRoles = serviceRoles;
    }

    public boolean getServiceRoles() {
        return serviceRoles;
    }

    public void setAccountRoles(boolean accountRoles) {
        this.accountRoles = accountRoles;
    }

    public boolean getAccountRoles() {
        return accountRoles;
    }

    public void setThirdPartyRoles(boolean thirdPartyRoles) {
        this.thirdPartyRoles = thirdPartyRoles;
    }

    public boolean getThirdPartyRoles() {
        return thirdPartyRoles;
    }

    public boolean getWebProviderRoles() {
        return webProviderRoles;
    }

    public void setWebProviderRoles(boolean webProviderRoles) {
        this.webProviderRoles = webProviderRoles;
    }

    public void setWebProvider(String webProvider) {
        this.webProvider = webProvider;
    }

    public String getWebProvider() {
        return this.webProvider;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

}
