package com.amazonaws.eclipse.opsworks.explorer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.eclipse.opsworks.ServiceAPIUtils;
import com.amazonaws.eclipse.opsworks.explorer.node.AppsRootNode;
import com.amazonaws.eclipse.opsworks.explorer.node.LayerElementNode;
import com.amazonaws.eclipse.opsworks.explorer.node.LayersRootNode;
import com.amazonaws.eclipse.opsworks.explorer.node.OpsWorksRootNode;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Instance;
import com.amazonaws.services.opsworks.model.Layer;
import com.amazonaws.services.opsworks.model.Stack;

public class OpsWorksContentProvider extends AbstractContentProvider {

    private static OpsWorksContentProvider instance;

    public OpsWorksContentProvider() {
        instance = this;
    }

    /*
     * Abstract methods of AbstractContentProvider
     */

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof AWSResourcesRootElement ||
            element instanceof OpsWorksRootNode ||
            element instanceof Stack) {
            return true;
        }

        if (element instanceof LayersRootNode) {
            LayersRootNode node = (LayersRootNode)element;
            return node.getLayerNodes() != null && !node.getLayerNodes().isEmpty();
        }

        if (element instanceof LayerElementNode) {
            LayerElementNode node = (LayerElementNode)element;
            return node.getInstancesInLayer() != null && !node.getInstancesInLayer().isEmpty();
        }

        if (element instanceof AppsRootNode) {
            AppsRootNode node = (AppsRootNode)element;
            return node.getApps() != null && !node.getApps().isEmpty();
        }

        return false;
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { OpsWorksRootNode.ROOT_ELEMENT };
        }

        if (parentElement instanceof OpsWorksRootNode) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                            .getOpsWorksClient();
                    return ServiceAPIUtils.getAllStacks(client).toArray();
                }
            }.start();
        }

        if (parentElement instanceof Stack) {
            final String stackId = ((Stack) parentElement).getStackId();

            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                            .getOpsWorksClient();

                    List<Layer> layers = ServiceAPIUtils
                            .getAllLayersInStack(client, stackId);
                    List<LayerElementNode> layerNodes = new LinkedList<>();
                    for (Layer layer : layers) {
                        layerNodes.add(new LayerElementNode(layer));
                    }
                    LayersRootNode layersRoot = new LayersRootNode(layerNodes);

                    List<App> apps = ServiceAPIUtils
                            .getAllAppsInStack(client, stackId);
                    AppsRootNode appsRoot = new AppsRootNode(apps);

                    return new Object[] {appsRoot, layersRoot};
                }
            }.start();
        }

        if (parentElement instanceof LayersRootNode) {
            AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                    .getOpsWorksClient();

            List<LayerElementNode> layerNodes = ((LayersRootNode)parentElement).getLayerNodes();
            for (LayerElementNode layerNode : layerNodes) {
                List<Instance> instances = ServiceAPIUtils
                        .getAllInstancesInLayer(client, layerNode.getLayer().getLayerId());
                layerNode.setInstancesInLayer(instances);
            }

            return layerNodes.toArray();
        }

        if (parentElement instanceof LayerElementNode) {
            return ((LayerElementNode)parentElement).getInstancesInLayer().toArray();
        }

        if (parentElement instanceof AppsRootNode) {
            return ((AppsRootNode)parentElement).getApps().toArray();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.OPSWORKS;
    }

    @Override
    public void dispose() {
        viewer.removeOpenListener(listener);
        super.dispose();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        this.viewer.addOpenListener(listener);
    }

    public static OpsWorksContentProvider getInstance() {
        return instance;
    }

    private final IOpenListener listener = new IOpenListener() {

        @Override
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection)event.getSelection();

            Iterator<?> i = selection.iterator();
            while ( i.hasNext() ) {
                Object obj = i.next();
                // Node double-click actions
            }
        }
    };
}
