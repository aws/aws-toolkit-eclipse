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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.plugin.AbstractAwsProjectWizard;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.sdk.ui.FilenameFilters;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.model.NewAwsJavaProjectWizardDataModel;

import static com.amazonaws.eclipse.core.util.JavaProjectUtils.setDefaultJreToProjectClasspath;

/**
 * A Project Wizard for creating a new Java project configured to build against the
 * AWS SDK for Java.
 */

public class NewAwsJavaProjectWizard extends AbstractAwsProjectWizard {

    private static final String DEFAULT_GROUP_ID = "com.amazonaws";
    private static final String DEFAULT_ARTIFACT_ID = "samples";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_PACKAGE_NAME = MavenFactory.assumePackageName(DEFAULT_GROUP_ID, DEFAULT_ARTIFACT_ID);

    private final NewAwsJavaProjectWizardDataModel dataModel = new NewAwsJavaProjectWizardDataModel();
    private final String actionSource;
    private NewAwsJavaProjectWizardPageOne pageOne;
    private IProject project;

    /**
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        if (pageOne == null)
            pageOne = new NewAwsJavaProjectWizardPageOne(dataModel);
        addPage(pageOne);
    }

    public NewAwsJavaProjectWizard() {
        this("Default");
    }

    public NewAwsJavaProjectWizard(String actionSource) {
        super("New AWS Java Project");
        this.actionSource = actionSource;
        initDataModel();
    }

    @Override
    protected void initDataModel() {
        MavenConfigurationDataModel mavenDataModel = dataModel.getMavenConfigurationDataModel();
        mavenDataModel.setGroupId(DEFAULT_GROUP_ID);
        mavenDataModel.setArtifactId(DEFAULT_ARTIFACT_ID);
        mavenDataModel.setVersion(DEFAULT_VERSION);
        mavenDataModel.setPackageName(DEFAULT_PACKAGE_NAME);
        dataModel.setActionSource(actionSource);
    }

    private void addSamplesToProject()
            throws CoreException, IOException {

        String packageName = dataModel.getMavenConfigurationDataModel().getPackageName();
        AccountInfo accountInfo = dataModel.getAccountInfo();
        List<SdkSample> samples = dataModel.getSdkSamples();

        IPath srcPath = getSamplesRootFolder(packageName);
        if (!srcPath.toFile().exists()) {
            srcPath.toFile().mkdirs();
        }

        AccountInfo selectedAccount = accountInfo;

        for (SdkSample sample : samples) {
            for (File sampleSourceFile : sample.getPath().toFile().listFiles(new FilenameFilters.JavaSourceFilenameFilter())) {
                String sampleContent = FileUtils.readFileToString(sampleSourceFile);

                if (selectedAccount != null) {
                    sampleContent = updateSampleContentWithConfiguredProfile(sampleContent, selectedAccount, packageName);
                }

                IFileStore projectSourceFolderDestination = EFS.getLocalFileSystem().fromLocalFile(
                        srcPath.append(sampleSourceFile.getName()).toFile());

                try (PrintStream ps = new PrintStream(
                        projectSourceFolderDestination.openOutputStream(
                                EFS.OVERWRITE, null))) {
                    ps.print(sampleContent);
                }
            }
        }
    }

    private Model getModel() {
        Model model = new Model();
        model.setModelVersion(MavenFactory.getMavenModelVersion());
        model.setGroupId(dataModel.getMavenConfigurationDataModel().getGroupId());
        model.setArtifactId(dataModel.getMavenConfigurationDataModel().getArtifactId());
        model.setVersion(dataModel.getMavenConfigurationDataModel().getVersion());
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(MavenFactory.getLatestAwsSdkDependency("compile"));
        dependencies.add(MavenFactory.getAmazonKinesisClientDependency("1.2.1", "compile"));
        model.setDependencies(dependencies);
        return model;
    }

    // Return the package path where all the sample files are in.
    private IPath getSamplesRootFolder(String packageName) {
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath srcPath = workspaceRoot.append(project.getFullPath()).append(MavenFactory.getMavenSourceFolder())
                .append(packageName.replace('.', '/'));
        return srcPath;
    }


    private static String updateSampleContentWithConfiguredProfile(String sampleContent, final AccountInfo selectedAccount, final String packageName) {
        final String credFileLocation = AwsToolkitCore.getDefault().getPreferenceStore().getString(
                PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION);

        String paramString;
        if (AwsToolkitCore.getDefault().getPreferenceStore().isDefault(
                PreferenceConstants.P_CREDENTIAL_PROFILE_FILE_LOCATION)) {
            // Don't need to specify the file location
            paramString = String.format("\"%s\"", selectedAccount.getAccountName());

        } else {
            paramString = String.format("\"%s\", \"%s\"",
                    credFileLocation, selectedAccount.getAccountName());
        }

        // Change the parameter of the ProfileCredentialsProvider
        sampleContent = sampleContent.replace(
                "new ProfileCredentialsProvider().getCredentials();",
                String.format("new ProfileCredentialsProvider(%s).getCredentials();",
                              escapeBackSlashes(paramString)));

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
                String.format("(%s)", escapeBackSlashes(credFileLocation)));

        sampleContent = "package " + packageName + ";\n" + sampleContent;

        return sampleContent;
    }

    private static String escapeBackSlashes(String str) {
        return str.replace("\\", "\\\\");
    }

    @Override
    protected IStatus doFinish(IProgressMonitor monitor) {

        SubMonitor progress = SubMonitor.convert(monitor, 100);
        progress.setTaskName(getJobTitle());

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject(dataModel.getProjectNameDataModel().getProjectName());
        Model mavenModel = getModel();

        try {
            MavenFactory.createMavenProject(project, mavenModel, progress.newChild(50));
            IJavaProject javaProject = JavaCore.create(project);
            setDefaultJreToProjectClasspath(javaProject, monitor);
            progress.worked(50);
        } catch (Exception e) {
            return JavaSdkPlugin.getDefault().logError(
                    "Failed to create AWS Sample Maven Project.", e);
        }
        try {
            addSamplesToProject();
            progress.worked(50);
        } catch (Exception e) {
            return JavaSdkPlugin.getDefault().logError(
                    "Failed to add samples to AWS Sample Maven Project", e);
        }

        // Finally, refresh the project so that the new files show up
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            return JavaSdkPlugin.getDefault().logWarning(
                    "Unable to refresh the created project.", e);
        }
        return Status.OK_STATUS;
    }

    @Override
    protected void afterExecution(IStatus status) {
        super.afterExecution(status);
        if (status.isOK()) {
            dataModel.actionSucceeded();
        } else {
            dataModel.actionFailed();
        }
        dataModel.setActionExecutionTimeMillis(Long.valueOf(getActionExecutionTimeMillis()));
        dataModel.publishMetrics();
    }

    @Override
    protected String getJobTitle() {
        return "Creating AWS Java Project";
    }

    @Override
    public boolean performCancel() {
        dataModel.actionCanceled();
        dataModel.publishMetrics();
        return super.performCancel();
    }

    @Override
    protected AbstractAwsPlugin getPlugin() {
        return JavaSdkPlugin.getDefault();
    }
}
