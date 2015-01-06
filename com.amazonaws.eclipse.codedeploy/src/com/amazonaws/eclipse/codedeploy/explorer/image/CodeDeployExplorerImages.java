package com.amazonaws.eclipse.codedeploy.explorer.image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;

public class CodeDeployExplorerImages {

    public static final String IMG_AWS_BOX = "aws-box";
    public static final String IMG_SERVICE = "codedeploy-service";
    public static final String IMG_APPLICATION = "application";
    public static final String IMG_DEPLOYMENT_GROUP = "deploymentgroup";
    public static final String IMG_CHECK_ICON = "check-icon";

    public static ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = new ImageRegistry(Display.getCurrent());

        imageRegistry.put(IMG_AWS_BOX, ImageDescriptor.createFromFile(CodeDeployPlugin.class, "/icons/aws-box.gif"));
        imageRegistry.put(IMG_SERVICE, ImageDescriptor.createFromFile(CodeDeployPlugin.class, "/icons/codedeploy-service.png"));
        imageRegistry.put(IMG_APPLICATION, ImageDescriptor.createFromFile(CodeDeployPlugin.class, "/icons/application.png"));
        imageRegistry.put(IMG_DEPLOYMENT_GROUP, ImageDescriptor.createFromFile(CodeDeployPlugin.class, "/icons/deployment-group.png"));
        imageRegistry.put(IMG_CHECK_ICON, ImageDescriptor.createFromFile(CodeDeployPlugin.class, "/icons/12px-check-icon.png"));

        return imageRegistry;
    }
}
