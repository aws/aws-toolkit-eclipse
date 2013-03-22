/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.sdk.ui.wizard;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.sdk.ui.FilenameFilters;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsSdkClasspathUtils;

/**
 * A Project Wizard for creating a new Java project configured to build against the
 * AWS SDK for Java.
 */
public class NewAwsJavaProjectWizard extends NewElementWizard implements INewWizard {

    private static final String AWS_CREDENTIALS_URL = "http://aws.amazon.com/security-credentials";
    private static final String AWS_CREDENTIALS_PROPERTIES_FILE = "AwsCredentials.properties";
    private NewAwsJavaProjectWizardPageOne pageOne;
    private NewAwsJavaProjectWizardPageTwo pageTwo;

    /**
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        if (pageOne == null)
            pageOne = new NewAwsJavaProjectWizardPageOne();
        addPage(pageOne);

        if (pageTwo == null)
            pageTwo = new NewAwsJavaProjectWizardPageTwo(pageOne);
        addPage(pageTwo);

        pageOne.init(getSelection(), getActivePart());
    }

    public NewAwsJavaProjectWizard() {
        this(null, null);
    }

    public NewAwsJavaProjectWizard(NewAwsJavaProjectWizardPageOne pageOne, NewAwsJavaProjectWizardPageTwo pageTwo) {
        setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
        setWindowTitle("New AWS Java Project");
        setDefaultPageImageDescriptor(
                AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
    }

    /**
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void finishPage(IProgressMonitor monitor)
            throws InterruptedException, CoreException {
        pageTwo.performFinish(monitor);

        monitor.setTaskName("Configuring AWS Java project");
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                configureNewProject();
            }
        });
    }

    private void configureNewProject() {
        final IJavaProject javaProject = pageTwo.getJavaProject();
        selectAndReveal(javaProject.getProject());

        IWorkbenchPart activePart = getActivePart();
        if (activePart instanceof IPackagesViewPart) {
            PackageExplorerPart view = PackageExplorerPart.openInActivePerspective();
            view.tryToReveal(javaProject);
        }

        addAwsSdkToProject();

        try {
            addSamplesToProject();
        } catch (CoreException e) {
            e.printStackTrace();
        }

        try {
            addCredentialsToProject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }

        // Finally, refresh the project so that the new files show up
        try {
            pageTwo.getJavaProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
     */
    @Override
    public IJavaElement getCreatedElement() {
        return pageTwo.getJavaProject();
    }


    /*
     * Private Interface
     */

    private void addAwsSdkToProject() {
        IJavaProject javaProject = pageTwo.getJavaProject();

        // TODO: What about error handling... should we propagate it up higher?
        AwsSdkClasspathUtils.addAwsSdkToProjectClasspath(
                javaProject, pageOne.getSelectedSdkInstall());
    }

    private void addSamplesToProject() throws CoreException {
        IProject project = pageTwo.getJavaProject().getProject();
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath projectPath = workspaceRoot.append(project.getFullPath());

        for (SdkSample sample : pageOne.getSelectedSamples()) {
            for (File sampleSourceFile : sample.getPath().toFile().listFiles(new FilenameFilters.JavaSourceFilenameFilter())) {
                IFileStore projectSourceFolderDestination = EFS.getLocalFileSystem().fromLocalFile(
                                projectPath.append("src").append(sampleSourceFile.getName()).toFile());
                EFS.getLocalFileSystem().fromLocalFile(sampleSourceFile).copy(
                        projectSourceFolderDestination, EFS.OVERWRITE, null);
            }
        }
    }

    private void addCredentialsToProject() throws IOException, CoreException {
        IProject project = pageTwo.getJavaProject().getProject();
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath srcPath = workspaceRoot.append(project.getFullPath()).append("src");

        Properties credentialProperties = new Properties();
        credentialProperties.setProperty("accessKey", pageOne.getAccessKey());
        credentialProperties.setProperty("secretKey", pageOne.getSecretKey());

        IFileStore credentialPropertiesFile =
            EFS.getLocalFileSystem().fromLocalFile(srcPath.append(AWS_CREDENTIALS_PROPERTIES_FILE).toFile());
        OutputStream os = credentialPropertiesFile.openOutputStream(EFS.NONE, null);
        credentialProperties.store(os, "Insert your AWS Credentials from " + AWS_CREDENTIALS_URL);
        os.close();
    }

    private IWorkbenchPart getActivePart() {
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWindow != null) {
            IWorkbenchPage activePage = activeWindow.getActivePage();
            if (activePage != null) {
                return activePage.getActivePart();
            }
        }
        return null;
    }

    @Override
    public boolean performCancel() {
        pageTwo.performCancel();
        return super.performCancel();
    }

}
