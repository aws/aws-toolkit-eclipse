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
package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.core.util.JavaProjectUtils.setDefaultJreToProjectClasspath;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;

import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.plugin.AbstractAwsProjectWizard;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.core.validator.JavaPackageName;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;
import com.amazonaws.eclipse.lambda.project.metadata.ServerlessProjectMetadata;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewServerlessProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.TemplateException;

public class NewServerlessProjectWizard extends AbstractAwsProjectWizard {

    private static final String DEFAULT_GROUP_ID = "com.serverless";
    private static final String DEFAULT_ARTIFACT_ID = "demo";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_PACKAGE_NAME = MavenFactory.assumePackageName(DEFAULT_GROUP_ID, DEFAULT_ARTIFACT_ID);

    private final NewServerlessProjectDataModel dataModel = new NewServerlessProjectDataModel();
    private NewServerlessProjectWizardPageOne pageOne;

    private IProject project;

    @Override
    public void addPages() {
        if (pageOne == null) {
            pageOne = new NewServerlessProjectWizardPageOne(dataModel);
        }
        addPage(pageOne);
    }

    public NewServerlessProjectWizard() {
        super("New AWS Serverless Maven Project");
        initDataModel();
    }

    @Override
    protected String getJobTitle() {
        return "Creating AWS Serverless Maven Project";
    }

    @Override
    protected IStatus doFinish(IProgressMonitor monitor) {
        LambdaAnalytics.trackServerlessProjectSelection(dataModel);
        final String projectName = dataModel.getProjectNameDataModel().getProjectName();
        final Model mavenModel = getModel();

        File readmeFile = null;

        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject(projectName);

        try {
            MavenFactory.createMavenProject(project, mavenModel, monitor);
            IJavaProject javaProject = JavaCore.create(project);
            setDefaultJreToProjectClasspath(javaProject, monitor);
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to create AWS Serverless Maven Project.",
                    new AwsActionException(AwsToolkitMetricType.LAMBDA_NEW_SERVERLESS_PROJECT_WIZARD.getName(), e.getMessage(), e));
        }

        try {
            FunctionProjectUtil.createServerlessBlueprintProject(project, dataModel);
            readmeFile = FunctionProjectUtil.emitServerlessReadme(project, dataModel);
            FunctionProjectUtil.refreshProject(project);
            new UpdateMavenProjectJob(new IProject[]{project}).schedule();

        } catch (Exception e) {
            LambdaAnalytics.trackServerlessProjectCreationFailed();
            LambdaPlugin.getDefault().reportException("Failed to create new Serverless project",
                    new AwsActionException(AwsToolkitMetricType.LAMBDA_NEW_SERVERLESS_PROJECT_WIZARD.getName(), e.getMessage(), e));
        }

        LambdaAnalytics.trackServerlessProjectCreationSucceeded();
        saveMetadata();

        try {
            IFile handlerClass = findHandlerClassFile(project, dataModel);
            WorkbenchUtils.selectAndReveal(handlerClass, workbench); // show in explorer
            WorkbenchUtils.openFileInEditor(handlerClass, workbench); // show in editor
            if (readmeFile != null) {
                WorkbenchUtils.openInternalBrowserAsEditor(readmeFile.toURI().toURL(), workbench);
            }
        } catch (Exception e) {
            LambdaPlugin.getDefault().logWarning(
                    "Failed to open the start up file.", e);
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackServerlessProjectCreationCanceled();
        return true;
    }

    private Model getModel() {
        Model model = new Model();
        model.setModelVersion(MavenFactory.getMavenModelVersion());
        model.setGroupId(dataModel.getMavenConfigurationDataModel().getGroupId());
        model.setArtifactId(dataModel.getMavenConfigurationDataModel().getArtifactId());
        model.setVersion(MavenFactory.getMavenModelVersion());
        return model;
    }

    private static IFile findHandlerClassFile(IProject project,
            NewServerlessProjectDataModel dataModel)
                    throws JsonParseException, JsonMappingException, IOException, TemplateException {

        IPath handlerPath = new Path("");
        List<ServerlessHandlerTemplateData> templates = dataModel.getServerlessHandlerTemplateData();
        if (templates == null || templates.isEmpty()) {
            handlerPath = handlerPath.append(CodeTemplateManager.SAM_FILE_NAME);
        } else {
            ServerlessHandlerTemplateData template = templates.get(0);
            handlerPath = handlerPath.append(MavenFactory.getMavenSourceFolder());
            JavaPackageName handlerPackage = JavaPackageName.parse(template.getPackageName());
            for (String component : handlerPackage.getComponents()) {
                handlerPath = handlerPath.append(component);
            }
            handlerPath = handlerPath.append(template.getClassName() + ".java");
        }
        return project.getFile(handlerPath);
    }

    @Override
    protected void initDataModel() {
        dataModel.getMavenConfigurationDataModel().setGroupId(DEFAULT_GROUP_ID);
        dataModel.getMavenConfigurationDataModel().setArtifactId(DEFAULT_ARTIFACT_ID);
        dataModel.getMavenConfigurationDataModel().setVersion(DEFAULT_VERSION);
        dataModel.getMavenConfigurationDataModel().setPackageName(DEFAULT_PACKAGE_NAME);
    }

    private void saveMetadata() {
        ServerlessProjectMetadata metadata = new ServerlessProjectMetadata();
        metadata.setPackagePrefix(dataModel.getMavenConfigurationDataModel().getPackageName());
        try {
            ProjectMetadataManager.saveServerlessProjectMetadata(project, metadata);
        } catch (IOException e) {
            LambdaPlugin.getDefault().logError(e.getMessage(), e);
        }
    }

    @Override
    protected AbstractAwsPlugin getPlugin() {
        return LambdaPlugin.getDefault();
    }
}
