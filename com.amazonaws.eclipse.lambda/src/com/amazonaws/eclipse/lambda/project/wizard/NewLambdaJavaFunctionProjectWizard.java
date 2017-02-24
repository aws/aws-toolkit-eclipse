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
import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.plugin.AbstractAwsProjectWizard;
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
            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException(
                        "Failed to create AWS Lambda Maven Project.", e);
            }

            FunctionProjectUtil.addSourceToProject(project, dataModel.getLambdaFunctionDataModel());

            if (dataModel.isShowReadmeFile()) {
                readmeFile = FunctionProjectUtil.addReadmeFileToProject(project,
                        dataModel.getLambdaFunctionDataModel().collectHandlerTestTemplateData());
            }

            FunctionProjectUtil.refreshProject(project);

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

    private Model getModel() {
        Model model = new Model();
        String groupId = dataModel.getMavenConfigurationDataModel().getGroupId();
        String artifactId = dataModel.getMavenConfigurationDataModel().getArtifactId();
        model.setModelVersion(MavenFactory.getMavenModelVersion());
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(MavenFactory.getMavenModelVersion());
        model.addDependency(MavenFactory.getAwsLambdaJavaCoreDependency());
        model.addDependency(MavenFactory.getAwsLambdaJavaEventsDependency());
        model.addDependency(MavenFactory.getJunitDependency());
        model.addDependency(MavenFactory.getLatestAwsSdkDependency("compile"));
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
        mavenDataModel.setGroupId(DEFAULT_GROUP_ID);
        mavenDataModel.setArtifactId(DEFAULT_ARTIFACT_ID);
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
