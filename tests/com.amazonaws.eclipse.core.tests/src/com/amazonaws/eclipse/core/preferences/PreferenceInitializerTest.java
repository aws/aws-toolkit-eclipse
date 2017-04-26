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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

/**
 * Unit tests for the preference initializer to verify that it correctly imports
 * any pre-existing preferences from the EC2 plugin.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class PreferenceInitializerTest {

    /**
     * If the EC2 plugin preferences haven't been imported yet, we expect them
     * to be pulled from the EC2 plugin preference store and imported into the
     * AWS Toolkit Core preference store.
     */
    @Test
    public void testImportPreferences() {
        MockPreferenceInitializer preferenceInitializer = new MockPreferenceInitializer();
        IPreferenceStore preferenceStore = preferenceInitializer.getAwsToolkitCorePreferenceStore();

        assertEquals("", preferenceStore.getString(PreferenceConstants.P_ACCESS_KEY));
        assertFalse(preferenceStore.getBoolean(PreferenceConstants.P_EC2_PREFERENCES_IMPORTED));

        preferenceInitializer.initializeDefaultPreferences();

        assertEquals("accessKey", preferenceStore.getString(PreferenceConstants.P_ACCESS_KEY));
        assertEquals("secretKey", preferenceStore.getString(PreferenceConstants.P_SECRET_KEY));
        assertEquals("userId", preferenceStore.getString(PreferenceConstants.P_USER_ID));
        assertEquals("certFile", preferenceStore.getString(PreferenceConstants.P_CERTIFICATE_FILE));
        assertEquals("privateKey", preferenceStore.getString(PreferenceConstants.P_PRIVATE_KEY_FILE));
        assertTrue(preferenceStore.getBoolean(PreferenceConstants.P_EC2_PREFERENCES_IMPORTED));
    }

    /**
     * If the EC2 plugin preferences have already been imported, we expect them
     * to not be imported again.
     */
    @Test
    public void testOnlyImportOnce() {
        MockPreferenceInitializer preferenceInitializer = new MockPreferenceInitializer();
        IPreferenceStore preferenceStore = preferenceInitializer.getAwsToolkitCorePreferenceStore();

        preferenceStore.setValue(PreferenceConstants.P_EC2_PREFERENCES_IMPORTED, true);
        assertEquals("", preferenceStore.getString(PreferenceConstants.P_ACCESS_KEY));

        preferenceInitializer.initializeDefaultPreferences();
        assertEquals("", preferenceStore.getString(PreferenceConstants.P_ACCESS_KEY));
    }


    /**
     * Subclass of AWS Toolkit Core's preference initializer that stubs out real
     * EC2 and AWS Toolkit Core preference stores for easy testing.
     */
    private static class MockPreferenceInitializer extends PreferenceInitializer {

        private IPreferenceStore awsToolkitCorePreferenceStore = new PreferenceStore();
        private IPreferenceStore ec2PreferenceStore = new PreferenceStore();

        MockPreferenceInitializer() {
            ec2PreferenceStore.setValue(PreferenceConstants.P_ACCESS_KEY, "accessKey");
            ec2PreferenceStore.setValue(PreferenceConstants.P_SECRET_KEY, "secretKey");
            ec2PreferenceStore.setValue(PreferenceConstants.P_USER_ID, "userId");
            ec2PreferenceStore.setValue(PreferenceConstants.P_CERTIFICATE_FILE, "certFile");
            ec2PreferenceStore.setValue(PreferenceConstants.P_PRIVATE_KEY_FILE, "privateKey");
        }

        @Override
        protected IPreferenceStore getEc2PluginPreferenceStore() {
            return ec2PreferenceStore;
        }

        @Override
        protected IPreferenceStore getAwsToolkitCorePreferenceStore() {
            return awsToolkitCorePreferenceStore;
        }

    }

}
