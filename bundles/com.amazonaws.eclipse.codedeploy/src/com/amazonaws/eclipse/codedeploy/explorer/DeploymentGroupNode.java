package com.amazonaws.eclipse.codedeploy.explorer;

import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;

public class DeploymentGroupNode {

    private final DeploymentGroupInfo deploymentGroup;
    private final DeploymentInfo mostRecentDeployment;

    public DeploymentGroupNode(DeploymentGroupInfo deploymentGroup,
            DeploymentInfo mostRecentDeployment) {
        this.deploymentGroup = deploymentGroup;
        this.mostRecentDeployment = mostRecentDeployment;
    }

    public DeploymentGroupInfo getDeploymentGroup() {
        return deploymentGroup;
    }

    public DeploymentInfo getMostRecentDeployment() {
        return mostRecentDeployment;
    }
}
