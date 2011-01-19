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
package com.amazonaws.eclipse.ec2.preferences;

import com.amazonaws.eclipse.core.preferences.PreferenceChangeRefreshListener;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Preference change refresh listener implementation that detects changes to the
 * Amazon EC2 plugin preferences that require objects to refresh their data (ex:
 * changing the current EC2 region).
 */
public class Ec2PreferenceChangeRefreshListener extends PreferenceChangeRefreshListener {

	/**
	 * Constructs a new EC2 preference change refresh listener ready to refresh
	 * the specified refreshable object when any relevant EC2 plugin preferences
	 * change.
	 *
	 * @param refreshable
	 *            The object to refresh when a relevant EC2 plugin preference
	 *            changes.
	 */
	public Ec2PreferenceChangeRefreshListener(IRefreshable refreshable) {
        /*
         * We need to listen for just the Region endpoint, otherwise we'll
         * refresh the views too many times and introduce race conditions.
         */
		super(refreshable,
				Ec2Plugin.getDefault().getPreferenceStore(),
				new String[] {
                	com.amazonaws.eclipse.ec2.preferences.PreferenceConstants.P_EC2_REGION_ENDPOINT,
                });
	}

}
