/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.core.util.JavaProjectUtils.setDefaultJreToProjectClasspath;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;

import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.plugin.AbstractAwsProjectWizard;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.core.validator.JavaPackageName;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;

public class NewLambdaJavaFunctionProjectWizard extends AbstractAwsProjectWizard {

    private static final String DEFAULT_GROUP_ID = "com.amazonaws.lambda";
    private static final String DEFAULT_ARTIFACT_ID = "demo";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_PACKAGE_NAME = MavenFactory.assumePackageName(DEFAULT_GROUP_ID, DEFAULT_ARTIFACT_ID);

    private final LambdaFunctionWizardDataModel dataModel = new LambdaFunctionWizardDataModel();
    private NewLambdaJavaFunctionProjectWizardPageOne pageOne;

    private IProject project;

    @Override
    public void addPages() {
        if (pageOne == null) {
            pageOne = new NewLambdaJavaFunctionProjectWizardPageOne(dataModel);
        }
        addPage(pageOne);
    }

    public NewLambdaJavaFunctionProjectWizard() {
        super("New AWS Lambda Maven Project");
        initDataModel();
    }

    @Override
    protected IStatus doFinish(IProgressMonitor monitor) {

        LambdaAnalytics.trackNewProjectAttributes(dataModel);
        final String projectName = dataModel.getProjectNameDataModel().getProjectName();
        final Model mavenModel = getModel();

        File readmeFile = null;

        try {
            savePreferences(dataModel, LambdaPlugin.getDefault().getPreferenceStore());

            final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            project = root.getProject(projectName);

            try {
                MavenFactory.createMavenProject(project, mavenModel, monitor);
                IJavaProject javaProject = JavaCore.create(project);
                setDefaultJreToProjectClasspath(javaProject, monitor);
            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException(
                        "Failed to create AWS Lambda Maven Project.",
                        new AwsActionException(AwsToolkitMetricType.LAMBDA_NEW_LAMBDA_PROJECT_WIZARD.getName(), e.getMessage(), e));
            }

            FunctionProjectUtil.createLambdaBlueprintProject(project, dataModel);

            if (dataModel.isShowReadmeFile()) {
                readmeFile = FunctionProjectUtil.emitLambdaProjectReadme(project, dataModel.getLambdaFunctionDataModel());
            }

            FunctionProjectUtil.refreshProject(project);
            new UpdateMavenProjectJob(new IProject[]{project}).schedule();

        } catch (Exception e) {
            LambdaAnalytics.trackProjectCreationFailed();
            LambdaPlugin.getDefault().reportException("Failed to create new Lambda project", e);
        }

        LambdaAnalytics.trackProjectCreationSucceeded();

        IFile handlerClass = findHandlerClassFile(project, dataModel);
        WorkbenchUtils.selectAndReveal(handlerClass, workbench); // show in explorer
        WorkbenchUtils.openFileInEditor(handlerClass, workbench); // show in editor

        if (readmeFile != null) {
            try {
                WorkbenchUtils.openInternalBrowserAsEditor(readmeFile.toURI().toURL(), workbench);
            } catch (MalformedURLException e) {
                LambdaPlugin.getDefault().logWarning(
                        "Failed to open README.html for the new Lambda project", e);
            }
        }
        return Status.OK_STATUS;
    }

    // Use this basic Maven model to create a simple Maven project.
    private Model getModel() {
        Model model = new Model();
        String groupId = dataModel.getMavenConfigurationDataModel().getGroupId();
        String artifactId = dataModel.getMavenConfigurationDataModel().getArtifactId();
        model.setModelVersion(MavenFactory.getMavenModelVersion());
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(MavenFactory.getMavenModelVersion());
        return model;
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackProjectCreationCanceled();
        return true;
    }

    @Override
    protected void initDataModel() {
        MavenConfigurationDataModel mavenDataModel = dataModel.getMavenConfigurationDataModel();
        // Bind the package name property from the maven config model to Lambda config model to show the template code changes in the preview.
        mavenDataModel.addPropertyChangeListener((e) -> {
            dataModel.getLambdaFunctionDataModel().setPackageName(mavenDataModel.getPackageName());
        });
        mavenDataModel.setGroupId(DEFAULT_GROUP_ID);
        mavenDataModel.setArtifactId(DEFAULT_ARTIFACT_ID);
        mavenDataModel.setVersion(DEFAULT_VERSION);
        mavenDataModel.setPackageName(DEFAULT_PACKAGE_NAME);
        dataModel.setShowReadmeFile(LambdaPlugin.getDefault().getPreferenceStore()
              .getBoolean(LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT));
    }

    @Override
    protected String getJobTitle() {
        return "Creating AWS Lambda Maven Project";
    }

    private static IFile findHandlerClassFile(IProject project,
            LambdaFunctionWizardDataModel dataModel) {

        IPath handlerPath = new Path(MavenFactory.getMavenSourceFolder());
        JavaPackageName handlerPackage = JavaPackageName.parse(
                dataModel.getLambdaFunctionDataModel().getPackageName());
        for (String component : handlerPackage.getComponents()) {
            handlerPath = handlerPath.append(component);
        }
        handlerPath = handlerPath.append(dataModel.getLambdaFunctionDataModel().getClassName()
                + ".java");

        return project.getFile(handlerPath);
    }

    private static void savePreferences(
            LambdaFunctionWizardDataModel dataModel,
            IPreferenceStore prefStore) {
        prefStore.setValue(
                LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT,
                dataModel.isShowReadmeFile());
    }

    @Override
    protected AbstractAwsPlugin getPlugin() {
        return LambdaPlugin.getDefault();
    }
}
