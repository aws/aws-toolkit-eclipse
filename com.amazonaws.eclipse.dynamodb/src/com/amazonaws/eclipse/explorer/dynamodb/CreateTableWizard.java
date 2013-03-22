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
package com.amazonaws.eclipse.explorer.dynamodb;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;

/**
 * Wizard to create a new DynamoDB table.
 */
class CreateTableWizard extends Wizard {

    private CreateTableWizard.CreateTableWizardPage page;

    public CreateTableWizard() {
        setNeedsProgressMonitor(true);
        setWindowTitle("Create New DynamoDB Table");
    }

    @Override
    public void addPages() {
        page = new CreateTableWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        final CreateTableRequest rq = page.getRequest();
        final String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        AmazonDynamoDB dynamoDBClient = AwsToolkitCore.getClientFactory(accountId).getDynamoDBClient();
        try {
            dynamoDBClient.createTable(rq);
        } catch ( AmazonClientException e ) {
            StatusManager.getManager().handle(
                    new Status(IStatus.ERROR, DynamoDBPlugin.PLUGIN_ID, "Failed to create table", e),
                    StatusManager.SHOW);
            return false;
        }
        return true;
    }

    private static class CreateTableWizardPage extends WizardPage {

        private final CreateTableRequest rq = new CreateTableRequest();
        private boolean usesRangeKey = false;
        private Font italicFont;

        @Override
        public void dispose() {
            if (italicFont != null) italicFont.dispose();
            super.dispose();
        }

        private static final String[] DATA_TYPES = new String[] { "S", "N" };
        private static final String[] DATA_TYPE_STRINGS = new String[] { "String", "Number" };

        protected CreateTableWizardPage() {
            super("Configure table");
            setMessage("Configure new DynamoDB table");
            setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                    .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
            rq.setTableName("");
            rq.setKeySchema(new KeySchema().withHashKeyElement(
                    new KeySchemaElement().withAttributeType("S").withAttributeName("")).withRangeKeyElement(
                    new KeySchemaElement().withAttributeType("S").withAttributeName("")));
            rq.setProvisionedThroughput(new ProvisionedThroughput());
        }

        public void createControl(Composite parent) {
            Composite comp = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);

            Label tableNameLabel = new Label(comp, SWT.READ_ONLY);
            tableNameLabel.setText("Table Name:");
            final Text tableNameText = newTextField(comp);
            tableNameText.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    rq.setTableName(tableNameText.getText());
                    validate();
                }
            });
            
            
            Group hashKeyGroup = newGroup(comp, "Hash Key", 2);

            new Label(hashKeyGroup, SWT.READ_ONLY).setText("Hash Key Name:");
            final Text hashKeyText = newTextField(hashKeyGroup);
            hashKeyText.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    rq.getKeySchema().getHashKeyElement().setAttributeName(hashKeyText.getText());
                    validate();
                }
            });

            new Label(hashKeyGroup, SWT.READ_ONLY).setText("Hash Key Type:");
            final Combo hashKeyTypeCombo = new Combo(hashKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            hashKeyTypeCombo.setItems(DATA_TYPE_STRINGS);
            hashKeyTypeCombo.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    rq.getKeySchema().getHashKeyElement()
                            .setAttributeType(DATA_TYPES[hashKeyTypeCombo.getSelectionIndex()]);
                    validate();
                }
            });
            hashKeyTypeCombo.select(0);

            
            Group rangeKeyGroup = newGroup(comp, "Range Key", 2);
            
            final Button enableRangeKey = new Button(rangeKeyGroup, SWT.CHECK);
            enableRangeKey.setText("Enable Range Key");
            GridDataFactory.fillDefaults().span(2, 1).applyTo(enableRangeKey);

            final Label rangeKeyAttributeLabel = new Label(rangeKeyGroup, SWT.READ_ONLY);
            rangeKeyAttributeLabel.setText("Range Key Name:");
            final Text rangeKeyText = newTextField(rangeKeyGroup);
            rangeKeyText.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    rq.getKeySchema().getRangeKeyElement().setAttributeName(rangeKeyText.getText());
                    validate();
                }
            });

            final Label rangeKeyTypeLabel = new Label(rangeKeyGroup, SWT.READ_ONLY);
            rangeKeyTypeLabel.setText("Range Key Type:");
            final Combo rangeKeyTypeCombo = new Combo(rangeKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            rangeKeyTypeCombo.setItems(DATA_TYPE_STRINGS);
            rangeKeyTypeCombo.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    rq.getKeySchema().getRangeKeyElement()
                            .setAttributeType(DATA_TYPES[rangeKeyTypeCombo.getSelectionIndex()]);
                    validate();
                }
            });
            rangeKeyTypeCombo.select(0);

            enableRangeKey.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    usesRangeKey = enableRangeKey.getSelection();
                    rangeKeyAttributeLabel.setEnabled(usesRangeKey);
                    rangeKeyText.setEnabled(usesRangeKey);
                    rangeKeyTypeLabel.setEnabled(usesRangeKey);
                    rangeKeyTypeCombo.setEnabled(usesRangeKey);
                    validate();
                }
            });
            enableRangeKey.setSelection(false);

            rangeKeyAttributeLabel.setEnabled(usesRangeKey);
            rangeKeyText.setEnabled(usesRangeKey);
            rangeKeyTypeLabel.setEnabled(usesRangeKey);
            rangeKeyTypeCombo.setEnabled(usesRangeKey);
            
            
            FontData[] fontData = tableNameLabel.getFont().getFontData();
            for (FontData fd : fontData) {
                fd.setStyle(SWT.ITALIC);
            }
            italicFont = new Font(Display.getDefault(), fontData);
            
            
            Group throughputGroup = newGroup(comp, "Table Throughput", 3);
            
            new Label(throughputGroup, SWT.READ_ONLY).setText("Read Capacity Units:");
            final Text readCapacityText = newTextField(throughputGroup);
            readCapacityText.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    try {
                        rq.getProvisionedThroughput().setReadCapacityUnits(Long.parseLong(readCapacityText.getText()));
                    } catch ( NumberFormatException e1 ) {
                        rq.getProvisionedThroughput().setReadCapacityUnits(null);
                    }
                    validate();
                }
            });
            readCapacityText.setText("3");
            Label minimumReadCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
            minimumReadCapacityLabel.setText("(Minimum capacity 1)");
            minimumReadCapacityLabel.setFont(italicFont);
            

            new Label(throughputGroup, SWT.READ_ONLY).setText("Write Capacity Units:");
            final Text writeCapacityText = newTextField(throughputGroup);
            writeCapacityText.addModifyListener(new ModifyListener() {

                public void modifyText(ModifyEvent e) {
                    try {
                        rq.getProvisionedThroughput()
                                .setWriteCapacityUnits(Long.parseLong(writeCapacityText.getText()));
                    } catch ( NumberFormatException e1 ) {
                        rq.getProvisionedThroughput().setWriteCapacityUnits(null);
                    }
                    validate();
                }
            });
            writeCapacityText.setText("5");
            Label minimumWriteCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
            minimumWriteCapacityLabel.setText("(Minimum capacity 1)");
            minimumWriteCapacityLabel.setFont(italicFont);

            final Label throughputCapacityLabel = new Label(throughputGroup, SWT.WRAP);
            throughputCapacityLabel.setText("Amazon DynamoDB will reserve the necessary machine resources to meet your throughput needs based on the read and write capacity specified with consistent, low-latency performance.  You pay a flat, hourly rate based on the capacity you reserve.");
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalSpan = 3;
            gridData.widthHint = 200;
            throughputCapacityLabel.setLayoutData(gridData);
            throughputCapacityLabel.setFont(italicFont);

            String pricingLinkText = "<a href=\"" + "http://aws.amazon.com/dynamodb/#pricing" + "\">" +
            		"More information on Amazon DynamoDB pricing</a>. ";
            newLink(new WebLinkListener(), pricingLinkText, throughputGroup);
            
            setControl(comp);
        }

        private Text newTextField(Composite comp) {
            Text text = new Text(comp, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
            return text;
        }

        protected Link newLink(Listener linkListener, String linkText, Composite composite) {
            Link link = new Link(composite, SWT.WRAP);
            link.setText(linkText);
            link.addListener(SWT.Selection, linkListener);
            GridData data = new GridData(SWT.FILL, SWT.TOP, false, false);
            data.horizontalSpan = 3;
            link.setLayoutData(data);
            
            return link;
        }
        
        private Group newGroup(Composite composite, String text, int columns) {
            Group group = new Group(composite, SWT.NONE);
            group.setText(text + ":");
            group.setLayout(new GridLayout(columns, false));
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalSpan = 2;
            group.setLayoutData(gridData);
            return group;
        }
        
        private void validate() {
            if (rq.getTableName().length() == 0) {
                setErrorMessage("Please provide a table name");
                setPageComplete(false);
                return;
            }

            if ( rq.getKeySchema().getHashKeyElement().getAttributeName().length() == 0 ) {
                setErrorMessage("Please provide an attribute name for the hash key");
                setPageComplete(false);
                return;
            }

            if ( rq.getProvisionedThroughput().getReadCapacityUnits() == null
                    || rq.getProvisionedThroughput().getReadCapacityUnits() < 5 ) {
                setErrorMessage("Please enter a read capacity of 5 or more.");
                setPageComplete(false);
                return;
            }

            if ( rq.getProvisionedThroughput().getWriteCapacityUnits() == null
                    || rq.getProvisionedThroughput().getWriteCapacityUnits() < 5 ) {
                setErrorMessage("Please enter a write capacity of 5 or more.");
                setPageComplete(false);
                return;
            }

            if ( usesRangeKey && rq.getKeySchema().getRangeKeyElement().getAttributeName().length() == 0 ) {
                setErrorMessage("Please provide an attribute name for the range key");
                setPageComplete(false);
                return;
            }

            setErrorMessage(null);
            setPageComplete(true);
        }

        @Override
        public boolean isPageComplete() {
            return getErrorMessage() == null;
        }

        CreateTableRequest getRequest() {
            if ( !usesRangeKey ) {
                rq.getKeySchema().setRangeKeyElement(null);
            }
            return rq;
        }

    }


}