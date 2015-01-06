package com.amazonaws.eclipse.opsworks.explorer;

import java.util.List;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.explorer.image.OpsWorksExplorerImages;
import com.amazonaws.eclipse.opsworks.explorer.node.AppsRootNode;
import com.amazonaws.eclipse.opsworks.explorer.node.LayerElementNode;
import com.amazonaws.eclipse.opsworks.explorer.node.LayersRootNode;
import com.amazonaws.eclipse.opsworks.explorer.node.OpsWorksRootNode;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Instance;
import com.amazonaws.services.opsworks.model.Stack;

public class OpsWorksLabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public Image getDefaultImage(Object element) {
        ImageRegistry imageRegistry = OpsWorksPlugin.getDefault().getImageRegistry();
        if ( element instanceof OpsWorksRootNode ) {
            return imageRegistry.get(OpsWorksExplorerImages.IMG_SERVICE);
        }

        if ( element instanceof Stack ) {
            return imageRegistry.get(OpsWorksExplorerImages.IMG_STACK);
        }

        if ( element instanceof LayerElementNode || element instanceof LayersRootNode ) {
            return imageRegistry.get(OpsWorksExplorerImages.IMG_LAYER);
        }

        if ( element instanceof Instance ) {
            return imageRegistry.get(OpsWorksExplorerImages.IMG_INSTANCE);
        }

        if ( element instanceof App || element instanceof AppsRootNode ) {
            return imageRegistry.get(OpsWorksExplorerImages.IMG_APP);
        }

        return null;
    }

    @Override
    public String getText(Object element) {
        if ( element instanceof OpsWorksRootNode ) {
            return "AWS OpsWorks";
        }

        if ( element instanceof Stack ) {
            Stack stack = (Stack)element;
            return stack.getName();
        }

        if ( element instanceof LayersRootNode ) {
            LayersRootNode layerRoot = (LayersRootNode)element;
            List<LayerElementNode> layers = layerRoot.getLayerNodes();
            int count = layers == null ? 0 : layers.size();
            if (count > 1) {
                return count + " Layers";
            } else {
                return count + " Layer";
            }
        }

        if ( element instanceof LayerElementNode ) {
            LayerElementNode layerNode = (LayerElementNode)element;
            return layerNode.getLayer().getName();
        }

        if ( element instanceof Instance ) {
            Instance instance = (Instance)element;
            return instance.getHostname();
        }

        if ( element instanceof AppsRootNode ) {
            AppsRootNode appRoot = (AppsRootNode)element;
            List<App> apps = appRoot.getApps();
            int count = apps == null ? 0 : apps.size();
            if (count > 1) {
                return count + " Apps";
            } else {
                return count + " App";
            }
        }

        if ( element instanceof App ) {
            App app = (App)element;
            return app.getName();
        }

        return getExplorerNodeText(element);
    }

}
