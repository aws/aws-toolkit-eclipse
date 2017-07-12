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
package com.amazonaws.eclipse.core.accounts;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.eclipse.core.preferences.AbstractPreferencePropertyMonitor;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;

/**
 * Responsible for monitoring for changes on default accounts.
 * This includes either changing to another global/regional default account,
 * or enable/disable a regional default account.
 */
public class DefaultAccountMonitor extends AbstractPreferencePropertyMonitor{

    private static final Set<String> watchedProperties = new HashSet<>();
    static {
        watchedProperties.add(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT);
        watchedProperties.add(PreferenceConstants.P_REGIONS_WITH_DEFAULT_ACCOUNTS);
        for (Region region : RegionUtils.getRegions()) {
            watchedProperties.add(PreferenceConstants.P_REGION_CURRENT_DEFAULT_ACCOUNT(region));
            watchedProperties.add(PreferenceConstants.P_REGION_DEFAULT_ACCOUNT_ENABLED(region));
        }
    }

    public DefaultAccountMonitor() {
        super(0); // no delay
    }

    @Override
    protected boolean watchPreferenceProperty(String preferenceKey) {
        return watchedProperties.contains(preferenceKey);
    }
}
