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
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.wizard.AwsJavaSdkNotInstalledWizardPage;

/**
 * Wizard for creating a new WTP Dynamic Web project, pre-configured for use
 * with AWS (Java SDK configuration, security credentials, etc).
 */
public class NewAwsJavaWebProjectWizard extends Wizard implements INewWizard {

    private NewAwsJavaWebProjectDataModel dataModel = new NewAwsJavaWebProjectDataModel();

    private boolean awsSdkInstalled = true;

    public NewAwsJavaWebProjectWizard() {
        // Check if the SDK is installed
        JavaSdkManager sdk = JavaSdkManager.getInstance();
        if (sdk.getDefaultSdkInstall() == null
                && sdk.getInstallationJob() == null) {
            awsSdkInstalled = false;
            addPage(new AwsJavaSdkNotInstalledWizardPage());

        } else {
            this.addPage(new JavaWebProjectWizardPage(dataModel));

            setWindowTitle("New AWS Java Web Project");
            setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(
                    AwsToolkitCore.IMAGE_AWS_LOGO));
            setNeedsProgressMonitor(true);
        }

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        if ( !awsSdkInstalled ) {
            JavaSdkManager.getInstance().initializeSDKInstalls();
            return true;
        }

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

    public void init(IWorkbench workbench, IStructuredSelection selection) {}

}
