package com.amazonaws.eclipse.android.sdk.preferences;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager;
import com.amazonaws.eclipse.sdk.ui.preferences.AbstractSDKPreferencesPage;

public class AndroidSDKPreferencePage extends AbstractSDKPreferencesPage implements IWorkbenchPreferencePage {

    public static final String ID = "com.amazonaws.eclipse.android.sdk.preferences.AndroidSDKPreferencePage";            

    public AndroidSDKPreferencePage() {
        super("AWS SDK For Android Preferences");
    }

    protected String getDownloadAutomaticallyPreferenceName() {
        return PreferenceConstants.DOWNLOAD_AUTOMATICALLY;
    }

    protected String getDownloadDirectoryPreferenceName() {
        return PreferenceConstants.DOWNLOAD_DIRECTORY;
    }

    protected void checkForSDKUpdates() {
        AndroidSdkManager.getInstance().initializeSDKInstalls();
    }

    public void init(IWorkbench workbench) {
        setPreferenceStore(AndroidSDKPlugin.getDefault().getPreferenceStore());
    }
}
