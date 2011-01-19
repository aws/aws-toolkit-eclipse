/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.jst.j2ee.classpathdep.UpdateClasspathAttributeUtil;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties.FacetDataModelMap;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkInstall;
import com.amazonaws.eclipse.sdk.ui.SdkManager;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsSdkClasspathUtils;

/**
 * Runnable (with progress) that creates a new AWS Java web project, based on
 * the configured data model. This class is responsible for creating the WTP
 * dynamic web project, adding and configuring the AWS SDK for Java, creating
 * the security credential configuration file and eventually configuring the WTP
 * runtime targeted by the new project.
 */
final class CreateNewAwsJavaWebProjectRunnable implements IRunnableWithProgress {

    private static final String ELASTIC_BEANSTALK_RUNTIME_ID = "com.amazonaws.eclipse.elasticbeanstalk.jee.runtime";
    private static final String GENERIC_JEE_RUNTIME_ID = "org.eclipse.jst.server.core.runtimeType";

    private final NewAwsJavaWebProjectDataModel dataModel;

    public CreateNewAwsJavaWebProjectRunnable(NewAwsJavaWebProjectDataModel dataModel) {
        this.dataModel = dataModel;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, "Creating new AWS Java web project", 100);

        try {
            IRuntime genericJeeServerRuntime = configureGenericJeeServerRuntime();

            // Create a WTP Dynamic Web project
            IDataModel newWebProjectDataModel = DataModelFactory.createDataModel(IWebFacetInstallDataModelProperties.class);
            newWebProjectDataModel.setProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME, dataModel.getProjectName());
            if (genericJeeServerRuntime != null) {
                newWebProjectDataModel.setProperty(
                        IFacetProjectCreationDataModelProperties.FACET_RUNTIME,
                        RuntimeManager.getRuntime(genericJeeServerRuntime.getId()));
            }

            // Default to a 2.5 web app
            FacetDataModelMap facetDataModelMap = (FacetDataModelMap)newWebProjectDataModel.getProperty(IFacetProjectCreationDataModelProperties.FACET_DM_MAP);
            IDataModel facetDataModel = facetDataModelMap.getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB);
            facetDataModel.setProperty(IFacetDataModelProperties.FACET_VERSION, IJ2EEFacetConstants.DYNAMIC_WEB_25);

            newWebProjectDataModel.getDefaultOperation().execute(monitor.newChild(30), null);

            // Add the AWS SDK for Java
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(dataModel.getProjectName());
            IJavaProject javaProject = JavaCore.create(project);
            SdkInstall sdkInstall = SdkManager.getInstance().getDefaultSdkInstall();
            if (sdkInstall.getRootDirectory().toString().contains("/plugins/")) {
                System.out.println("using plugin version");
            }
            sdkInstall.writeMetadataToProject(javaProject);
            AwsSdkClasspathUtils.addAwsSdkToProjectClasspath(javaProject, sdkInstall);
            monitor.worked(20);

            // Mark it as a Java EE module dependency
            // TODO: If the user changes the SDK version (through the properties page) then we'll lose the
            //       Java EE module dependency classpath entry attribute.
            Map<IClasspathEntry, IPath> classpathEntriesToRuntimePath = new HashMap<IClasspathEntry, IPath>();
            IClasspathEntry entry = findSdkClasspathEntry(javaProject);
            final IPath runtimePath = ClasspathDependencyUtil.getRuntimePath(null, true, ClasspathDependencyUtil.isClassFolderEntry(entry));
            classpathEntriesToRuntimePath.put(entry, runtimePath);
            IDataModelOperation addDependencyAttributesOperation = UpdateClasspathAttributeUtil.createAddDependencyAttributesOperation(project.getName(), classpathEntriesToRuntimePath);
            addDependencyAttributesOperation.execute(monitor.newChild(30), null);

            // Add files to the the project
            addTemplateFiles(project);
            monitor.worked(10);
            CredentialsUtils credentialsUtils = new CredentialsUtils();
            credentialsUtils.addAwsCredentialsFileToProject(project, dataModel.getAccessKeyId(), dataModel.getSecretAccessKey());
            credentialsUtils.saveCredentials(dataModel.getAccessKeyId(), dataModel.getSecretAccessKey());
            monitor.worked(10);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            progressMonitor.done();
        }
    }

    private IClasspathEntry findSdkClasspathEntry(IJavaProject javaProject) throws JavaModelException {
        IPath expectedPath = new AwsClasspathContainer(SdkManager.getInstance().getDefaultSdkInstall()).getPath();
        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
            if (entry.getPath().equals(expectedPath)) return entry;
        }

        return null;
    }

    private IRuntime configureGenericJeeServerRuntime() {
        // Return the existing AWS generic J2EE runtime if it already exists
        IRuntime runtime = ServerCore.findRuntime(ELASTIC_BEANSTALK_RUNTIME_ID);
        if (runtime != null) return runtime;

        // Otherwise try to create a new one...
        try {
            IRuntimeType jeeRuntimeType = ServerCore.findRuntimeType(GENERIC_JEE_RUNTIME_ID);
            IRuntimeWorkingCopy workingCopy = jeeRuntimeType.createRuntime(ELASTIC_BEANSTALK_RUNTIME_ID, new NullProgressMonitor());
            workingCopy.setName("AWS Elastic Beanstalk J2EE Runtime");

            Bundle bundle = ElasticBeanstalkPlugin.getDefault().getBundle();
            URL url = FileLocator.resolve(bundle.getEntry("/"));

            try {
                File source = new File(url.getFile(), "runtime-lib/j2ee.jar");
                File dest = new File(ElasticBeanstalkPlugin.getDefault().getStateLocation().toFile(), "runtime-lib/j2ee.jar");
                if ( !dest.exists() )
                    FileUtils.copyFile(source, dest);
                workingCopy.setLocation(new Path(dest.getParentFile().getAbsolutePath()));
            } catch ( Exception e ) {
                // If we can't copy the j2ee jar into the workspace, fall back
                // to using the file in the plugin.
                workingCopy.setLocation(new Path(url.getFile(), "runtime-lib"));
            }

            return workingCopy.save(true, new NullProgressMonitor());
        } catch ( Exception e ) {
            ElasticBeanstalkPlugin.getDefault().getLog()
                    .log(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage(), e));
            return null;
        }
    }

    private static class CredentialsUtils {
        private static final String AWS_CREDENTIALS_URL = "http://aws.amazon.com/security-credentials";
        private static final String AWS_CREDENTIALS_PROPERTIES_FILE = "AwsCredentials.properties";

        public void saveCredentials(String accessKeyId, String secretKey) {
            // bail out if we don't have any data to persist
            if (accessKeyId == null || accessKeyId.trim().length() == 0) return;
            if (secretKey == null || secretKey.trim().length() == 0) return;

            AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
            accountInfo.setAccessKey(accessKeyId);
            accountInfo.setSecretKey(secretKey);
        }

        public void addAwsCredentialsFileToProject(final IProject project, String accessKeyId, String secretKey) throws CoreException {
            Properties credentialProperties = new Properties();
            credentialProperties.setProperty("accessKey", accessKeyId);
            credentialProperties.setProperty("secretKey", secretKey);

            IPath srcDirPath = project.getLocation().append("src");
            final IPath credentialsFilePath = srcDirPath.append(AWS_CREDENTIALS_PROPERTIES_FILE);

            IFileStore credentialPropertiesFile =
                EFS.getLocalFileSystem().fromLocalFile(credentialsFilePath.toFile());
            OutputStream os = credentialPropertiesFile.openOutputStream(EFS.NONE, null);

            try {
                credentialProperties.store(os, "Insert your AWS Credentials from " + AWS_CREDENTIALS_URL);
            } catch (IOException e) {
                Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Unable to write AWS credentials to file", e);
                throw new CoreException(status);
            } finally {
                try {os.close();} catch (Exception e) {}
            }

            project.refreshLocal(IResource.DEPTH_INFINITE, null);

            final IFile[] files = project.getWorkspace().getRoot().findFilesForLocationURI(credentialsFilePath.toFile().toURI());
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    for (IFile file : files) {
//                        BasicNewResourceWizard.selectAndReveal(
//                                file, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                }
            });
        }
    }

    private void addTemplateFiles(IProject project) throws IOException, CoreException {
        Bundle bundle = ElasticBeanstalkPlugin.getDefault().getBundle();
        URL url = FileLocator.resolve(bundle.getEntry("/"));

        IPath templateRoot = null;
        if (dataModel.isSampleAppIncluded()) {
            templateRoot = new Path(url.getFile(), "templates").append("TravelLog");
        } else {
            templateRoot = new Path(url.getFile(), "templates").append("basic");
        }

        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.toString().contains("/.svn/") == false);
            }
        };
        FileUtils.copyDirectory(templateRoot.toFile(), project.getLocation().toFile(), filter);

        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        final IResource resourceToReveal = project.findMember(new Path("WebContent/index.jsp"));
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
//                BasicNewResourceWizard.selectAndReveal(
//                        resourceToReveal, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            }
        });
    }
}
