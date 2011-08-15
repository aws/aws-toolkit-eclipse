/*
 * Copyright 2009-2011 Amazon Technologies, Inc. 
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
package com.amazonaws.eclipse.core.preferences;

import com.amazonaws.eclipse.core.ui.preferences.ObfuscatingStringFieldEditor;

/**
 * Preference constants for the AWS Toolkit Core plugin.
 */
public class PreferenceConstants {

	/** Preference key for a user's AWS user ID. */
	public static final String P_USER_ID = "userId";
	
	/** Preference key for a user's AWS secret key. */
	public static final String P_SECRET_KEY = "secretKey";
	
	/** Preference key for a user's AWS access key. */
	public static final String P_ACCESS_KEY = "accessKey";

	/** Preference key for a user's EC2 private key file. */
	public static final String P_PRIVATE_KEY_FILE = "privateKeyFile";
	
	/** Preference key for a user's EC2 certificate file. */
	public static final String P_CERTIFICATE_FILE = "certificateFile";
	
    /**
     * Preference key for the ID of the default region.
     */
    public static final String P_DEFAULT_REGION = "defaultRegion";
    
	/**
	 * Preference key indicating whether the preferences from the EC2 plugin
	 * preference store have been imported yet.
	 */
	public static final String P_EC2_PREFERENCES_IMPORTED = "ec2PreferencesImported";

	/**
	 * Preference key for the set of all account ids stored
	 */
    public static final String P_ACCOUNT_IDS = "accountIds";

    /**
     * Preference key for the currently selected account id. Used to fetch all
     * other account details.
     */
    public static final String P_CURRENT_ACCOUNT = "currentAccount";
    
    /**
     * Preference key for the user-visible name for an account; just a memento.
     */
    public static final String P_ACCOUNT_NAME = "accountName";

    public static final String ACCOUNT_ID_SEPARATOR = "|";
    public static final String ACCOUNT_ID_SEPARATOR_REGEX = "\\|";
    public static final String DEFAULT_ACCOUNT_NAME = "default";
    public static final String DEFAULT_ACCOUNT_NAME_BASE_64 = ObfuscatingStringFieldEditor
            .encodeString(DEFAULT_ACCOUNT_NAME);
    
}
