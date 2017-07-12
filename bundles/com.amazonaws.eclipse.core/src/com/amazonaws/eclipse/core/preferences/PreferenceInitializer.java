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

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.util.StringUtils;

/**
 * Responsible for initializing the preference store for the AWS Toolkit Core
 * plugin. When initializing the default values, this class will attempt to load
 * the preferences from the Amazon EC2 Eclipse plugin and import the AWS account
 * settings into this plugin's preference store.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /**
     * The preferences that should be imported from the Amazon EC2 Eclipse
     * plugin preferences (if available). These preferences were originally
     * stored in the EC2 plugin preference store, but need to be moved to the
     * AWS Toolkit Core preference store now that they are stored there.
     */
    private final String[] preferencesToImport = new String[] {
            PreferenceConstants.P_ACCESS_KEY,
            PreferenceConstants.P_SECRET_KEY,
            PreferenceConstants.P_USER_ID,
            PreferenceConstants.P_CERTIFICATE_FILE,
            PreferenceConstants.P_PRIVATE_KEY_FILE,
    };


    /*
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = getAwsToolkitCorePreferenceStore();

        /* For backwards compatibility */
        importEc2AccountPreferences();
        bootstrapAccountPreferences();

        /* System defaults */
        store.setDefault(PreferenceConstants.P_DEFAULT_REGION, "us-west-2");

        store.setDefault(PreferenceConstants.P_CONNECTION_TIMEOUT, 20 * 1000);
        store.setDefault(PreferenceConstants.P_SOCKET_TIMEOUT, 20 * 1000);

        String defaultCredentialsFile =
                System.getProperty("user.home") + File.separator
                + ".aws" + File.separator
                + "credentials";
        store.setDefault(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION, defaultCredentialsFile);

        /* Toolkit analytics is enabled by default */
        store.setDefault(
                PreferenceConstants.P_TOOLKIT_ANALYTICS_COLLECTION_ENABLED,
                true);
    }

    /**
     * Imports the AWS account preferences from the Amazon EC2 Eclipse plugin
     * and stores them in the AWS Toolkit Core's preference store. This is
     * necessary for backwards compatibility, so that users who already
     * installed and configured the Amazon EC2 Eclipse plugin don't lose their
     * AWS account information once the AWS Toolkit Core plugin is installed.
     */
    private void importEc2AccountPreferences() {
        IPreferenceStore awsToolkitPreferenceStore = getAwsToolkitCorePreferenceStore();

        /*
         * If the EC2 plugin preferences have already been imported, we don't
         * want to overwrite anything, so just bail out.
         */
        if (awsToolkitPreferenceStore.getBoolean(
                PreferenceConstants.P_EC2_PREFERENCES_IMPORTED)) {
            return;
        }

        IPreferenceStore ec2PluginPreferenceStore =    getEc2PluginPreferenceStore();

        for (String preferenceToImport : preferencesToImport) {
            String value = ec2PluginPreferenceStore.getString(preferenceToImport);
            awsToolkitPreferenceStore.setValue(preferenceToImport, value);
        }

        /*
         * Record that we imported the pre-existing EC2 plugin preferences so
         * that we know not to re-import them next time.
         */
        awsToolkitPreferenceStore.setValue(
                PreferenceConstants.P_EC2_PREFERENCES_IMPORTED, true);
    }

    /**
     * Bootstraps the current account preferences for new customers or customers
     * migrating from the legacy single-account or global-accounts-only preference
     */
    private void bootstrapAccountPreferences() {
        IPreferenceStore awsToolkitPreferenceStore = getAwsToolkitCorePreferenceStore();

        // Bootstrap customers from the global-accounts-only preference
        String globalDefaultAccount = awsToolkitPreferenceStore
                .getString(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT);

        if (StringUtils.isNullOrEmpty(globalDefaultAccount)) {
            awsToolkitPreferenceStore.putValue(
                    PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT,
                    awsToolkitPreferenceStore
                            .getString(PreferenceConstants.P_CURRENT_ACCOUNT));
        }
    }

    /**
     * Returns the preference store for the AWS Toolkit Core plugin. Primarily
     * abstracted to facilitate testing.
     *
     * @return The preference store for the AWS Toolkit Core plugin.
     */
    protected IPreferenceStore getAwsToolkitCorePreferenceStore() {
        return AwsToolkitCore.getDefault().getPreferenceStore();
    }

    /**
     * Returns the preference store for the EC2 plugin. Primarily abstracted to
     * facilitate testing.
     *
     * @return The preference store for the EC2 plugin.
     */
    protected IPreferenceStore getEc2PluginPreferenceStore() {
        return new ScopedPreferenceStore(new InstanceScope(), "com.amazonaws.eclipse.ec2");
    }

}
