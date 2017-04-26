package com.amazonaws.eclipse.codedeploy.explorer;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.explorer.image.CodeDeployExplorerImages;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.services.codedeploy.model.ApplicationInfo;

public class CodeDeployLabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public Image getDefaultImage(Object element) {
        ImageRegistry imageRegistry = CodeDeployPlugin.getDefault().getImageRegistry();
        if ( element instanceof CodeDeployRootElement ) {
            return imageRegistry.get(CodeDeployExplorerImages.IMG_SERVICE);
        }

        if ( element instanceof ApplicationInfo ) {
            return imageRegistry.get(CodeDeployExplorerImages.IMG_APPLICATION);
        }

        if ( element instanceof DeploymentGroupNode ) {
            return imageRegistry.get(CodeDeployExplorerImages.IMG_DEPLOYMENT_GROUP);
        }

        return null;
    }

    @Override
    public String getText(Object element) {
        if ( element instanceof CodeDeployRootElement ) {
            return "AWS CodeDeploy";
        }

        if ( element instanceof ApplicationInfo ) {
            ApplicationInfo app = (ApplicationInfo)element;
            return app.getApplicationName();
        }

        if ( element instanceof DeploymentGroupNode ) {
            DeploymentGroupNode deployGroupNode = (DeploymentGroupNode)element;
            return deployGroupNode.getDeploymentGroup().getDeploymentGroupName();
        }

        return getExplorerNodeText(element);
    }

}
