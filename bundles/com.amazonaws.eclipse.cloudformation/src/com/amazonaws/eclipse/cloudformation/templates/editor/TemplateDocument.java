/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.cloudformation.templates.TemplateArrayNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNodePath.PathNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;

public class TemplateDocument extends Document {

    public static interface TemplateDocumentListener {
        void onTemplateDocumentChanged();
    }

    private final List<TemplateDocumentListener> listeners = new ArrayList<>();
    private TemplateNode model;
    /** The path from the root to the current position in the Json Document.*/
    private List<PathNode> subPaths;

    public void addTemplateDocumentListener(TemplateDocumentListener listener) {
        listeners.add(listener);
    }

    public void removeTemplateDocumentListener(TemplateDocumentListener listener) {
        listeners.remove(listener);
    }

    public TemplateNode getModel() {
        return model;
    }

    public void setModel(TemplateNode root) {
        this.model = root;

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                for (TemplateDocumentListener listener : listeners) {
                    listener.onTemplateDocumentChanged();
                }
            }
        });
    }

    public TemplateNode lookupNodeByPath(List<PathNode> subPaths) {
        TemplateNode node = model;

        if (subPaths.isEmpty() || !subPaths.get(0).getFieldName().equals(TemplateNode.ROOT_PATH)) {
            throw new RuntimeException("Unexpected path encountered");
        }

        for (int i = 1; i < subPaths.size(); ++i) {
            PathNode subPath = subPaths.get(i);

            if (node != null && node instanceof TemplateObjectNode) {
                TemplateObjectNode object = (TemplateObjectNode) node;
                String path = subPath.getFieldName();
                node = object.get(path);
            } else if (node != null && node instanceof TemplateArrayNode) {
                TemplateArrayNode array = (TemplateArrayNode) node;
                node = array.getMembers().get(subPath.getIndex());
            } else {
                throw new RuntimeException("Unexpected node structure");
            }
        }

        return node;
    }

    public TemplateNode findNode(int documentOffset) {
        return findNode(documentOffset, model);
    }

    private TemplateNode findNode(int documentOffset, TemplateNode node) {
        if (node.getStartLocation().getCharOffset() > documentOffset) {
            throw new RuntimeException("Out of bounds in node search");
        }

        if (node instanceof TemplateObjectNode) {
            TemplateObjectNode object = (TemplateObjectNode)node;
            for (Entry<String, TemplateNode> entry : object.getFields()) {
                entry.getKey();
                if (match(documentOffset, entry.getValue())) {
                    return findNode(documentOffset, entry.getValue());
                }
            }
        } else if (node instanceof TemplateArrayNode) {
            TemplateArrayNode array = (TemplateArrayNode)node;
            for (TemplateNode member : array.getMembers()) {
                if (match(documentOffset, member)) {
                    return findNode(documentOffset, member);
                }
            }
        }
        return node;
    }

    public void setSubPaths(List<PathNode> subPaths) {
        this.subPaths = subPaths;
    }

    public List<PathNode> getSubPaths(int offset) {
        if (subPaths != null && !subPaths.isEmpty()) {
            return subPaths;
        } else {
            TemplateNode node = findNode(offset);
            return node.getSubPaths();
        }
    }

    private boolean match(int offset, TemplateNode node) {
        return offset >= node.getStartLocation().getCharOffset() &&
               offset <=  node.getEndLocation().getCharOffset();
    }
}