/*
 * Copyright 2008-2011 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.core;

import java.io.File;

/**
 * Abstract class describing the interface for accessing configured AWS account
 * preferences and provides hooks for notification of change events.
 */
public abstract class AccountInfo {

	/**
	 * Returns the currently configured AWS user account ID.
	 *
	 * @return The currently configured AWS user account ID.
	 */
	public abstract String getUserId();

	/**
	 * Returns the currently configured AWS user access key.
	 *
	 * @return The currently configured AWS user access key.
	 */
	public abstract String getAccessKey();

	/**
	 * Sets the AWS Access Key ID for this account info object.
	 *
	 * @param accessKey The AWS Access Key ID.
	 */
	public abstract void setAccessKey(String accessKey);

	/**
	 * Returns the currently configured AWS secret key.
	 *
	 * @return The currently configured AWS secret key.
	 */
	public abstract String getSecretKey();

    /**
     * Sets the AWS Secret Access Key for this account info object.
     *
     * @param secretKey The AWS Secret Access Key.
     */
    public abstract void setSecretKey(String secretKey);

    /**
	 * Returns the currently configured EC2 private key file.
	 *
	 * @return The currently configured EC2 private key file.
	 */
	public abstract String getEc2PrivateKeyFile();

	/**
	 * Returns the currently configured EC2 certificate file.
	 *
	 * @return The currently configured EC2 certificate file.
	 */
	public abstract String getEc2CertificateFile();

	/**
	 * Returns true if the configured account information appears to be valid by
	 * verifying that none of the values are missing.
	 *
	 * @return True if the configured account information appears to be valid,
	 *         otherwise false.
	 */
	public boolean isValid() {
		if (getAccessKey() == null || getAccessKey().length() == 0) return false;
		if (getSecretKey() == null || getSecretKey().length() == 0) return false;
		return true;
	}

	/**
	 * Returns true if and only if the configured certificate and corresponding
	 * private key are valid. Currently that distinction is made by verifying
	 * that the referenced files exist.
	 *
	 * @return True if and only if the configured certificate and corresponding
	 *         private key are valid.
	 */
	public boolean isCertificateValid() {
		String certificateFile = getEc2CertificateFile();
		String privateKeyFile = getEc2PrivateKeyFile();

		if (certificateFile == null || certificateFile.length() == 0) return false;
		if (privateKeyFile == null || privateKeyFile.length() == 0) return false;

		return (new File(certificateFile).isFile() &&
				new File(privateKeyFile).isFile());
	}

}
