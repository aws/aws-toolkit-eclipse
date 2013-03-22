/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.android.sdk.newproject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.android.sdk.AndroidSdkInstall;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager;
import com.amazonaws.eclipse.android.sdk.classpath.AwsAndroidSdkClasspathUtils;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreator;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizardState;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectWizardState.Mode;

public class NewAndroidProjectWizard extends Wizard implements INewWizard {

    private IRunnableContext runnableContext;

    private NewAndroidProjectDataModel dataModel = new NewAndroidProjectDataModel();


    public NewAndroidProjectWizard() {
        setWindowTitle("New AWS Android Project");
        setDefaultPageImageDescriptor(
                AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        setNeedsProgressMonitor(true);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // Make sure an Android Platform is installed
        Sdk sdk = Sdk.getCurrent();
        if (sdk == null || sdk.getTargets().length == 0) {

            // TODO: Is this causing an initial UI bug in the wizard?

            throw new RuntimeException("Unable to find an Android SDK installed.  " +
                "Use the Android SDK manager to install an Android SDK before you " +
                "create an AWS Android project.");
        }
    }

    @Override
    @SuppressWarnings("restriction")
    public boolean performFinish() {
        if (getContainer() instanceof WizardDialog) {
            setRunnableContext((WizardDialog)getContainer());
        }

        try {
            NewProjectWizardState newProjectWizardState = new NewProjectWizardState(Mode.ANY);
            newProjectWizardState.projectName = dataModel.getProjectName();
            newProjectWizardState.applicationName = "AWS Android Application";
            newProjectWizardState.packageName = dataModel.getPackageName();
            newProjectWizardState.target = dataModel.getAndroidTarget();
            newProjectWizardState.createActivity = false;

            new NewProjectCreator(newProjectWizardState, runnableContext).createAndroidProjects();
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(dataModel.getProjectName());

            IJavaProject javaProject = JavaCore.create(project);
            AndroidSdkInstall awsAndroidSdk = AndroidSdkManager.getInstance().getDefaultSdkInstall();
            awsAndroidSdk.writeMetadataToProject(javaProject);
            AwsAndroidSdkClasspathUtils.addAwsAndroidSdkToProjectClasspath(javaProject, awsAndroidSdk);

            AndroidManifestFile androidManifestFile = new AndroidManifestFile(project);
            androidManifestFile.initialize();
            copyProguardPropertiesFile(project);

            if (dataModel.isSampleCodeIncluded()) {
                // copy sample code files over
                Bundle bundle = Platform.getBundle(AndroidSDKPlugin.PLUGIN_ID);
                URL url = FileLocator.find(bundle, new Path("resources/S3_Uploader/"), null);
                try {
                    File sourceFile = new File(FileLocator.resolve(url).toURI());
                    File projectFolder = project.getLocation().toFile();
                    File projectSourceFolder = new File(projectFolder, "src");

                    for (File file : sourceFile.listFiles()) {
                        File destinationFile = new File(project.getLocation().toFile(), file.getName());
                        if (file.isDirectory()) FileUtils.copyDirectory(file, destinationFile);
                        else FileUtils.copyFile(file, destinationFile);
                    }

                    // move *.java files to new src dir
                    String s = dataModel.getPackageName().replace(".", File.separator) + File.separator;
                    for (File file : projectSourceFolder.listFiles()) {
                        if (file.isDirectory()) continue;
                        File destinationFile = new File(projectSourceFolder, s + file.getName());
                        FileUtils.moveFile(file, destinationFile);

                        // update package lines with regex
                        // replace "com.amazonaws.demo.s3uploader" with dataModel.getPackageName()
                        List<String> lines = FileUtils.readLines(destinationFile);
                        ArrayList<String> outLines = new ArrayList<String>();
                        for (String line : lines) {
                            outLines.add(line.replace("com.amazonaws.demo.s3uploader", dataModel.getPackageName()));
                        }
                        FileUtils.writeLines(destinationFile, outLines);
                    }

                    // update android manifest file
                    androidManifestFile.addSampleActivity();
                } catch (Exception e) {
                    IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Unable to update AWS SDK with sample app for Android project setup", e);
                    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
                }
            }

            // refresh the workspace to pick up the changes we just made
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Unable to create new AWS Android project", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            return false;
        }

        return true;
    }

    @Override
    public void addPages() {
        addPage(new AndroidProjectWizardPage(dataModel));
    }

    public void setRunnableContext(IRunnableContext runnableContext) {
        this.runnableContext = runnableContext;
    }

    /**
     * Copies the Proguard file from the resources directory to the base of the
     * specified project.
     */
    private void copyProguardPropertiesFile(IProject project) {
        Bundle bundle = Platform.getBundle(AndroidSDKPlugin.PLUGIN_ID);
        URL url = FileLocator.find(bundle, new Path("resources/proguard-project.txt"), null);
        try {
            File sourceFile = new File(FileLocator.resolve(url).toURI());
            FileUtils.copyFile(sourceFile, new File(project.getLocation().toFile(), "proguard-project.txt"));
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Unable to copy AWS SDK for Android Progaurd configuration file", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
}
