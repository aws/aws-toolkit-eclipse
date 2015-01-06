package com.amazonaws.eclipse.opsworks.explorer.node;

import java.util.Collections;
import java.util.List;

import com.amazonaws.services.opsworks.model.Instance;
import com.amazonaws.services.opsworks.model.Layer;

public class LayerElementNode {

    private final Layer layer;

    private List<Instance> instancesInLayer;

    public LayerElementNode(Layer layer) {
        this.layer = layer;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setInstancesInLayer(List<Instance> instancesInLayer) {
        this.instancesInLayer = instancesInLayer;
    }

    public List<Instance> getInstancesInLayer() {
        return Collections.unmodifiableList(instancesInLayer);
    }

}
