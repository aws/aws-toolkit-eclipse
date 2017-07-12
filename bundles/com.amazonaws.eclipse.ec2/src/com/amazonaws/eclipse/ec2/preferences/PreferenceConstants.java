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

/**
 * Constant definitions for plug-in preference keys.
 */
public class PreferenceConstants {

    /** Preference key for the path to the PuTTY executable on Windows. */
    public static final String P_PUTTY_EXECUTABLE = "puttyExecutable";

    /** Preference key for the path to the terminal executable. */
    public static final String P_TERMINAL_EXECUTABLE = "terminalExecutable";
    
    /** Preference key for the path to the ssh executable. */
    public static final String P_SSH_CLIENT = "sshExecutable";

    /** Preference key for additional SSH command line options. */
    public static final String P_SSH_OPTIONS = "sshOptions";
    
    /** Preference key for the SSH user to log in as */
    public static final String P_SSH_USER = "sshUser";

}
