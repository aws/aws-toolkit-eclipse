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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;

/**
 * Utility class for launching a customized Maven build.
 *
 * @see org.eclipse.m2e.actions.ExecutePomAction
 */
public class MavenBuildLauncher extends AbstractApplicationLauncher {
    private static final String POM_FILE_NAME = "pom.xml";

    private static final String executePomActionExecutingMessage(String goals, String projectLocation) {
        return String.format("Executing %s in %s", goals, projectLocation);
    }

    private final IProject project;
    private final String goals;

    public MavenBuildLauncher(IProject project, String goals, IProgressMonitor monitor) {
        super(ILaunchManager.RUN_MODE, monitor);
        this.project = project;
        this.goals = goals;
    }

    @Override
    protected ILaunchConfiguration createLaunchConfiguration() throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

        String rawConfigName = executePomActionExecutingMessage(goals, project.getLocation().toString());
        String safeConfigName = launchManager.generateLaunchConfigurationName(rawConfigName);

        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, safeConfigName);
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, project.getLocation().toOSString());
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goals);
        workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
        workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
        workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

        setProjectConfiguration(workingCopy, project);

        IPath path = getJREContainerPath(project);
        if (path != null) {
            workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                    path.toPortableString());
        }

        return workingCopy;
    }

    private void setProjectConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IFile pomFile = basedir.getFile(new Path(POM_FILE_NAME));
        IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());
        if (projectFacade != null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

            String selectedProfiles = configuration.getSelectedProfiles();
            if (selectedProfiles != null && selectedProfiles.length() > 0) {
                workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, selectedProfiles);
            }
        }
    }

    private IPath getJREContainerPath(IContainer basedir) throws CoreException {
        IProject project = basedir.getProject();
        if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
                    return entry.getPath();
                }
            }
        }
        return null;
    }

    /**
     * Validate all the provided parameters to perform a launch.
     *
     * @throws IllegalArgumentException if any parameter is not legal.
     */
    @Override
    protected void validateParameters() {
        if (project == null) {
            throw new IllegalArgumentException("The provided project cannot be null!");
        }
        if (project.findMember(POM_FILE_NAME) == null) {
            throw new IllegalArgumentException("The project must be a Maven project with a " + POM_FILE_NAME + " file in the root!");
        }
        if (goals == null || goals.isEmpty()) {
            throw new IllegalArgumentException("The goals specified must not be empty!");
        }
    }
}
