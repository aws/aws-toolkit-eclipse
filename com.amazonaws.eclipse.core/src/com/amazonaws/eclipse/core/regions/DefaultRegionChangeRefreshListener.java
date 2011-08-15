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
package com.amazonaws.eclipse.core.regions;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceChangeRefreshListener;

import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * Preference change refresh listener implementation that detects changes to the
 * default region preference, which requires objects to refresh their data.
 */
public class DefaultRegionChangeRefreshListener extends PreferenceChangeRefreshListener {

    /**
     * Constructs a new preference change refresh listener ready to refresh the
     * specified refreshable object when the default region preference is
     * changed.
     * 
     * @param refreshable
     *            The object to refresh when the default region preference is
     *            changed.
     */
	public DefaultRegionChangeRefreshListener(IRefreshable refreshable) {
		super(refreshable,
		        AwsToolkitCore.getDefault().getPreferenceStore(),
				new String[] {
		            com.amazonaws.eclipse.core.preferences.PreferenceConstants.P_DEFAULT_REGION,
                });
	}
}
