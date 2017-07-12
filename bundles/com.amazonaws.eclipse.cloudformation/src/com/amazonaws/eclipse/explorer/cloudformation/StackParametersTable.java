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

import com.amazonaws.services.cloudformation.model.Parameter;

class StackParametersTable extends Composite {
    private TreeViewer viewer;

    public StackParametersTable(Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);
        
        this.setLayout(new GridLayout());

        Composite composite = toolkit.createComposite(this);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        StackParametersContentProvider contentProvider = new StackParametersContentProvider();
        StackParametersLabelProvider labelProvider = new StackParametersLabelProvider();

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
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    public void updateStackParameters(List<Parameter> parameters) {
        viewer.setInput(parameters.toArray(new Parameter[parameters.size()]));
    }
    
    private class StackParametersContentProvider implements ITreePathContentProvider {
        Parameter[] parameters;
        
        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Parameter[]) parameters = (Parameter[])newInput;
            else parameters = new Parameter[0];
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return parameters;
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
    
    private class StackParametersLabelProvider implements ITableLabelProvider {
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
            if (element instanceof Parameter == false) return "";
            
            Parameter parameter = (Parameter)element;
            switch (columnIndex) {
                case 0: return parameter.getParameterKey();
                case 1: return parameter.getParameterValue();
                default: return "";
            }
        }
    }

    public void setStackParameters(List<Parameter> parameters) {
        if (parameters == null) viewer.setInput(new Parameter[0]);
        viewer.setInput(parameters.toArray(new Parameter[parameters.size()]));
    }
    
}