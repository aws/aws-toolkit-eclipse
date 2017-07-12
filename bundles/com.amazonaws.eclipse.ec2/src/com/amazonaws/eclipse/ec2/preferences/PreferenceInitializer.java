/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /** The default ssh client for unix-ish hosts (Linux and Mac) */
    private static final String DEFAULT_UNIX_SSH_CLIENT = "/usr/bin/ssh";

    /** The default graphical terminal for Linux hosts */
    private static final String DEFAULT_LINUX_TERMINAL = "/usr/bin/gnome-terminal";

    /** The default PuTTY path on Windows */
    private static final String DEFAULT_WINDOWS_PUTTY_PATH = "C:\\Program Files\\PuTTY\\PuTTY.exe";

    /** The default SSH options for connections to EC2 instances */
    private static final String DEFAULT_SSH_OPTIONS = "-o CheckHostIP=no -o TCPKeepAlive=yes " +
            "-o StrictHostKeyChecking=no -o ServerAliveInterval=120 -o ServerAliveCountMax=100";

    /** The default SSH user for connections to EC2 instances */
    private static final String DEFAULT_SSH_USER = "ec2-user";

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Ec2Plugin.getDefault().getPreferenceStore();

        store.setDefault(PreferenceConstants.P_SSH_USER, DEFAULT_SSH_USER);

        // Windows specific preferences...
        store.setDefault(PreferenceConstants.P_PUTTY_EXECUTABLE, DEFAULT_WINDOWS_PUTTY_PATH);

        // Unix specific preferences...
        store.setDefault(PreferenceConstants.P_SSH_CLIENT, DEFAULT_UNIX_SSH_CLIENT);
        store.setDefault(PreferenceConstants.P_SSH_OPTIONS, DEFAULT_SSH_OPTIONS);

        // Linux specific preferences...
        store.setDefault(PreferenceConstants.P_TERMINAL_EXECUTABLE, DEFAULT_LINUX_TERMINAL);
    }

}
