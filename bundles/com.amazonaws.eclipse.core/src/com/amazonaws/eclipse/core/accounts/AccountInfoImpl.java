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
package com.amazonaws.eclipse.core.accounts;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.eclipse.core.AccountInfo;

/**
 * A Java-bean compliant implementation of the AccountInfo interface. This class
 * consists of two main components - AccountCredentialsConfiguration and
 * AccountOptionalConfiguration. These two components are independent and might
 * use different source to read and persist the configurations.
 */
public class AccountInfoImpl implements AccountInfo {

    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
            this);

    /**
     * The internal account identifier associated with this account.
     */
    private final String accountId;

    /**
     * Config information related to the security credentials for this account.
     */
    private final AccountCredentialsConfiguration credentialsConfig;

    /**
     * All the optional configuration for this account.
     */
    private final AccountOptionalConfiguration optionalConfig;

    public AccountInfoImpl(
            final String accountId,
            final AccountCredentialsConfiguration credentialsConfig,
            final AccountOptionalConfiguration optionalConfig) {
        if (accountId == null)
            throw new IllegalArgumentException("accountId must not be null.");
        if (credentialsConfig == null)
            throw new IllegalArgumentException("credentialsConfig must not be null.");
        if (optionalConfig == null)
            throw new IllegalArgumentException("optionalConfig must not be null.");

        this.accountId         = accountId;
        this.credentialsConfig = credentialsConfig;
        this.optionalConfig    = optionalConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInternalAccountId() {
        return accountId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccountName() {
        return credentialsConfig.getAccountName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAccountName(String accountName) {
        String oldValue = getAccountName();
        if ( !isEqual(oldValue, accountName) ) {
            credentialsConfig.setAccountName(accountName);
            firePropertyChange("accountName", oldValue, accountName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessKey() {
        return credentialsConfig.getAccessKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAccessKey(String accessKey) {
        String oldValue = getAccessKey();
        if ( !isEqual(oldValue, accessKey) ) {
            credentialsConfig.setAccessKey(accessKey);
            firePropertyChange("accessKey", oldValue, accessKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSecretKey() {
        return credentialsConfig.getSecretKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSecretKey(String secretKey) {
        String oldValue = getSecretKey();
        if ( !isEqual(oldValue, secretKey) ) {
            credentialsConfig.setSecretKey(secretKey);
            firePropertyChange("secretKey", oldValue, secretKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUseSessionToken() {
        return credentialsConfig.isUseSessionToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUseSessionToken(boolean useSessionToken) {
        boolean oldValue = isUseSessionToken();
        if ( oldValue != useSessionToken ) {
            credentialsConfig.setUseSessionToken(useSessionToken);
            firePropertyChange("useSessionToken", oldValue, useSessionToken);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSessionToken() {
        return credentialsConfig.getSessionToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSessionToken(String sessionToken) {
        String oldValue = getSessionToken();
        if ( !isEqual(oldValue, sessionToken) ) {
            credentialsConfig.setSessionToken(sessionToken);
            firePropertyChange("sessionToken", oldValue, sessionToken);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId() {
        return optionalConfig.getUserId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserId(String userId) {
        String oldValue = getUserId();
        if ( !isEqual(oldValue, userId) ) {
            optionalConfig.setUserId(userId);
            firePropertyChange("userId", oldValue, userId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEc2PrivateKeyFile() {
        return optionalConfig.getEc2PrivateKeyFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEc2PrivateKeyFile(String ec2PrivateKeyFile) {
        String oldValue = getEc2PrivateKeyFile();
        if ( !isEqual(oldValue, ec2PrivateKeyFile) ) {
            optionalConfig.setEc2PrivateKeyFile(ec2PrivateKeyFile);
            firePropertyChange("ec2PrivateKeyFile", oldValue, ec2PrivateKeyFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEc2CertificateFile() {
        return optionalConfig.getEc2CertificateFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEc2CertificateFile(String ec2CertificateFile) {
        String oldValue = getEc2CertificateFile();
        if ( !isEqual(oldValue, ec2CertificateFile) ) {
            optionalConfig.setEc2CertificateFile(ec2CertificateFile);
            firePropertyChange("ec2CertificateFile", oldValue, ec2CertificateFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        credentialsConfig.save();
        optionalConfig.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        credentialsConfig.delete();
        optionalConfig.delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return credentialsConfig.isDirty()
                || optionalConfig.isDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return credentialsConfig.isCredentialsValid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCertificateValid() {
        return optionalConfig.isCertificateValid();
    }

    /* Java Bean related interfaces */

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName,
                listener);
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName,
                listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue,
            Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue,
                newValue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(getAccountName());
        sb.append("]: ");

        sb.append("accessKey=");
        sb.append(getAccessKey());
        sb.append(", secretKey=");
        sb.append(getSecretKey());
        sb.append(", userId=");
        sb.append(getUserId());
        sb.append(", certFile=");
        sb.append(getEc2CertificateFile());
        sb.append(", privateKey=");
        sb.append(getEc2PrivateKeyFile());

        return sb.toString();
    }

    private static boolean isEqual(Object a, Object b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.equals(b);
    }

}
