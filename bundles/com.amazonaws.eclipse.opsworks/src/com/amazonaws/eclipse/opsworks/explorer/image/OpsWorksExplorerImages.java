package com.amazonaws.eclipse.opsworks.explorer.image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;

public class OpsWorksExplorerImages {

    public static final String IMG_AWS_BOX = "aws-box";
    public static final String IMG_SERVICE = "opsworks-service";
    public static final String IMG_STACK = "stack";
    public static final String IMG_LAYER = "layer";
    public static final String IMG_INSTANCE = "instance";
    public static final String IMG_APP = "app";
    public static final String IMG_ADD = "add";


    public static ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = new ImageRegistry(Display.getCurrent());

        imageRegistry.put(IMG_AWS_BOX, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/aws-box.gif"));
        imageRegistry.put(IMG_SERVICE, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/opsworks-service.png"));
        imageRegistry.put(IMG_STACK, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/stack.png"));
        imageRegistry.put(IMG_LAYER, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/layer.png"));
        imageRegistry.put(IMG_INSTANCE, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/instance.png"));
        imageRegistry.put(IMG_APP, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/app.png"));
        imageRegistry.put(IMG_ADD, ImageDescriptor.createFromFile(OpsWorksPlugin.class, "/icons/add.png"));

        return imageRegistry;
    }
}
