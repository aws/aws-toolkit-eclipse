/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.upload.wizard.model;

public class FunctionConfigPageDataModel {

    public static final String P_DESCRIPTION = "description";
    public static final String P_MEMORY = "memory";
    public static final String P_TIMEOUT = "timeout";
    public static final String P_PUBLISH_NEW_VERSION = "publishNewVersion";
    public static final String P_CREATE_NEW_VERSION_ALIAS = "createNewVersionAlias";
    public static final String P_NONE_ENCRYPTION = "noneEncryption";
    public static final String P_S3_ENCRYPTION = "s3Encryption";
    public static final String P_KMS_ENCRYPTION = "kmsEncryption";

    private String description;
    private boolean publishNewVersion;
    private boolean createNewVersionAlias;
    private Long memory;
    private Long timeout;
    private boolean noneEncryption = true;
    private boolean s3Encryption = false;
    private boolean kmsEncryption = false;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublishNewVersion() {
        return publishNewVersion;
    }

    public void setPublishNewVersion(boolean publishNewVersion) {
        this.publishNewVersion = publishNewVersion;
    }

    public boolean isCreateNewVersionAlias() {
        return createNewVersionAlias;
    }

    public void setCreateNewVersionAlias(boolean createNewVersionAlias) {
        this.createNewVersionAlias = createNewVersionAlias;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public boolean isNoneEncryption() {
        return noneEncryption;
    }

    public void setNoneEncryption(boolean noneEncryption) {
        this.noneEncryption = noneEncryption;
    }

    public boolean isS3Encryption() {
        return s3Encryption;
    }

    public void setS3Encryption(boolean s3Encryption) {
        this.s3Encryption = s3Encryption;
    }

    public boolean isKmsEncryption() {
        return kmsEncryption;
    }

    public void setKmsEncryption(boolean kmsEncryption) {
        this.kmsEncryption = kmsEncryption;
    }
}
