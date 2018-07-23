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
package com.amazonaws.eclipse.explorer.sqs;

import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.ALL;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.ARN;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.CREATED;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.DELAY_SECONDS;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.MAX_MESSAGE_SIZE;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.NUMBER_OF_MESSAGES;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.RETENTION_PERIOD;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.SENDER_ID;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.SENT;
import static com.amazonaws.eclipse.explorer.sqs.QueueAttributes.VISIBILITY_TIMEOUT;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;


public class QueueEditor extends EditorPart implements IRefreshable {

    private QueueEditorInput queueEditorInput;
    private Text retentionPeriodLabel;
    private Text maxMessageSizeLabel;
    private Text createdLabel;
    private Text visibilityTimeoutLabel;
    private Text queueArnLabel;
    private Text numberOfMessagesLabel;
    private TreeViewer viewer;
    private Text queueDelayLabel;

    @Override
    public void doSave(IProgressMonitor arg0) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void setFocus() {}

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        queueEditorInput = (QueueEditorInput)input;
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

        form.setText(queueEditorInput.getName());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_QUEUE));
        form.getBody().setLayout(new GridLayout());


        createQueueSummaryInfoSection(form, toolkit);
        createMessageList(form, toolkit);

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().add(new Separator());
        form.getToolBarManager().add(new AddMessageAction(getClient(), queueEditorInput.getQueueUrl(), this));

        form.getToolBarManager().update(true);
    }

    private class DeleteMessageAction extends Action {
        public DeleteMessageAction() {
            this.setText("Delete");
            this.setToolTipText("Delete this message from the queue");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public boolean isEnabled() {
            return !viewer.getSelection().isEmpty();
        }

        @Override
        public void run() {
            StructuredSelection selection = (StructuredSelection)viewer.getSelection();
            Iterator<Message> iterator = selection.iterator();

            while (iterator.hasNext()) {
                Message message = iterator.next();
                getClient().deleteMessage(new DeleteMessageRequest(queueEditorInput.getQueueUrl(), message.getReceiptHandle()));
            }

            new RefreshAction().run();
        }
    }

    private class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh queue message sampling");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            new LoadMessagesThread().start();
            new LoadQueueAttributesThread().start();
        }
    }

    private void createQueueSummaryInfoSection(final ScrolledForm form, final FormToolkit toolkit) {
        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);

        Composite composite = toolkit.createComposite(form.getBody());
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        toolkit.createLabel(composite, "Retention Period:");
        retentionPeriodLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(retentionPeriodLabel);

        toolkit.createLabel(composite, "Max Message Size:");
        maxMessageSizeLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(maxMessageSizeLabel);

        toolkit.createLabel(composite, "Created:");
        createdLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(createdLabel);

        toolkit.createLabel(composite, "Visibility Timeout:");
        visibilityTimeoutLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(visibilityTimeoutLabel);

        toolkit.createLabel(composite, "Queue ARN:");
        queueArnLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(queueArnLabel);

        toolkit.createLabel(composite, "Approx. Message Count:");
        numberOfMessagesLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(numberOfMessagesLabel);

        toolkit.createLabel(composite, "Message Delay (seconds):");
        queueDelayLabel = toolkit.createText(composite, "", SWT.READ_ONLY);
        gridDataFactory.applyTo(queueDelayLabel);

        new LoadQueueAttributesThread().start();
    }

    private AmazonSQS getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(queueEditorInput.getAccountId());
        return clientFactory.getSQSClientByEndpoint(queueEditorInput.getRegionEndpoint());
    }

    private class LoadQueueAttributesThread extends Thread {
        @Override
        public void run() {
            GetQueueAttributesRequest request = new GetQueueAttributesRequest(queueEditorInput.getQueueUrl()).withAttributeNames(ALL);
            final Map<String, String> attributes = getClient().getQueueAttributes(request).getAttributes();

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    retentionPeriodLabel.setText(attributes.get(RETENTION_PERIOD));
                    maxMessageSizeLabel.setText(attributes.get(MAX_MESSAGE_SIZE));
                    createdLabel.setText(attributes.get(CREATED));
                    visibilityTimeoutLabel.setText(attributes.get(VISIBILITY_TIMEOUT));
                    queueArnLabel.setText(attributes.get(ARN));
                    numberOfMessagesLabel.setText(attributes.get(NUMBER_OF_MESSAGES));
                    queueDelayLabel.setText(valueOrDefault(attributes.get(DELAY_SECONDS), "0"));

                    numberOfMessagesLabel.getParent().layout();
                }
            });
        }

        private String valueOrDefault(String value, String defaultValue) {
            if (value != null) return value;
            else return defaultValue;
        }
    }

    private class LoadMessagesThread extends Thread {
        @Override
        public void run() {
            final Map<String, Message> messagesById = new HashMap<>();

            for (int i = 0; i < 5; i++) {
                ReceiveMessageRequest request = new ReceiveMessageRequest(queueEditorInput.getQueueUrl()).withVisibilityTimeout(0).withMaxNumberOfMessages(10).withAttributeNames(ALL);
                List<Message> messages = getClient().receiveMessage(request).getMessages();

                for (Message message : messages) {
                    messagesById.put(message.getMessageId(), message);
                }
            }

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    viewer.setInput(messagesById.values());
                }
            });
        }
    }

    private final class MessageContentProvider implements ITreePathContentProvider {

        private Message[] messages;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Collection) {
                messages = ((Collection<Message>)newInput).toArray(new Message[0]);
            } else {
                messages = new Message[0];
            }
        }

        @Override
        public Object[] getChildren(TreePath arg0) {
            return null;
        }

        @Override
        public Object[] getElements(Object arg0) {
            return messages;
        }

        @Override
        public TreePath[] getParents(Object arg0) {
            return new TreePath[0];
        }

        @Override
        public boolean hasChildren(TreePath arg0) {
            return false;
        }
    }

    private final class MessageLabelProvider implements ITableLabelProvider {

        private final DateFormat dateFormat;

        public MessageLabelProvider() {
            dateFormat = DateFormat.getDateTimeInstance();
        }

        @Override
        public void addListener(ILabelProviderListener arg0) {}
        @Override
        public void removeListener(ILabelProviderListener arg0) {}

        @Override
        public void dispose() {}

        @Override
        public boolean isLabelProperty(Object obj, String column) {
            return false;
        }

        @Override
        public Image getColumnImage(Object obj, int column) {
            return null;
        }

        @Override
        public String getColumnText(Object obj, int column) {
            if (obj instanceof Message == false) return "";

            Message message = (Message)obj;
            Map<String, String> attributes = message.getAttributes();

            switch (column) {
                case 0: return message.getMessageId();
                case 1: return message.getBody();
                case 2: return formatDate(attributes.get(SENT));
                case 3: return attributes.get(SENDER_ID);
            }

            return "";
        }

        private String formatDate(String epochString) {
            if (epochString == null || epochString.trim().length() == 0) return "";

            long epochSeconds = Long.parseLong(epochString);
            return dateFormat.format(new Date(epochSeconds));
        }
    }

    private void createMessageList(final ScrolledForm form, final FormToolkit toolkit) {
        Composite parent = toolkit.createComposite(form.getBody());
        parent.setLayout(new GridLayout());
        parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = toolkit.createLabel(parent, "Message Sampling");
        label.setFont(JFaceResources.getHeaderFont());
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        label.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));



        Composite composite = toolkit.createComposite(parent);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        MessageContentProvider contentProvider = new MessageContentProvider();
        MessageLabelProvider labelProvider = new MessageLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());
        viewer.setInput(new Object());


        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(new DeleteMessageAction());
            }
        });

        Menu menu = menuManager.createContextMenu(viewer.getTree());
        viewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuManager, viewer);

        new LoadMessagesThread().start();
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "ID");
        createColumn(tree, columnLayout, "Body");
        createColumn(tree, columnLayout, "Sent");
        createColumn(tree, columnLayout, "Sender");
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
        new RefreshAction().run();
    }

}
