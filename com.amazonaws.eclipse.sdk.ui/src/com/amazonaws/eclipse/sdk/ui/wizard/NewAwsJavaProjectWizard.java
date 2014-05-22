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
import java.io.PrintStream;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
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

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.sdk.ui.FilenameFilters;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsSdkClasspathUtils;

/**
 * A Project Wizard for creating a new Java project configured to build against the
 * AWS SDK for Java.
 */
public class NewAwsJavaProjectWizard extends NewElementWizard implements INewWizard {

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
        } catch (IOException e) {
            logError("Unable to add sample source code to the project.", e);
        } catch (CoreException e) {
            logError("Unable to add sample source code to the project.", e);
        }

        try {
            if (pageOne.getSelectedAccount() == null) {
                copyEmptyCredentialsFileToProject();
            }
        } catch (CoreException e) {
            logError("Unable to add sthe credentials file to the project.", e);
        }

        // Finally, refresh the project so that the new files show up
        try {
            pageTwo.getJavaProject().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            logError("Unable to refresh the created project.", e);
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

    private void addSamplesToProject() throws CoreException, IOException {
        IProject project = pageTwo.getJavaProject().getProject();
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath srcPath = workspaceRoot.append(project.getFullPath()).append("src");

        AccountInfo selectedAccount = pageOne.getSelectedAccount();

        for (SdkSample sample : pageOne.getSelectedSamples()) {
            for (File sampleSourceFile : sample.getPath().toFile().listFiles(new FilenameFilters.JavaSourceFilenameFilter())) {
                String sampleContent = FileUtils.readFileToString(sampleSourceFile);

                if (selectedAccount != null) {
                    sampleContent = updateSampleContentWithConfiguredProfile(sampleContent, selectedAccount);
                }

                IFileStore projectSourceFolderDestination = EFS.getLocalFileSystem().fromLocalFile(
                        srcPath.append(sampleSourceFile.getName()).toFile());
                PrintStream ps = new PrintStream(
                        projectSourceFolderDestination.openOutputStream(
                                EFS.OVERWRITE, null));
                ps.print(sampleContent);
                ps.close();
            }
        }
    }

    /**
     * Copy the empty credentials file from the SDK sample to the created
     * project.
     */
    private void copyEmptyCredentialsFileToProject() throws CoreException {
        IProject project = pageTwo.getJavaProject().getProject();
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath srcPath = workspaceRoot.append(project.getFullPath()).append("src");

        for (SdkSample sample : pageOne.getSelectedSamples()) {
            for (File sampleSourceFile : sample.getPath().toFile().listFiles(new FilenameFilters.CredentialsFilenameFilter())) {
                IFileStore projectSourceFolderDestination = EFS.getLocalFileSystem().fromLocalFile(
                        srcPath.append(sampleSourceFile.getName()).toFile());
                EFS.getLocalFileSystem().fromLocalFile(sampleSourceFile).copy(
                        projectSourceFolderDestination, EFS.OVERWRITE, null);
            }
        }
    }

    private static String updateSampleContentWithConfiguredProfile(String sampleContent, final AccountInfo selectedAccount) {
        String paramString;
        if (AwsToolkitCore.getDefault().getPreferenceStore().isDefault(
                PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)) {
            // Don't need to specify the file location
            paramString = String.format("\"%s\"", selectedAccount.getAccountName());

        } else {
            paramString = String.format("\"%s\", \"%s\"",
                    AwsToolkitCore.getDefault().getPreferenceStore().getString(
                            PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION),
                    selectedAccount.getAccountName())
                        .replace("\\", "\\\\"); // escape backslashes
        }

        // Change the parameter of the ProfileCredentialsProvider
        sampleContent = sampleContent.replace(
                "new ProfileCredentialsProvider().getCredentials();",
                String.format("new ProfileCredentialsProvider(%s).getCredentials();",
                        paramString));

        // Remove the block of comment between "Before running the code" and "WARNING"
        String COMMNET_TO_REMOVE_REGEX = "(Before running the code:.*?)?Fill in your AWS access credentials.*?(?=WANRNING:)";
        sampleContent = Pattern.compile(COMMNET_TO_REMOVE_REGEX, Pattern.DOTALL) // dot should match newline
                .matcher(sampleContent).replaceAll("");

        // [default] ==> [selected-profile-name]
        sampleContent = sampleContent.replace(
                "[default]",
                String.format("[%s]", selectedAccount.getAccountName()));

        // (~/.aws/credentials) ==> (user-specified preference store value)
        sampleContent = sampleContent.replace(
                "(~/.aws/credentials)",
                String.format("(%s)", AwsToolkitCore.getDefault().getPreferenceStore().getString(
                        PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)));

        return sampleContent;
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

    private void logError(String errMsg, Throwable t) {
        AwsToolkitCore.getDefault().logException(errMsg, t);
    }
}
