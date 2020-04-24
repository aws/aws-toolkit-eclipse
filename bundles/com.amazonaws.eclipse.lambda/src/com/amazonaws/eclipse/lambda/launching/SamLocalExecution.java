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
package com.amazonaws.eclipse.lambda.launching;

import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PROJECT;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class SamLocalExecution implements ILaunchShortcut {
    private static final String SAM_LOCAL_LAUNCH_CONFIGURATION_ID = "com.amazonaws.eclipse.lambda.launching.samLocal";

    /**
     * Launch SAM Local from project explorer
     */
    @Override
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection ) selection;
            Object firstElement = structuredSelection.getFirstElement();
            if (firstElement instanceof IJavaElement) {
                launch((IJavaElement)firstElement, LaunchMode.fromValue(mode));
            }
        }
    }

    /**
     * Launch SAM Local from file editor
     */
    @Override
    public void launch(IEditorPart editor, String mode) {
        IEditorInput editorInput = editor.getEditorInput();
        if (editorInput instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput)editorInput).getFile();
            IJavaElement element = JavaCore.create(file);
            if (element != null) {
                launch(element, LaunchMode.fromValue(mode));
            }
        }
    }

    /**
     * Central place for all entries of invoking SAM Local.
     */
    public static void launch(IJavaElement javaElement, LaunchMode mode) {
        try {
            IProject project = javaElement.getJavaProject().getProject();
            ILaunchConfiguration configuration = getLaunchConfiguration(project, mode);
            if (configuration == null) {
                configuration = createLaunchConfiguration(project, mode);
                ILaunchGroup group = DebugUITools.getLaunchGroup(configuration, mode.getMode());
                DebugUITools.openLaunchConfigurationDialog(Display.getCurrent().getActiveShell(),
                        configuration, group.getIdentifier(), null);
            } else {
                DebugUITools.launch(configuration, mode.getMode());
            }
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException("Failed to run SAM Locol",
                    new AwsActionException(AwsToolkitMetricType.SAMLOCAL_LAUNCH.getName(), e.getMessage(), e));
        }
    }

    /**
     * Create a new {@link ILaunchConfiguration} for the specified project.
     */
    private static ILaunchConfiguration createLaunchConfiguration(IContainer basedir, LaunchMode mode) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(SAM_LOCAL_LAUNCH_CONFIGURATION_ID);

        String configName = launchManager.generateLaunchConfigurationName("AWS SAM Local " + basedir.getName());
        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, configName);

        workingCopy.setAttribute(A_PROJECT, basedir.getName());

        return workingCopy.doSave();
    }

    /**
     * Returns an existing {@link ILaunchConfiguration} based on the provided project.
     *
     * @return The only existing one or the chosen one {@link ILaunchConfiguration}, or null if no one exists.
     * @throws CoreException
     */
    private static ILaunchConfiguration getLaunchConfiguration(IContainer basedir, LaunchMode mode) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(SAM_LOCAL_LAUNCH_CONFIGURATION_ID);

        if (launchConfigurationType == null) {
            return null;
        }

        // Scan existing launch configurations
        ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);

        List<ILaunchConfiguration> matchingConfigs = Arrays.stream(launchConfigurations).filter(config -> {
            try {
                String projectName = config.getAttribute(A_PROJECT, (String) null);
                return projectName != null && projectName.equals(basedir.getName());
            } catch (CoreException e) {
                return false;
            }
        }).collect(Collectors.toList());

        if (matchingConfigs.size() == 1) {
            return matchingConfigs.get(0);
        } else if (matchingConfigs.size() > 1) {
            IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
            ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                    AwsToolkitCore.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), labelProvider);
            dialog.setElements(matchingConfigs.toArray(new ILaunchConfiguration[matchingConfigs.size()]));
            dialog.setTitle("Select SAM Local Configuration");
            dialog.setMessage("Choose which launch profile to use");
            dialog.setMultipleSelection(false);
            int result = dialog.open();
            labelProvider.dispose();
            return result == Window.OK ? (ILaunchConfiguration) dialog.getFirstResult() : null;
        }
        return null;
    }

    public static enum LaunchMode {
        RUN("run"),
        DEBUG("debug"),
        ;

        private final String mode;
        private LaunchMode(String mode) {
            this.mode = mode;
        }

        public static LaunchMode fromValue(String mode) {
            for (LaunchMode launchMode : LaunchMode.values()) {
                if (launchMode.getMode().equals(mode)) {
                    return launchMode;
                }
            }
            throw new IllegalArgumentException(mode + " is not a valid mode for running AWS SAM Local.");
        }

        public String getMode() {
            return mode;
        }
    }
}
