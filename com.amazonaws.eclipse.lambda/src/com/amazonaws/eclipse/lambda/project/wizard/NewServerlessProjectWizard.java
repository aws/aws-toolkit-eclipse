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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.DependencyManagement;
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

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.plugin.AbstractAwsProjectWizard;
import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.core.validator.JavaPackageName;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewServerlessProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.serverless.NameUtils;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class NewServerlessProjectWizard extends AbstractAwsProjectWizard {

    private static final String DEFAULT_GROUP_ID = "com.serverless";
    private static final String DEFAULT_ARTIFACT_ID = "demo";
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
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to create AWS Serverless Maven Project.", e);
        }

        try {
            dataModel.buildServerlessModel();
            FunctionProjectUtil.addSourceToProject(project, dataModel);
            readmeFile = FunctionProjectUtil.addServerlessReadmeFileToProject(project, dataModel);
            FunctionProjectUtil.refreshProject(project);

        } catch (Exception e) {
            LambdaAnalytics.trackServerlessProjectCreationFailed();
            LambdaPlugin.getDefault().reportException("Failed to create new Serverless project", e);
        }

        LambdaAnalytics.trackServerlessProjectCreationSucceeded();

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
        model.addDependency(MavenFactory.getAwsLambdaJavaCoreDependency());
        model.addDependency(MavenFactory.getAwsLambdaJavaEventsDependency());
        model.addDependency(MavenFactory.getJunitDependency());
        model.addDependency(MavenFactory.getLatestAwsSdkDependency("compile"));
        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.addDependency(MavenFactory.getLatestAwsBomDependency());
        model.setDependencyManagement(dependencyManagement);
        return model;
    }

    private static IFile findHandlerClassFile(IProject project,
            NewServerlessProjectDataModel dataModel)
                    throws JsonParseException, JsonMappingException, IOException {

        IPath handlerPath = new Path("");
        List<ServerlessHandlerTemplateData> templates = dataModel.getServerlessHandlerTemplateData();
        if (templates == null || templates.isEmpty()) {
            handlerPath = handlerPath.append(NameUtils.SERVERLESS_TEMPLATE_FILE_NAME);
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
        dataModel.setPackagePrefix(DEFAULT_PACKAGE_NAME);
    }

    @Override
    protected AbstractAwsPlugin getPlugin() {
        return LambdaPlugin.getDefault();
    }
}
