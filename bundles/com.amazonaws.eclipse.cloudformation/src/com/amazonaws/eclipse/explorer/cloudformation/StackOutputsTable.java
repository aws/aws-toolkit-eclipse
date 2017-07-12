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
package com.amazonaws.eclipse.explorer.cloudformation;

import java.util.List;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.services.cloudformation.model.Output;

class StackOutputsTable extends Composite {
    private TreeViewer viewer;

    public StackOutputsTable(Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.setLayout(new GridLayout());

        Composite composite = toolkit.createComposite(this);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        StackOutputsContentProvider contentProvider = new StackOutputsContentProvider();
        StackOutputsLabelProvider labelProvider = new StackOutputsLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Key");
        createColumn(tree, columnLayout, "Value");
        createColumn(tree, columnLayout, "Description");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    public void updateStackOutputs(List<Output> outputs) {
        viewer.setInput(outputs.toArray(new Output[outputs.size()]));
    }

    private class StackOutputsContentProvider implements ITreePathContentProvider {

        private Output[] outputs;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Output[]) {
                outputs = (Output[]) newInput;
            } else {
                outputs = new Output[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return outputs;
        }

        @Override
        public Object[] getChildren(TreePath parentPath) {
            return null;
        }

        @Override
        public boolean hasChildren(TreePath path) {
            return false;
        }

        @Override
        public TreePath[] getParents(Object element) {
            return new TreePath[0];
        }
    }

    private class StackOutputsLabelProvider implements ITableLabelProvider {

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
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof Output == false) return "";

            Output output = (Output) element;
            switch (columnIndex) {
                case 0: return output.getOutputKey();
                case 1: return output.getOutputValue();
                case 2: return output.getDescription();
                default: return "";
            }
        }
    }

    public void setStackOutputs(List<Output> outputs) {
        if (outputs == null) viewer.setInput(new Output[0]);
        viewer.setInput(outputs.toArray(new Output[outputs.size()]));
    }
}