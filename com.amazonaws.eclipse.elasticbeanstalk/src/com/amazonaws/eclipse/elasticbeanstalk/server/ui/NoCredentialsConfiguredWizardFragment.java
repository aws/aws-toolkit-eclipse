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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.NoCredentialsDialog;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;

public class NoCredentialsConfiguredWizardFragment extends AbstractDeployWizardPage {

    protected NoCredentialsConfiguredWizardFragment(DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
        setComplete(false);
    }

    @Override
    public String getPageTitle() {
        return "No AWS security credentials configured";
    }

    @Override
    public String getPageDescription() {
        return "No AWS security credentials configured";
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        wizardHandle = handle;

        handle.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        return NoCredentialsDialog.createComposite(parent);
    }
}
