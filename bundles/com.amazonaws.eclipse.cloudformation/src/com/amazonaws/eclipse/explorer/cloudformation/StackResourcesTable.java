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
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.ec2.ui.views.instances.InstanceSelectionTable;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.StackResource;

class StackResourcesTable extends Composite {

    private TreeViewer viewer;
    private final StackEditorInput stackEditorInput;
    private InstanceSelectionTable instanceSelectionTable;

    public StackResourcesTable(Composite parent, FormToolkit toolkit, StackEditorInput stackEditorInput) {
        super(parent, SWT.NONE);
        this.stackEditorInput = stackEditorInput;

        this.setLayout(new GridLayout());

        instanceSelectionTable = new InstanceSelectionTable(this);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.minimumHeight = 200;
        gridData.heightHint    = 200;
        instanceSelectionTable.setLayoutData(gridData);

        Region region = RegionUtils.getRegionByEndpoint(stackEditorInput.getRegionEndpoint());
        if (region == null) {
            Status status = new Status(IStatus.WARNING, CloudFormationPlugin.PLUGIN_ID, "Unable to determine region from endpoint " + stackEditorInput.getRegionEndpoint());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        } else {
            String endpoint = region.getServiceEndpoint(ServiceAbbreviations.EC2);
            if (endpoint != null) {
                instanceSelectionTable.setEc2RegionOverride(region);
                instanceSelectionTable.refreshData();
            }
            if (endpoint == null) {
                Status status = new Status(IStatus.ERROR, CloudFormationPlugin.PLUGIN_ID, "Unable to determine EC2 endpoint for region " + region.getId());
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }

        Composite composite = toolkit.createComposite(this);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        StackResourcesContentProvider contentProvider = new StackResourcesContentProvider();
        StackResourcesLabelProvider labelProvider = new StackResourcesLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());

        refresh();
    }

    public void refresh() {
        new LoadStackResourcesThread().start();
        instanceSelectionTable.refreshData();
    }

    private AmazonCloudFormation getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(stackEditorInput.getAccountId());
        return clientFactory.getCloudFormationClientByEndpoint(stackEditorInput.getRegionEndpoint());
    }

    private class LoadStackResourcesThread extends Thread {
        @Override
        public void run() {
            try {
                DescribeStackResourcesRequest request = new DescribeStackResourcesRequest().withStackName(stackEditorInput.getStackName());
                final List<StackResource> stackResources = getClient().describeStackResources(request).getStackResources();

                List<String> instances = new LinkedList<>();
                for (StackResource resource : stackResources) {
                    if (resource.getResourceType().equalsIgnoreCase("AWS::EC2::Instance")) {
                        instances.add(resource.getPhysicalResourceId());
                    }
                }
                instanceSelectionTable.setInstancesToList(instances);

                Display.getDefault().asyncExec(new Runnable() {
                   @Override
                public void run() {
                       viewer.setInput(stackResources.toArray(new StackResource[stackResources.size()]));
                   }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, CloudFormationPlugin.PLUGIN_ID, "Unable to describe resources for stack " + stackEditorInput.getStackName(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

    private final class StackResourcesContentProvider implements ITreePathContentProvider {

        private StackResource[] resources;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof StackResource[]) {
                resources = (StackResource[])newInput;
            } else {
                resources = new StackResource[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return resources;
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

    private final class StackResourcesLabelProvider implements ITableLabelProvider {

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
            if (element instanceof StackResource == false) return "";

            StackResource resource = (StackResource)element;
            switch (columnIndex) {
                case 0: return resource.getLogicalResourceId();
                case 1: return resource.getPhysicalResourceId();
                case 2: return resource.getResourceType();
                case 3: return resource.getResourceStatus();
                case 4: return resource.getResourceStatusReason();
                default: return "";
            }
        }
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Logical ID");
        createColumn(tree, columnLayout, "Physical ID");
        createColumn(tree, columnLayout, "Type");
        createColumn(tree, columnLayout, "Status");
        createColumn(tree, columnLayout, "Status Reason");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

}