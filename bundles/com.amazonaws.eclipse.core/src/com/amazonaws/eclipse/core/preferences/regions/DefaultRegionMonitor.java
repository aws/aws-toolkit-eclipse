/*
 * Copyright 2010-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.preferences.regions;

import com.amazonaws.eclipse.core.preferences.AbstractPreferencePropertyMonitor;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Responsible for monitoring for default region changes. We could use this
 * monitor to update the plugin to use the correct region-specific default
 * account.
 */
public class DefaultRegionMonitor extends AbstractPreferencePropertyMonitor {

    public DefaultRegionMonitor() {
        super(0); // no delay
    }

    @Override
    protected boolean watchPreferenceProperty(String preferenceKey) {
        return preferenceKey.equals(PreferenceConstants.P_DEFAULT_REGION);
    }

}
