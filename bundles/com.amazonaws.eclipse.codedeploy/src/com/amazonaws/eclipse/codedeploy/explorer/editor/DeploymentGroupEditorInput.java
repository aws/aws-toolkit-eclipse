/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.codedeploy.explorer.editor;

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.codedeploy.explorer.image.CodeDeployExplorerImages;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;

public final class DeploymentGroupEditorInput extends AbstractAwsResourceEditorInput {

    private final String applicationName;
    private final String deploymentGroupName;

    public DeploymentGroupEditorInput(String applicationName,
            String deploymentGroupName, String endpoint, String accountId) {
        super(endpoint, accountId);

        this.applicationName = applicationName;
        this.deploymentGroupName = deploymentGroupName;
    }

    @Override
    public String getToolTipText() {
        return "Amazon CodeDeploy deployment group - " + getName();
    }

    @Override
    public String getName() {
        return String.format("%s [%s]", applicationName, deploymentGroupName);
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor(CodeDeployExplorerImages.IMG_DEPLOYMENT_GROUP);
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getDeploymentGroupName() {
        return deploymentGroupName;
    }

    public AmazonCodeDeploy getCodeDeployClient() {
        return AwsToolkitCore.getClientFactory().getCodeDeployClientByEndpoint(
                getRegionEndpoint());
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof DeploymentGroupEditorInput) )
            return false;

        DeploymentGroupEditorInput otherEditor = (DeploymentGroupEditorInput) obj;
        return otherEditor.getApplicationName().equals(this.getApplicationName()) &&
               otherEditor.getDeploymentGroupName().equals(this.getDeploymentGroupName());
    }
}
