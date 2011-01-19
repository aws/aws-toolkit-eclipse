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
package com.amazonaws.eclipse.elasticbeanstalk.deploy;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.*;

import com.amazonaws.services.ec2.model.KeyPairInfo;

public class DeployWizardDataModel {

    // Bean property name constants
    public static final String EXISTING_APPLICATION_NAME = "existingApplicationName";
    public static final String CREATING_NEW_APPLICATION = "creatingNewApplication";
    public static final String NEW_APPLICATION_NAME = "newApplicationName";
    public static final String NEW_APPLICATION_DESCRIPTION = "newApplicationDescription";

    public static final String VERSION_RELEASE_LABEL = "versionReleaseLabel";
    public static final String VERSION_DESCRIPTION = "versionDescription";

    public static final String EXISTING_S3_BUCKET_NAME = "existingS3BucketName";
    public static final String CREATING_NEW_S3_BUCKET = "creatingNewS3Bucket";
    public static final String NEW_S3_BUCKET_NAME = "newS3BucketName";

    public static final String NEW_ENVIRONMENT_NAME = "newEnvironmentName";
    public static final String NEW_ENVIRONMENT_DESCRIPTION = "newEnvironmentDescription";

    public static final String USING_KEY_PAIR = "usingKeyPair";
    public static final String KEY_PAIR = "keyPair";

    public static final String USING_CNAME = "usingCname";
    public static final String CNAME = "cname";

    public static final String SNS_ENDPOINT = "snsEndpoint";
    public static final String SSL_CERTIFICATE_ID = "sslCertificateId";
    public static final String HEALTH_CHECK_URL = "healthCheckUrl";

    public static final String REGION_ENDPOINT = "regionEndpoint";

    // Bean properties
    private String regionEndpoint;

    private String existingApplicationName;
    private boolean isCreatingNewApplication;
    private String newApplicationName;
    private String newApplicationDescription;

    private String newEnvironmentName;
    private String newEnvironmentDescription;

    private boolean usingCname = false;
    private String cname;

    private boolean usingKeyPair = false;
    private KeyPairInfo keyPair;

    private String snsEndpoint;
    private String healthCheckUrl;
    private String sslCertificateId;

    public boolean isUsingKeyPair() {
        return usingKeyPair;
    }

    public void setUsingKeyPair(boolean usingKeyPair) {
        trace("Setting using key pair = " + usingKeyPair);
        this.usingKeyPair = usingKeyPair;
    }

    public boolean isUsingCname() {
        return usingCname;
    }

    public void setUsingCname(boolean usingCname) {
        trace("Setting using cname = " + usingCname);
        this.usingCname = usingCname;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        trace("Setting cname = " + cname);
        this.cname = cname;
    }

    public KeyPairInfo getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPairInfo keyPair) {
        trace("Setting key pair = " + keyPair);
        this.keyPair = keyPair;
    }

    public String getApplicationName() {
        if ( isCreatingNewApplication )
            return newApplicationName;
        return existingApplicationName;
    }

    public String getExistingApplicationName() {
        return existingApplicationName;
    }

    public void setExistingApplicationName(String existingApplicationName) {
        trace("Setting existing application name = " + existingApplicationName);
        this.existingApplicationName = existingApplicationName;
    }

    public boolean isCreatingNewApplication() {
        return isCreatingNewApplication;
    }

    public void setCreatingNewApplication(boolean isCreatingNewApplication) {
        trace("Setting creating new application = " + isCreatingNewApplication);
        this.isCreatingNewApplication = isCreatingNewApplication;
    }

    public String getNewApplicationName() {
        return newApplicationName;
    }

    public void setNewApplicationName(String newApplicationName) {
        trace("Setting new application name = " + newApplicationName);
        this.newApplicationName = newApplicationName;
    }

    public String getNewApplicationDescription() {
        return newApplicationDescription;
    }

    public void setNewApplicationDescription(String newApplicationDescription) {
        trace("Setting new application description = " + newApplicationDescription);
        this.newApplicationDescription = newApplicationDescription;
    }

    // Environment Options

    public String getEnvironmentName() {
        return newEnvironmentName;
    }

    public String getNewEnvironmentName() {
        return newEnvironmentName;
    }

    public void setNewEnvironmentName(String newEnvironmentName) {
        trace("Setting new environment name = " + newEnvironmentName);
        this.newEnvironmentName = newEnvironmentName;
    }

    public String getNewEnvironmentDescription() {
        return newEnvironmentDescription;
    }

    public void setNewEnvironmentDescription(String newEnvironmentDescription) {
        trace("Setting new environment description = " + newEnvironmentDescription);
        this.newEnvironmentDescription = newEnvironmentDescription;
    }

    public String getRegionEndpoint() {
        return regionEndpoint;
    }

    public void setRegionEndpoint(String regionEndpoint) {
        trace("Setting region endpoint = " + regionEndpoint);
        this.regionEndpoint = regionEndpoint;
    }

    public String getSnsEndpoint() {
        return snsEndpoint;
    }

    public void setSnsEndpoint(String snsEndpoint) {
        trace("Setting sns endpoint = " + snsEndpoint);
        this.snsEndpoint = snsEndpoint;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        trace("Setting healthcheck url = " + healthCheckUrl);
        this.healthCheckUrl = healthCheckUrl;
    }

    public String getSslCertificateId() {
        return sslCertificateId;
    }

    public void setSslCertificateId(String sslCertificateId) {
        trace("Setting ssl certificate id = " + sslCertificateId);
        this.sslCertificateId = sslCertificateId;
    }
}
