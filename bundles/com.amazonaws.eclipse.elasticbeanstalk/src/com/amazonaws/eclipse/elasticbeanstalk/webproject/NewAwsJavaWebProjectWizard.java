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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkAnalytics;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;

/**
 * Wizard for creating a new WTP Dynamic Web project, pre-configured for use
 * with AWS (Java SDK configuration, security credentials, etc).
 */
public class NewAwsJavaWebProjectWizard extends Wizard implements INewWizard {
    private static final String DEFAULT_GROUP_ID = "com.amazonaws.beanstalk";
    private static final String DEFAULT_ARTIFACT_ID = "webproject";
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String DEFAULT_PACKAGE_NAME = MavenFactory.assumePackageName(DEFAULT_GROUP_ID, DEFAULT_ARTIFACT_ID);

    private NewAwsJavaWebProjectDataModel dataModel = new NewAwsJavaWebProjectDataModel();

    public NewAwsJavaWebProjectWizard() {
        this.addPage(new JavaWebProjectWizardPage(dataModel));

        setWindowTitle("New AWS Java Web Project");
        setDefaultPageImageDescriptor(AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        setNeedsProgressMonitor(true);
        initDataModel();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        trackCustomerMetrics();
        try {
            getContainer().run(true, false, new CreateNewAwsJavaWebProjectRunnable(dataModel));
            return true;
        } catch (InvocationTargetException e) {
            Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to create new AWS Java web project.", e.getCause());
            StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
        } catch (InterruptedException e) {}

        return false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {}

    public void initDataModel() {
        MavenConfigurationDataModel mavenConfig = dataModel.getMavenConfigurationDataModel();
        mavenConfig.setGroupId(DEFAULT_GROUP_ID);
        mavenConfig.setArtifactId(DEFAULT_ARTIFACT_ID);
        mavenConfig.setVersion(DEFAULT_VERSION);
        mavenConfig.setPackageName(DEFAULT_PACKAGE_NAME);
    }

    /*
     * Track user behavior from DataModel to the metrics system.
     */
    public void trackCustomerMetrics() {
        if (dataModel.getProjectTemplate() == JavaWebProjectTemplate.WORKER) {
            ElasticBeanstalkAnalytics.trackCreateNewWorkerApplication();
        } else if (dataModel.getProjectTemplate() == JavaWebProjectTemplate.DEFAULT) {
            if (dataModel.getUseDynamoDBSessionManagement()) {
                ElasticBeanstalkAnalytics.trackCreateNewWebApplication_DDB();
            } else {
                ElasticBeanstalkAnalytics.trackCreateNewWebApplication_NDDB();
            }
        }
    }

}
