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
package com.amazonaws.eclipse.opsworks.explorer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import com.amazonaws.eclipse.opsworks.explorer.node.LayerElementNode;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Instance;

public class ExplorerNodeDecorator implements ILightweightLabelDecorator {

    private static final Map<String, String> APP_TYPES_MAPPING = new HashMap<>();
    static {
        APP_TYPES_MAPPING.put("java", "Java");
        APP_TYPES_MAPPING.put("rails", "Ruby on Rails");
        APP_TYPES_MAPPING.put("php", "PHP");
        APP_TYPES_MAPPING.put("nodejs", "Node.js");
        APP_TYPES_MAPPING.put("static", "Static");
    }

    @Override
    public void addListener(ILabelProviderListener listener) {}
    @Override
    public void removeListener(ILabelProviderListener listener) {}
    @Override
    public void dispose() {}

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof LayerElementNode) {
            List<Instance> instances = ((LayerElementNode)element).getInstancesInLayer();
            int count = instances == null ? 0 : instances.size();
            if (count > 1) {
                decoration.addSuffix(" (" + count + " instances)");
            } else {
                decoration.addSuffix(" (" + count + " instance)");
            }
        }

        if (element instanceof Instance) {
            Instance node = (Instance)element;
            decoration.addSuffix(String.format(" %s (%s)", node.getPublicIp(), node.getStatus()));
        }

        if (element instanceof App) {
            App node = (App)element;
            String appTypeName = APP_TYPES_MAPPING.get(node.getType());
            if (appTypeName != null) {
                decoration.addSuffix(" (" + appTypeName + ")");
            }
        }
    }

}
