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

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.ui.preferences.ObfuscatingStringFieldEditor;

/**
 * Preference constants for the AWS Toolkit Core plugin.
 */
public class PreferenceConstants {

    /* Preference keys that are used as suffix for a specific accountId */

    /** Preference key for a user's AWS user ID. */
    public static final String P_USER_ID = "userId";

    /**
     * Preference key for the user-visible name for an account; just a memento.
     */
    public static final String P_ACCOUNT_NAME = "accountName";

    /** Preference key for a user's AWS secret key. */
    public static final String P_SECRET_KEY = "secretKey";

    /** Preference key for a user's AWS access key. */
    public static final String P_ACCESS_KEY = "accessKey";

    /** Preference key for a user's EC2 private key file. */
    public static final String P_PRIVATE_KEY_FILE = "privateKeyFile";

    /** Preference key for a user's EC2 certificate file. */
    public static final String P_CERTIFICATE_FILE = "certificateFile";

    /* "Real" preference keys */

    /**
     * Preference key for the ID of the default region.
     */
    public static final String P_DEFAULT_REGION = "defaultRegion";

    /**
     * Preference key for the "|"-separated String of all the regions configured
     * with a region-specific default accounts (which corresponds to all the
     * region tabs to be shown in account preference page).
     */
    public static final String P_REGIONS_WITH_DEFAULT_ACCOUNTS = "regionsWithDefaultAccounts";

    /**
     * Preference key indicating whether the preferences from the EC2 plugin
     * preference store have been imported yet.
     */
    public static final String P_EC2_PREFERENCES_IMPORTED = "ec2PreferencesImported";

    /**
     * Preference key for the set of all account ids configured as *global*
     * default accounts.
     */
    public static final String P_ACCOUNT_IDS = "accountIds";

    /**
     * Returns the preference key for the set of all account ids configured as
     * default accounts for the given region.
     */
    public static String P_ACCOUNT_IDS(Region region) {
        return region == null ?
                P_ACCOUNT_IDS
                :
                P_ACCOUNT_IDS + "-" + region.getId();
    }

    /**
     * Preference key for the currently selected account id. Used to fetch all
     * other account details.
     */
    public static final String P_CURRENT_ACCOUNT = "currentAccount";

    /**
     * Preference key for the current global default account id. The value of
     * this preference property will be initialized by the P_CURRENT_ACCOUNT
     * property value, if the user migrates from global-accounts-only
     * preferences.
     */
    public static final String P_GLOBAL_CURRENT_DEFAULT_ACCOUNT = "currentDefaultAccount";

    public static final String P_CONNECTION_TIMEOUT = "connectionTimeout";
    public static final String P_SOCKET_TIMEOUT = "socketTimeout";

    /**
     * Returns the preference key for the selected default account for the given
     * region.
     */
    public static String P_REGION_CURRENT_DEFAULT_ACCOUNT(Region region) {
        return region == null ?
                P_GLOBAL_CURRENT_DEFAULT_ACCOUNT
                :
                P_GLOBAL_CURRENT_DEFAULT_ACCOUNT + "-" + region.getId();
    }

    /**
     * Returns the preference key for whether region-default accounts are
     * enabled for the given region.
     */
    public static String P_REGION_DEFAULT_ACCOUNT_ENABLED(Region region) {
        return "regionalAccountEnabled-" + region.getId();
    }

    /* Constants used for parsing/creating preference property values */

    public static final String ACCOUNT_ID_SEPARATOR = "|";
    public static final String ACCOUNT_ID_SEPARATOR_REGEX = "\\|";

    public static final String REGION_ID_SEPARATOR = "|";
    public static final String REGION_ID_SEPARATOR_REGEX = "\\|";

    /** The default name for newly created global accounts */
    public static final String DEFAULT_ACCOUNT_NAME = "default";

    /** Returns the default name for newly created accounts for the given region */
    public static String DEFAULT_ACCOUNT_NAME(Region region) {
        return region == null ?
                DEFAULT_ACCOUNT_NAME
                :
                DEFAULT_ACCOUNT_NAME + "-" + region.getId();
    }

    /** The B64-encoded default name for newly created global accounts */
    public static final String DEFAULT_ACCOUNT_NAME_BASE_64 = ObfuscatingStringFieldEditor
            .encodeString(DEFAULT_ACCOUNT_NAME);

    /** Returns the B64-encoded default name for newly created accounts for the given region */
    public static String DEFAULT_ACCOUNT_NAME_BASE_64(Region region) {
        return ObfuscatingStringFieldEditor
                .encodeString(DEFAULT_ACCOUNT_NAME(region));
    }
}
