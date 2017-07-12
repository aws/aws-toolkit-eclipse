/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.upload.wizard.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

@SuppressWarnings("restriction")
public class FunctionJarExportHelper {

    public static File exportProjectToJarFile(IProject project, boolean logInfo) {

        JarPackageData jarExportOps = new JarPackageData();
        jarExportOps.setExportJavaFiles(false);
        jarExportOps.setExportClassFiles(true);
        jarExportOps.setIncludeDirectoryEntries(true);
        jarExportOps.setUsesManifest(false);
        jarExportOps.setOverwrite(true);
        jarExportOps.setJarBuilder(new LambdaFunctionJarBuilder());

        try {

            Object[] elements = getElementsToExport(project);
            jarExportOps.setElements(elements);

            // prefix should be at least three characters long
            File jarFile = File.createTempFile(project.getName() + "-lambda", ".zip");
            jarFile.deleteOnExit();

            jarExportOps.setJarLocation(new Path(jarFile.getAbsolutePath()));

            if (logInfo) {
                LambdaPlugin.getDefault().logInfo(
                        String.format("Exporting project [%s] to %s",
                                project.getName(), jarFile.getAbsolutePath()));
            }

            IJarExportRunnable runnable = jarExportOps
                    .createJarExportRunnable(null);
            runnable.run(null);

            if (logInfo) {
                LambdaPlugin.getDefault().logInfo("Project exported to " + jarFile.getAbsolutePath());
            }

            return jarFile;

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    String.format("Unable to export project [%s] to jar file",
                            project.getName()), e);
            return null;
        }
    }

    private static Object[] getElementsToExport(IProject project)
            throws CoreException {

        IJavaProject javaProj = JavaCore.create(project);

        try {
            ILaunchConfiguration launchConfiguration = createLaunchConfigurationForProject(javaProj);
            return getSelectedElementsWithoutContainedChildren(launchConfiguration);

        } catch (CoreException e) {
            LambdaPlugin.getDefault().logWarning(
                    "Unable to resolve dependencies of project "
                            + project.getName(), e);

            // Fall back to export all the file resource inside the project
            return project.members(IResource.FILE);
        }
    }

    private static ILaunchConfiguration createLaunchConfigurationForProject(
            IJavaProject javaProject) throws CoreException {

        DebugPlugin plugin = DebugPlugin.getDefault();
        ILaunchManager manager = plugin.getLaunchManager();

        ILaunchConfigurationType javaAppType = manager
                .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        ILaunchConfigurationWorkingCopy wc = javaAppType.newInstance(null, "temp-config");

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                javaProject.getElementName());
        return wc;
    }

    /*
     * ********************************************************************************************
     * START OF SOURCE EXTRACTED FROM org.eclipse.jdt.internal.ui.jarpackager.JarPackageWizardPage
     * ********************************************************************************************
     */

    private static Object[] getSelectedElementsWithoutContainedChildren(ILaunchConfiguration launchconfig) throws CoreException {
        if (launchconfig == null)
            return new Object[0];

        String projectName= launchconfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$

        IPath[] classpath= getClasspath(launchconfig);
        IPackageFragmentRoot[] classpathResources= getRequiredPackageFragmentRoots(classpath, projectName);

        return classpathResources;
    }

    private static IPath[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        IRuntimeClasspathEntry[] entries= JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
        entries= JavaRuntime.resolveRuntimeClasspath(entries, configuration);

        ArrayList<IPath> userEntries= new ArrayList<>(entries.length);
        for (int i= 0; i < entries.length; i++) {
            if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {

                String location= entries[i].getLocation();
                if (location != null) {
                    IPath entry= Path.fromOSString(location);
                    if (!userEntries.contains(entry)) {
                        userEntries.add(entry);
                    }
                }
            }
        }
        return userEntries.toArray(new IPath[userEntries.size()]);
    }

    /**
     * @param classpathEntries the path to the package fragment roots
     * @param projectName the root of the project dependency tree
     * @return all package fragment roots corresponding to each classpath entry start the search at project with projectName
     */
    private static IPackageFragmentRoot[] getRequiredPackageFragmentRoots(IPath[] classpathEntries, final String projectName) {
        HashSet<IPackageFragmentRoot> result= new HashSet<>();

        IJavaProject[] searchOrder= getProjectSearchOrder(projectName);

        for (int i= 0; i < classpathEntries.length; i++) {
            IPath entry= classpathEntries[i];
            IPackageFragmentRoot[] elements= findRootsForClasspath(entry, searchOrder);
            if (elements != null) {
                for (int j= 0; j < elements.length; j++) {
                    result.add(elements[j]);
                }
            }
        }

        return result.toArray(new IPackageFragmentRoot[result.size()]);
    }

    private static IJavaProject[] getProjectSearchOrder(String projectName) {

        ArrayList<String> projectNames= new ArrayList<>();
        projectNames.add(projectName);

        int nextProject= 0;
        while (nextProject < projectNames.size()) {
            String nextProjectName= projectNames.get(nextProject);
            IJavaProject jproject= getJavaProject(nextProjectName);

            if (jproject != null) {
                try {
                    String[] childProjectNames= jproject.getRequiredProjectNames();
                    for (int i= 0; i < childProjectNames.length; i++) {
                        if (!projectNames.contains(childProjectNames[i])) {
                            projectNames.add(childProjectNames[i]);
                        }
                    }
                } catch (JavaModelException e) {
                    JavaPlugin.log(e);
                }
            }
            nextProject+= 1;
        }

        ArrayList<IJavaProject> result= new ArrayList<>();
        for (int i= 0, size= projectNames.size(); i < size; i++) {
            String name= projectNames.get(i);
            IJavaProject project= getJavaProject(name);
            if (project != null)
                result.add(project);
        }

        return result.toArray(new IJavaProject[result.size()]);
    }

    private static IJavaProject getJavaProject(String projectName) {
        IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project == null)
            return null;

        IJavaProject result= JavaCore.create(project);
        if (result == null)
            return null;

        if (!result.exists())
            return null;

        return result;
    }

    private static IPackageFragmentRoot[] findRootsForClasspath(IPath entry, IJavaProject[] searchOrder) {
        for (int i= 0; i < searchOrder.length; i++) {
            IPackageFragmentRoot[] elements= findRootsInProject(entry, searchOrder[i]);
            if (elements.length != 0) {
                return elements;
            }
        }
        return null;
    }

    private static IPackageFragmentRoot[] findRootsInProject(IPath entry, IJavaProject project) {
        ArrayList<IPackageFragmentRoot> result= new ArrayList<>();

        try {
            IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
            for (int i= 0; i < roots.length; i++) {
                IPackageFragmentRoot packageFragmentRoot= roots[i];
                if (isRootAt(packageFragmentRoot, entry))
                    result.add(packageFragmentRoot);
            }
        } catch (Exception e) {
            JavaPlugin.log(e);
        }

        return result.toArray(new IPackageFragmentRoot[result.size()]);
    }

    private static boolean isRootAt(IPackageFragmentRoot root, IPath entry) {
        try {
            IClasspathEntry cpe= root.getRawClasspathEntry();
            if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath outputLocation= cpe.getOutputLocation();
                if (outputLocation == null)
                    outputLocation= root.getJavaProject().getOutputLocation();

                IPath location= ResourcesPlugin.getWorkspace().getRoot().findMember(outputLocation).getLocation();
                if (entry.equals(location))
                    return true;
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }

        IResource resource= root.getResource();
        if (resource != null && entry.equals(resource.getLocation()))
            return true;

        IPath path= root.getPath();
        if (path != null && entry.equals(path))
            return true;

        return false;
    }

    /*
     * ********************************************************************************************
     * END OF SOURCE EXTRACTED FROM org.eclipse.jdt.internal.ui.jarpackager.JarPackageWizardPage
     * ********************************************************************************************
     */
}
