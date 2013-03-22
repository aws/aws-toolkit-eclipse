/*
 * Copyright 2009-2012 Amazon Technologies, Inc. 
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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * Listens for AWS account configuration changes in the system preferences and
 * refreshes the IRefreshable object specified in the constructor when a
 * preference changes that affects how the plugins authenticate with AWS.
 */
public class AccountPreferenceChangeRefreshListener extends PreferenceChangeRefreshListener {
	
	/**
	 * The set of AWS account preferences in the AWS Toolkit Core plugin that
	 * require data refreshes when they change.
	 */
	public static final String[] ACCOUNT_PREFERENCES_REQURING_DATA_REFRESHES = new String[] {
		PreferenceConstants.P_ACCESS_KEY,
		PreferenceConstants.P_SECRET_KEY,
		PreferenceConstants.P_USER_ID,
	};
	
	/**
	 * Constructs a new account info property change listener that will refresh
	 * the specified refreshable object when it receives a notification that one
	 * of the AWS account preferences changed.
	 * 
	 * @param refreshable
	 *            The object to refresh when an AWS account preference change
	 *            occurs.
	 */
	public AccountPreferenceChangeRefreshListener(IRefreshable refreshable) {
		super(refreshable,
				AwsToolkitCore.getDefault().getPreferenceStore(), 
				ACCOUNT_PREFERENCES_REQURING_DATA_REFRESHES);
	}
	
}
