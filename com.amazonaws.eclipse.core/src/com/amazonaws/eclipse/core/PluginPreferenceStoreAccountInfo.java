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

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Implementation of the AccountInfo abstract class that provides AWS account information through
 * Eclipse's preference system.
 */
public class PluginPreferenceStoreAccountInfo extends AccountInfo {

	/** The preference store containing the account information */
	private final IPreferenceStore preferenceStore;

	/**
	 * Constructs a new object configured to pull account info from the
	 * specified preference store.
	 *
	 * @param preferenceStore
	 *            The preference store to pull AWS account information.
	 */
	public PluginPreferenceStoreAccountInfo(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
	}


	/*
	 * AccountInfo Interface
	 */

	@Override
	public String getUserId() {
		return decodeString(preferenceStore.getString(PreferenceConstants.P_USER_ID));
	}

	@Override
	public String getAccessKey() {
		return decodeString(preferenceStore.getString(PreferenceConstants.P_ACCESS_KEY));
	}

	@Override
	public String getSecretKey() {
		return decodeString(preferenceStore.getString(PreferenceConstants.P_SECRET_KEY));
	}

	@Override
	public String getEc2PrivateKeyFile() {
		return preferenceStore.getString(PreferenceConstants.P_PRIVATE_KEY_FILE);
	}

	@Override
	public String getEc2CertificateFile() {
		return preferenceStore.getString(PreferenceConstants.P_CERTIFICATE_FILE);
	}

    @Override
    public void setAccessKey(String accessKey) {
        preferenceStore.setValue(PreferenceConstants.P_ACCESS_KEY, encodeString(accessKey));
    }

    @Override
    public void setSecretKey(String secretKey) {
        preferenceStore.setValue(PreferenceConstants.P_SECRET_KEY, encodeString(secretKey));
    }


	/*
	 * Private Interface
	 */

	private String decodeString(String s) {
		return new String(Base64.decodeBase64(s.getBytes()));
	}

	private String encodeString(String s) {
	    return new String(Base64.encodeBase64(s.getBytes()));
	}

}
