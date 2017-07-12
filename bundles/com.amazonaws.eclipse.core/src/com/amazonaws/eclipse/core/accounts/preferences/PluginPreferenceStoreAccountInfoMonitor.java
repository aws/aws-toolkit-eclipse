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
package com.amazonaws.eclipse.core.accounts.preferences;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.eclipse.core.preferences.AbstractPreferencePropertyMonitor;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Responsible for monitoring for account info (access/secret keys, etc) changes
 * and notifying listeners.
 */
@SuppressWarnings("deprecation")
public final class PluginPreferenceStoreAccountInfoMonitor extends AbstractPreferencePropertyMonitor {

    private static final long NOTIFICATION_DELAY = 1000L;

    private static final Set<String> watchedProperties = new HashSet<>();
    static {
        watchedProperties.add(PreferenceConstants.P_ACCESS_KEY);
        watchedProperties.add(PreferenceConstants.P_ACCOUNT_IDS);
        watchedProperties.add(PreferenceConstants.P_CERTIFICATE_FILE);
        watchedProperties.add(PreferenceConstants.P_CURRENT_ACCOUNT);
        watchedProperties.add(PreferenceConstants.P_PRIVATE_KEY_FILE);
        watchedProperties.add(PreferenceConstants.P_SECRET_KEY);
        watchedProperties.add(PreferenceConstants.P_USER_ID);
    }

    public PluginPreferenceStoreAccountInfoMonitor() {
        super(NOTIFICATION_DELAY);
    }

    @Override
    protected boolean watchPreferenceProperty(String preferenceKey) {
        String bareProperty = preferenceKey.substring(preferenceKey.indexOf(":") + 1);
        return watchedProperties.contains(bareProperty);
    }
}
