/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;

/**
 * Super class for launching an application.
 *
 * @see MavenBuildLauncher
 * @see RemoteDebugLauncher
 */
public abstract class AbstractApplicationLauncher {
    protected long timeIntervalMilli = 5000L;
    protected final String mode;
    protected final IProgressMonitor monitor;

    protected AbstractApplicationLauncher(String mode, IProgressMonitor monitor) {
        this.mode = mode;
        this.monitor = monitor;
    }

    /**
     * Validate the required parameters for launching this application.
     * @throws IllegalArgumentException - If the required parameters are not provided or invalid.
     */
    protected abstract void validateParameters() throws IllegalArgumentException;

    /**
     * Create a new {@link ILaunchConfiguration} for this application.
     */
    protected abstract ILaunchConfiguration createLaunchConfiguration() throws CoreException;

    public final ILaunch launchAsync() throws CoreException {
        validateParameters();
        ILaunchConfiguration launchConfiguration = createLaunchConfiguration();
        return DebugUITools.buildAndLaunch(launchConfiguration, mode, monitor);
    }

    /**
     * Blocking call for launching the application. This call blocks until the application terminates.
     */
    public final ILaunch launch() throws CoreException {
        ILaunch launch = launchAsync();
        while (!launch.isTerminated()) {
            try {
                Thread.sleep(timeIntervalMilli);
            } catch (InterruptedException e) {
            }
        }
        return launch;
    }
}