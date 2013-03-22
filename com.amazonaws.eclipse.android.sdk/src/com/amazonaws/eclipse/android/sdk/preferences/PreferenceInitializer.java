package com.amazonaws.eclipse.android.sdk.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public PreferenceInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        AndroidSDKPlugin.getDefault().getPreferenceStore().setDefault(PreferenceConstants.DOWNLOAD_AUTOMATICALLY, true);
        AndroidSDKPlugin
                .getDefault()
                .getPreferenceStore()
                .setDefault(PreferenceConstants.DOWNLOAD_DIRECTORY,
                        AndroidSdkManager.getInstance().getDefaultSDKInstallDir().getAbsolutePath());
    }

}
