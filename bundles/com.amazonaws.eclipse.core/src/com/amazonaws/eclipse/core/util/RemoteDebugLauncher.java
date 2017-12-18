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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Launch a remote debugger.
 *
 * @see org.eclipse.m2e.internal.launch.MavenConsoleLineTracker
 */
public class RemoteDebugLauncher extends AbstractApplicationLauncher {
    private final IProject project;
    private final int portNo;

    public RemoteDebugLauncher(IProject project, int port, IProgressMonitor monitor) {
        super(ILaunchManager.DEBUG_MODE, monitor);
        this.project = project;
        this.portNo = port;
    }

    @Override
    protected ILaunchConfiguration createLaunchConfiguration() throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null,
                "Connecting debugger to port " + portNo);
        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, true);
        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
                IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);

        Map<String, String> connectMap = new HashMap<String, String>();
        connectMap.put("port", String.valueOf(portNo));
        connectMap.put("hostname", "localhost");
        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);

        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());

        return workingCopy;
    }

    @Override
    protected void validateParameters() {
        if (project == null) {
            throw new IllegalArgumentException("The project must be specified!");
        }
    }
}
