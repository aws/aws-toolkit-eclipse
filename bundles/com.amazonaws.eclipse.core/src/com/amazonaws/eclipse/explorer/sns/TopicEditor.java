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
package com.amazonaws.eclipse.explorer.sns;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.GetTopicAttributesRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.UnsubscribeRequest;

public class TopicEditor extends EditorPart implements IRefreshable {

    private TopicEditorInput topicEditorInput;
    private TreeViewer viewer;
    private Text ownerLabel;
    private Text pendingSubscriptionsLabel;
    private Text confirmedSubscriptionsLabel;
    private Text deletedSubscriptionsLabel;
    private Text displayNameLabel;
    private Text topicArnLabel;

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        topicEditorInput = (TopicEditorInput)input;
    }

    @Override
    public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(topicEditorInput.getName());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_QUEUE));
        form.getBody().setLayout(new GridLayout());

        createTopicSummaryComposite(toolkit, form.getBody());
        createSubscriptionsComposite(toolkit, form.getBody());

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().add(new Separator());
        form.getToolBarManager().add(new PublishMessageAction(getClient(), topicEditorInput.getTopic()));
        form.getToolBarManager().add(new NewSubscriptionAction(getClient(), topicEditorInput.getTopic(), this));

        form.getToolBarManager().update(true);
    }


    private class LoadTopicAttributesThread extends Thread {
        @Override
        public void run() {
            try {
                GetTopicAttributesRequest request = new GetTopicAttributesRequest(topicEditorInput.getTopic().getTopicArn());
                final Map<String, String> attributes = getClient().getTopicAttributes(request).getAttributes();

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        ownerLabel.setText(getValue(attributes,"Owner"));
                        pendingSubscriptionsLabel.setText(getValue(attributes, "SubscriptionsPending"));
                        confirmedSubscriptionsLabel.setText(getValue(attributes, "SubscriptionsConfirmed"));
                        deletedSubscriptionsLabel.setText(getValue(attributes, "SubscriptionsDeleted"));
                        displayNameLabel.setText(getValue(attributes, "DisplayName"));
                        topicArnLabel.setText(getValue(attributes, "TopicArn"));
                    }

                    private String getValue(Map<String, String> map, String key) {
                        if (map.get(key) != null) return map.get(key);
                        return "";
                    }
                });
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to load topic attributes", e);
            }
        }
    }

    private final class UnsubscribeAction extends Action {
        public UnsubscribeAction() {
            this.setText("Delete Subscription");
            this.setToolTipText("Delete the selected subscriptions");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public boolean isEnabled() {
            boolean isEmpty = viewer.getSelection().isEmpty();
            return !isEmpty;
        }

        @Override
        public void run() {
            MessageDialog confirmationDialog = new MessageDialog(
                Display.getDefault().getActiveShell(),
                "Delete Subscriptions?", null,
                "Are you sure you want to delete the selected subscriptions?",
                MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 0);

            if (confirmationDialog.open() == 0) {
                StructuredSelection selection = (StructuredSelection)viewer.getSelection();
                Iterator<Subscription> iterator = selection.iterator();
                while (iterator.hasNext()) {
                    Subscription subscription = iterator.next();
                    try {
                        getClient().unsubscribe(new UnsubscribeRequest(subscription.getSubscriptionArn()));
                    } catch (Exception e) {
                        AwsToolkitCore.getDefault().reportException("Unable to delete subscription", e);
                    }
                }

                new RefreshAction().run();
            }
        }
    }

    private final class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh topic information and subscriptions");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            new LoadTopicAttributesThread().start();
            new LoadSubscriptionsThread().start();
        }
    }

    private AmazonSNS getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(topicEditorInput.getAccountId());
        return clientFactory.getSNSClientByEndpoint(topicEditorInput.getRegionEndpoint());
    }


    private final class SubscriptionContentProvider implements ITreePathContentProvider {

        private Subscription[] subscriptions;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof List) {
                subscriptions = ((List<Subscription>)newInput).toArray(new Subscription[0]);
            } else {
                subscriptions = new Subscription[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return subscriptions;
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
            return null;
        }
    }

    private final class SubscriptionLabelProvider implements ITableLabelProvider {

        @Override
        public void dispose() {}
        @Override
        public void addListener(ILabelProviderListener listener) {}
        @Override
        public void removeListener(ILabelProviderListener listener) {}

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
            if (element instanceof Subscription == false) return "???";

            Subscription subscription = (Subscription)element;
            switch (columnIndex) {
                case 0: return subscription.getProtocol();
                case 1: return subscription.getOwner();
                case 2: return subscription.getEndpoint();
                case 3: return subscription.getSubscriptionArn();
                default: return "";
            }
        }

    }

    private void createTopicSummaryComposite(FormToolkit toolkit, Composite parent) {
        GridDataFactory gdf = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false);

        Composite summaryComposite = toolkit.createComposite(parent, SWT.NONE);
        summaryComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        summaryComposite.setLayout(new GridLayout(2, false));

        toolkit.createLabel(summaryComposite, "Owner ID:");
        ownerLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(ownerLabel);

        toolkit.createLabel(summaryComposite, "Pending Subscriptions:");
        pendingSubscriptionsLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(pendingSubscriptionsLabel);


        toolkit.createLabel(summaryComposite, "Confirmed Subscriptions:");
        confirmedSubscriptionsLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(confirmedSubscriptionsLabel);

        toolkit.createLabel(summaryComposite, "Deleted Subscriptions:");
        deletedSubscriptionsLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(deletedSubscriptionsLabel);


        toolkit.createLabel(summaryComposite, "Display Name:");
        displayNameLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(displayNameLabel);

        toolkit.createLabel(summaryComposite, "Topic ARN:");
        topicArnLabel = toolkit.createText(summaryComposite, "", SWT.READ_ONLY);
        gdf.applyTo(topicArnLabel);

        new LoadTopicAttributesThread().start();
    }

    private void createSubscriptionsComposite(FormToolkit toolkit, Composite parent) {
        Composite subscriptionsComposite = toolkit.createComposite(parent);
        subscriptionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        subscriptionsComposite.setLayout(new GridLayout());

        Label label = toolkit.createLabel(subscriptionsComposite, "Subscriptions");
        label.setFont(JFaceResources.getHeaderFont());
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        label.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        Composite composite = toolkit.createComposite(subscriptionsComposite);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        SubscriptionContentProvider contentProvider = new SubscriptionContentProvider();
        SubscriptionLabelProvider labelProvider = new SubscriptionLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());
        viewer.setInput(new Object());

        final IRefreshable refreshable = this;
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(new NewSubscriptionAction(getClient(), topicEditorInput.getTopic(), refreshable));
                manager.add(new UnsubscribeAction());
            }
        });

        Menu menu = menuManager.createContextMenu(viewer.getTree());
        viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);

        refreshData();
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Protocol");
        createColumn(tree, columnLayout, "Owner");
        createColumn(tree, columnLayout, "Endpoint");
        createColumn(tree, columnLayout, "Subscription ARN");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    @Override
    public void refreshData() {
        new LoadSubscriptionsThread().start();
    }

    private class LoadSubscriptionsThread extends Thread {
        @Override
        public void run() {
            AmazonSNS sns = getClient();

            try {
                String topicArn = topicEditorInput.getTopic().getTopicArn();
                String nextToken = null;
                ListSubscriptionsByTopicResult subscriptionsByTopic = null;
                final List<Subscription> subscriptions = new LinkedList<>();
                do {
                    if (subscriptionsByTopic != null) nextToken = subscriptionsByTopic.getNextToken();
                    subscriptionsByTopic = sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, nextToken));
                    subscriptions.addAll(subscriptionsByTopic.getSubscriptions());
                } while (subscriptionsByTopic.getNextToken() != null);

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(subscriptions);
                    }
                });
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to list subscriptions", e);
            }
        }
    }

}
