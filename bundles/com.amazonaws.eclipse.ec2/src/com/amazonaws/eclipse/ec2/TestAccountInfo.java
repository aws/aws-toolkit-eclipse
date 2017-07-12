/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import com.amazonaws.eclipse.core.AccountInfo;

/**
 * Implementation of the AccountInfo class that provides account info during
 * unit tests.
 */
public class TestAccountInfo implements AccountInfo {

    /** The account info properties */
    private static final Properties properties = new Properties();

    /** The file containing the account info properties */
    private static final File testAccountInfoFile;

    static {
        File userHome = new File(System.getProperty("user.home"));
        File ec2Directory = new File(userHome, ".ec2");
        testAccountInfoFile = new File(ec2Directory, "awsToolkitTestAccount.properties");

        try {
            properties.load(new FileInputStream(testAccountInfoFile));
        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException("Unable to load test account info from "
                    + testAccountInfoFile.getAbsolutePath());
        }
    }

    @Override
    public String getAccessKey() {
        return getProperty("accessKey");
    }

    @Override
    public String getSecretKey() {
        return getProperty("secretKey");
    }

    @Override
    public String getEc2CertificateFile() {
        return getProperty("ec2CertificateFile");
    }

    @Override
    public String getEc2PrivateKeyFile() {
        return getProperty("ec2PrivateKeyFile");
    }

    @Override
    public String getUserId() {
        return getProperty("userId");
    }

    @Override
    public void setAccessKey(String accessKey) {
        properties.setProperty("accessKey", accessKey);
    }

    @Override
    public void setSecretKey(String secretKey) {
        properties.setProperty("secretKey", secretKey);
    }

    /**
     * Returns the value of the property with the specified name. The value is
     * trimmed to ensure that no leading or trailing white space in the property
     * file will be included. If no propert by the specified name is found, this
     * method will throw a new RuntimeException.
     *
     * @param property
     *            The name of the property to return.
     *
     * @return The value of the property with the specified name.
     */
    private String getProperty(String property) {
        String value = properties.getProperty(property);
        if (value == null || value.trim().equals("")) {
            throw new RuntimeException("Unable to load property '" + property + "' from file: "
                    + testAccountInfoFile.getAbsolutePath());
        }

        return value.trim();
    }

    /* Un-implemented methods */

    @Override
    public String getInternalAccountId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAccountName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAccountName(String accountName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setUserId(String userId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEc2PrivateKeyFile(String ec2PrivateKeyFile) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setEc2CertificateFile(String ec2CertificateFile) {
        // TODO Auto-generated method stub

    }

    @Override
    public void save() {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCertificateValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUseSessionToken() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setUseSessionToken(boolean useSessionToken) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getSessionToken() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSessionToken(String sessionToken) {
        // TODO Auto-generated method stub

    }

}
