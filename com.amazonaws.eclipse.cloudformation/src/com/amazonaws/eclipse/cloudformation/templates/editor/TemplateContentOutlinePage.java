/*
 * Copyright 2012 Amazon Technologies, Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonLocation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.amazonaws.eclipse.cloudformation.templates.TemplateArrayNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocumentListener;

public class TemplateContentOutlinePage extends ContentOutlinePage implements TemplateDocumentListener {

    private final TemplateDocument document;

    public TemplateContentOutlinePage(TemplateDocument document) {
        this.document = document;
    }

    @Override
    public void dispose() {
        // TODO: Remove the document listener
        super.dispose();
    }


    public void createControl(Composite parent) {
        super.createControl(parent);
        final TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new TemplateOutlineContentProvider());
        viewer.setLabelProvider(new TemplateOutlineLabelProvider());
        viewer.addSelectionChangedListener(this);
        viewer.setAutoExpandLevel(2);

        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                TemplateOutlineNode selectedNode = (TemplateOutlineNode)((StructuredSelection)event.getSelection()).getFirstElement();

                JsonLocation startLocation = selectedNode.getNode().getStartLocation();
                JsonLocation endLocation   = selectedNode.getNode().getEndLocation();

                IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                if (activeEditor != null) {
                    TemplateEditor templateEditor = (TemplateEditor)activeEditor;
                    templateEditor.setHighlightRange(
                        (int)startLocation.getCharOffset(),
                        (int)endLocation.getCharOffset() - (int)startLocation.getCharOffset(), true);
                }
            }
        });

        document.addTemplateDocumentListener(this);
        updateContent();
    }

    // TODO: move/rename me
    private Set<String> expandedPaths = new HashSet<String>();

    private void updateContent() {
        TreeViewer viewer = getTreeViewer();

        if (document.getModel() == null) return;

        expandedPaths = new HashSet<String>();
        for (Object obj : viewer.getExpandedElements()) {
            TemplateOutlineNode expandedNode = (TemplateOutlineNode)obj;
            expandedPaths.add(expandedNode.getNode().getPath());
        }

        viewer.setInput(new TemplateOutlineNode("ROOT", document.getModel()));

        for (TreeItem treeItem : viewer.getTree().getItems()) {
            expandTreeItems(treeItem, expandedPaths);
        }
    }

    private void expandTreeItems(TreeItem treeItem, Set<String> paths) {
        TemplateOutlineNode node = (TemplateOutlineNode)treeItem.getData();
        if (node == null) return;
        
        if (paths.contains(node.getNode().getPath())) {
            getTreeViewer().setExpandedState(node, true);
            getTreeViewer().refresh(node, true);
        }

        for (TreeItem child : treeItem.getItems()) {
            expandTreeItems(child, paths);
        }
    }

    public class TemplateOutlineLabelProvider implements ILabelProvider {
        public void dispose() {}
        public void addListener(ILabelProviderListener listener) {}
        public void removeListener(ILabelProviderListener listener) {}

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public Image getImage(Object element) {
            return null;
        }

        public String getText(Object element) {
            if (element instanceof TemplateOutlineNode == false) return null;

            TemplateOutlineNode node = (TemplateOutlineNode)element;
            return node.getText();
        }
    }

    public class TemplateOutlineNode {
        private final String text;
        private TemplateNode node;

        public TemplateOutlineNode(String text, TemplateNode node) {
            this.text = text;
            this.node = node;
        }

        @Override
        public boolean equals(Object arg0) {
            if (arg0 instanceof TemplateOutlineNode == false) return false;

            TemplateOutlineNode other = (TemplateOutlineNode)arg0;
            return (other.getNode().equals(node));
        }

        public TemplateNode getNode() {
            return node;
        }
        
        @Override
        public int hashCode() {
            if (node == null) {
                return 0;
            }
            return node.hashCode();
        }

        public String getText() {
            return text;
        }

        public TemplateOutlineNode[] getChildren() {
            if (node == null) return new TemplateOutlineNode[0];

            List<TemplateOutlineNode> children = new ArrayList<TemplateOutlineNode>();
            if (node.isObject()) {
                TemplateObjectNode object = (TemplateObjectNode)node;

                for (Entry<String, TemplateNode> entry : object.getFields()) {
                    if (entry.getValue().isValue()) {
                        children.add(new TemplateOutlineNode(entry.getKey() + ": " + ((TemplateValueNode)entry.getValue()).getText(), entry.getValue()));
                    } else {
                        children.add(new TemplateOutlineNode(entry.getKey(), entry.getValue()));
                    }
                }
            } else if (node.isArray()) {
                TemplateArrayNode array = (TemplateArrayNode)node;
                
                for (TemplateNode node : array.getMembers()) {
                    if (node.isObject()) children.add(new TemplateOutlineNode("{Object}", node));
                    if (node.isArray())  children.add(new TemplateOutlineNode("{Array}", node));
                    if (node.isValue())  children.add(new TemplateOutlineNode(((TemplateValueNode)node).getText(), node));
                }
            }
            
            return children.toArray(new TemplateOutlineNode[0]);
        }
    }

    public class TemplateOutlineContentProvider implements ITreeContentProvider {
        public void dispose() {}

        private TemplateOutlineNode root;

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput == null) {
                root = null;
            } else if (newInput instanceof TemplateOutlineNode) {
                root = (TemplateOutlineNode)newInput;
            } else if (newInput instanceof TemplateObjectNode) {
                root = new TemplateOutlineNode("ROOT/", (TemplateObjectNode)newInput);
            } else {
                throw new RuntimeException("Unexpected input!!!");
            }
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        public Object[] getChildren(Object parentElement) {
            if (root == null) return new Object[0];

            if (parentElement instanceof TemplateOutlineNode) {
                return ((TemplateOutlineNode)parentElement).getChildren();
            }

            return null;
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            if (element instanceof TemplateOutlineNode) {
                return ((TemplateOutlineNode) element).getChildren().length > 0;
            } else {
                return false;
            }
        }
    }

    public void templateDocumentChanged() {
        updateContent();
    }
}