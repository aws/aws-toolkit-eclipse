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
package com.amazonaws.eclipse.elasticbeanstalk.webproject;

import static com.amazonaws.eclipse.core.util.JavaProjectUtils.setDefaultJreToProjectClasspath;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.util.BundleUtils;
import com.amazonaws.eclipse.core.validator.JavaPackageName;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;

/**
 * Runnable (with progress) that creates a new AWS Java web project, based on
 * the configured data model. This class is responsible for creating the WTP
 * dynamic web project, adding and configuring the AWS SDK for Java, creating
 * the security credential configuration file and eventually configuring the WTP
 * runtime targeted by the new project.
 */
final class CreateNewAwsJavaWebProjectRunnable implements IRunnableWithProgress {

    private final NewAwsJavaWebProjectDataModel dataModel;

    private static final IWorkbenchBrowserSupport BROWSER_SUPPORT =
            PlatformUI.getWorkbench().getBrowserSupport();

    public CreateNewAwsJavaWebProjectRunnable(NewAwsJavaWebProjectDataModel dataModel) {
        this.dataModel = dataModel;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, "Creating new AWS Java web project", 100);

        try {
            IProject project = createBeanstalkProject(
                    dataModel.getMavenConfigurationDataModel(), monitor);
            IJavaProject javaProject = JavaCore.create(project);
            setDefaultJreToProjectClasspath(javaProject, monitor);
            monitor.worked(20);

            addTemplateFiles(project);
            monitor.worked(10);

            // Configure the Tomcat session manager
            if (dataModel.getUseDynamoDBSessionManagement()) {
                addSessionManagerConfigurationFiles(project);
            }
            monitor.worked(10);

            if (dataModel.getProjectTemplate() == JavaWebProjectTemplate.DEFAULT) {
                // Open the readme.html in an editor browser window.
                File root = project.getLocation().toFile();
                final File indexHtml = new File(root, "src/main/webapp/index.html");

                // Internal browser must be opened within UI thread
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IWebBrowser browser = BROWSER_SUPPORT.createBrowser(
                                    IWorkbenchBrowserSupport.AS_EDITOR,
                                    null,
                                    null,
                                    null);
                            browser.openURL(indexHtml.toURI().toURL());
                        } catch (Exception e) {
                            ElasticBeanstalkPlugin
                                    .getDefault()
                                    .logError(
                                            "Failed to open project index page in Eclipse editor.",
                                            e);
                        }
                    }
                });
            }

        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            progressMonitor.done();
        }
    }

    private void addSessionManagerConfigurationFiles(IProject project) throws IOException, CoreException {
        Bundle bundle = ElasticBeanstalkPlugin.getDefault().getBundle();
        URL url = FileLocator.resolve(bundle.getEntry("/"));
        IPath templateRoot = new Path(url.getFile(), "templates");

        FileUtils.copyDirectory(
                templateRoot.append("dynamodb-session-manager").toFile(),
                project.getLocation().toFile());

        // Add the user's credentials to context.xml
        File localContextXml = project.getLocation()
            .append(".ebextensions")
            .append("context.xml").toFile();
        AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(dataModel.getAccountId());
        String contextContents = FileUtils.readFileToString(localContextXml);
        contextContents = contextContents.replace("{ACCESS_KEY}", accountInfo.getAccessKey());
        contextContents = contextContents.replace("{SECRET_KEY}", accountInfo.getSecretKey());
        FileUtils.writeStringToFile(localContextXml, contextContents);

        project.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Update the J2EE Deployment Assembly by creating a link from the '/.ebextensions'
        // folder to the '/WEB-INF/.ebextensions' folder in the web assembly mapping for WTP
        IVirtualComponent rootComponent = ComponentCore.createComponent(project);
        IVirtualFolder rootFolder = rootComponent.getRootFolder();
        try {
            Path source = new Path("/.ebextensions");
            Path target = new Path("/WEB-INF/.ebextensions");
            IVirtualFolder subFolder = rootFolder.getFolder(target);
            subFolder.createLink(source, 0, null);
        } catch( CoreException ce ) {
            String message = "Unable to configure deployment assembly to map .ebextension directory";
            ElasticBeanstalkPlugin.getDefault().logError(message, ce);
        }
    }

    private IProject createBeanstalkProject(MavenConfigurationDataModel mavenConfig, IProgressMonitor monitor) throws CoreException, IOException {
        List<IProject> projects = MavenFactory.createArchetypeProject(
                "org.apache.maven.archetypes", "maven-archetype-webapp", "1.0",
                mavenConfig.getGroupId(), mavenConfig.getArtifactId(), mavenConfig.getVersion(), mavenConfig.getPackageName(), monitor);
        // This archetype only has one project
        return projects.get(0);
    }

    private void addTemplateFiles(IProject project) throws IOException, CoreException {

        final String CREDENTIAL_PROFILE_PLACEHOLDER = "{CREDENTIAL_PROFILE}";
        final String PACKAGE_NAME_PLACEHOLDER = "{PACKAGE_NAME}";

        Bundle bundle = ElasticBeanstalkPlugin.getDefault().getBundle();
        File templateRoot = null;
        try {
            templateRoot = BundleUtils.getFileFromBundle(bundle, "templates");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load templates from ElasticBeanstalk bundle.", e);
        }
        AccountInfo currentAccountInfo = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(dataModel.getAccountId());
        File pomFile = project.getFile("pom.xml").getLocation().toFile();
        MavenConfigurationDataModel mavenConfig = dataModel.getMavenConfigurationDataModel();

        switch (dataModel.getProjectTemplate()) {
        case WORKER:
            replacePomFile(new File(templateRoot, "worker/pom.xml"),
                    mavenConfig.getGroupId(), mavenConfig.getArtifactId(), mavenConfig.getVersion(), pomFile);
            String packageName = dataModel.getMavenConfigurationDataModel().getPackageName();

            JavaPackageName javaPackageName = JavaPackageName.parse(packageName);
            IPath location = project.getFile(MavenFactory.getMavenSourceFolder()).getLocation();
            for (String component : javaPackageName.getComponents()) {
                location = location.append(component);
            }

            FileUtils.copyDirectory(new File(templateRoot, "worker/src"),
                    location.toFile());

            File workerServlet = location.append("WorkerServlet.java").toFile();
            replaceStringInFile(workerServlet, CREDENTIAL_PROFILE_PLACEHOLDER, currentAccountInfo.getAccountName());
            replaceStringInFile(workerServlet, PACKAGE_NAME_PLACEHOLDER, packageName);
            File workerRequest = location.append("WorkRequest.java").toFile();
            replaceStringInFile(workerRequest, PACKAGE_NAME_PLACEHOLDER, packageName);

            location = project.getFile("src/main/webapp").getLocation();
            FileUtils.copyDirectory(
                new File(templateRoot, "worker/WebContent/"),
                location.toFile());
            File webXml = location.append("WEB-INF/web.xml").toFile();
            replaceStringInFile(webXml, PACKAGE_NAME_PLACEHOLDER, packageName);
            break;

        case DEFAULT:
            replacePomFile(new File(templateRoot, "basic/pom.xml"),
                    mavenConfig.getGroupId(), mavenConfig.getArtifactId(), mavenConfig.getVersion(), pomFile);

            location = project.getFile("src/main/webapp").getLocation();
            FileUtils.copyDirectory(
                new File(templateRoot, "basic/WebContent"),
                location.toFile());

            File indexJsp = location.append("index.jsp").toFile();
            replaceStringInFile(indexJsp, CREDENTIAL_PROFILE_PLACEHOLDER, currentAccountInfo.getAccountName());

            break;

        default:
            throw new IllegalStateException("Unknown project template: " +
                                            dataModel.getProjectTemplate());
        }

        project.refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    private String replacePomFile(File pomTemplate, String groupId, String artifactId, String version, File targetFile) throws IOException {
        final String GROUP_ID_PLACEHOLDER = "{GROUP_ID}";
        final String ARTIFACT_ID_PLACEHOLDER = "{ARTIFACT_ID}";
        final String VERSION_PLACEHOLDER = "{VERSION}";
        String content = FileUtils.readFileToString(pomTemplate);
        content = content.replace(GROUP_ID_PLACEHOLDER, groupId)
                .replace(ARTIFACT_ID_PLACEHOLDER, artifactId)
                .replace(VERSION_PLACEHOLDER, version);
        FileUtils.writeStringToFile(targetFile, content);
        return content;
    }

    /** Replace source strings with target string and return the original content of the file. */
    private String replaceStringInFile(File file, String source, String target) throws IOException {

        String originalContent = FileUtils.readFileToString(file);
        String replacedContent = originalContent.replace(source, target);
        FileUtils.writeStringToFile(file, replacedContent);

        return originalContent;
    }

}
