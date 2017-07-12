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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.StackEvent;

public class StackEventsTable extends Composite {

    private TreeViewer viewer;
    private final StackEditorInput stackEditorInput;


    private final class StackEventsContentProvider implements ITreePathContentProvider {

        private StackEvent[] events;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof StackEvent[]) {
                events = (StackEvent[])newInput;
            } else {
                events = new StackEvent[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return events;
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

    private final class StackEventsLabelProvider implements ITableLabelProvider {
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
            if (element instanceof StackEvent == false) return "";

            StackEvent stackEvent = (StackEvent)element;
            switch (columnIndex) {
                case 0: return stackEvent.getTimestamp().toString();
                case 1: return stackEvent.getResourceStatus();
                case 2: return stackEvent.getResourceType();
                case 3: return stackEvent.getLogicalResourceId();
                case 4: return stackEvent.getPhysicalResourceId();
                case 5: return stackEvent.getResourceStatusReason();
            }

            return element.toString();
        }

    }

    public StackEventsTable(Composite parent, FormToolkit toolkit, StackEditorInput stackEditorInput) {
        super(parent, SWT.NONE);
        this.stackEditorInput = stackEditorInput;

        this.setLayout(new GridLayout());

        Composite composite = toolkit.createComposite(this);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        StackEventsContentProvider contentProvider = new StackEventsContentProvider();
        StackEventsLabelProvider labelProvider = new StackEventsLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());

        refresh();
    }

    public void refresh() {
        new LoadStackEventsThread().start();
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Event Time");
        createColumn(tree, columnLayout, "State");
        createColumn(tree, columnLayout, "Resource Type");
        createColumn(tree, columnLayout, "Logical ID");
        createColumn(tree, columnLayout, "Physical ID");
        createColumn(tree, columnLayout, "Reason");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    private AmazonCloudFormation getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(stackEditorInput.getAccountId());
        return clientFactory.getCloudFormationClientByEndpoint(stackEditorInput.getRegionEndpoint());
    }

    private class LoadStackEventsThread extends Thread {
        @Override
        public void run() {
            try {
                DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackEditorInput.getStackName());
                final List<StackEvent> stackEvents = new LinkedList<>();
                DescribeStackEventsResult result = null;
                do {
                    if (result != null) request.setNextToken(result.getNextToken());
                    result = getClient().describeStackEvents(request);

                    stackEvents.addAll(result.getStackEvents());
                } while (result.getNextToken() != null);

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(stackEvents.toArray(new StackEvent[stackEvents.size()]));
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, CloudFormationPlugin.PLUGIN_ID, "Unable to describe events for stack " + stackEditorInput.getStackName(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }
}