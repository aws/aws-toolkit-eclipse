package com.amazonaws.eclipse.opsworks.explorer.node;

import java.util.Collections;
import java.util.List;

import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.explorer.image.OpsWorksExplorerImages;

public class LayersRootNode extends ExplorerNode {

    private final List<LayerElementNode> layerNodes;

    public LayersRootNode(List<LayerElementNode> layerNodes) {
        super("Layers", 1,
                OpsWorksPlugin.getDefault().getImageRegistry()
                      .get(OpsWorksExplorerImages.IMG_LAYER),
                null);
        this.layerNodes = Collections.unmodifiableList(layerNodes);
    }

    public List<LayerElementNode> getLayerNodes() {
        return layerNodes;
    }
}
