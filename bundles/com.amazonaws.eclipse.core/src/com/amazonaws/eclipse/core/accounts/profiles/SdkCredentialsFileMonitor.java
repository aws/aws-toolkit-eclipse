/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.accounts.profiles;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * A preference store property change listener that tracks the latest
 * configuration of the credentials file' location. It also manages a file
 * monitor that tracks any modification to the file's content.
 */
public class SdkCredentialsFileMonitor implements IPropertyChangeListener {

    /**
     * The preference store instance whose property change is being tracked by
     * this monitor.
     */
    private IPreferenceStore prefStore;

    /**
     * The file monitor that tracks modification to the credentials file's
     * content
     */
    private SdkCredentialsFileContentMonitor fileContentMonitor;


    /**
     * @param prefStore
     *            the preference store where the credentials file's location is
     *            configured.
     */
    public void start(IPreferenceStore prefStore) {

        // Stop listening to preference property updates while configuring the
        // internals
        if (this.prefStore != null) {
            this.prefStore.removePropertyChangeListener(this);
        }

        // Spins up a new content monitor on the location that is currently
        // configured in the preference store.
        String location = prefStore.getString(PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION);
        resetFileContentMonitor(location);

        // Now we are done -- start listening to preference property changes
        prefStore.addPropertyChangeListener(this);
    }

    private void resetFileContentMonitor(String fileLocation) {

        // Stop the existing content monitor, if any
        if (fileContentMonitor != null) {
            fileContentMonitor.stop();
            fileContentMonitor = null;
        }

        File file = new File(fileLocation);
        fileContentMonitor = new SdkCredentialsFileContentMonitor(file);
        fileContentMonitor.start();
    }

    /**
     * When the credentials file location is changed in the preference store,
     * reset the content monitor to track the file at the new location.
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {

        String propertyName = event.getProperty();
        if ( !PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION.equals(propertyName) ) {
            return;
        }

        String newLocation = (String)event.getNewValue();
        resetFileContentMonitor(newLocation);
    }

}
