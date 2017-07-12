/*
 * Copyright 2013 Amazon Technologies, Inc.
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

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.databinding.RangeValidator;

public class CreateTableFirstPage extends WizardPage {

    private final String OK_MESSAGE = "Configure new DynamoDB table";
    private boolean usesRangeKey = false;
    private Font italicFont;
    private IObservableValue tableName;
    private IObservableValue hashKeyName;
    private IObservableValue hashKeyType;
    private IObservableValue enableRangeKey;
    private IObservableValue rangeKeyName;
    private IObservableValue rangeKeyType;
    private IObservableValue readCapacity;
    private IObservableValue writeCapacity;
    private final DataBindingContext bindingContext = new DataBindingContext();

    @Override
    public void dispose() {
        if (italicFont != null)
            italicFont.dispose();
        super.dispose();
    }

    private static final long CAPACITY_UNIT_MINIMUM = 1;
    private static final String[] DATA_TYPE_STRINGS = new String[] { "String", "Number", "Binary" };

    CreateTableFirstPage(CreateTableWizard wizard) {
        super("Configure table");
        setMessage(OK_MESSAGE);
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        tableName = PojoObservables.observeValue(wizard.getDataModel(), "tableName");
        hashKeyName = PojoObservables.observeValue(wizard.getDataModel(), "hashKeyName");
        hashKeyType = PojoObservables.observeValue(wizard.getDataModel(), "hashKeyType");
        enableRangeKey = PojoObservables.observeValue(wizard.getDataModel(), "enableRangeKey");
        rangeKeyName = PojoObservables.observeValue(wizard.getDataModel(), "rangeKeyName");
        rangeKeyType = PojoObservables.observeValue(wizard.getDataModel(), "rangeKeyType");
        readCapacity = PojoObservables.observeValue(wizard.getDataModel(), "readCapacity");
        writeCapacity = PojoObservables.observeValue(wizard.getDataModel(), "writeCapacity");
    }

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);

        // Table name
        Label tableNameLabel = new Label(comp, SWT.READ_ONLY);
        tableNameLabel.setText("Table Name:");
        final Text tableNameText = CreateTablePageUtil.newTextField(comp);
        bindingContext.bindValue(SWTObservables.observeText(tableNameText, SWT.Modify), tableName);
        ChainValidator<String> tableNameValidationStatusProvider = new ChainValidator<>(tableName, new NotEmptyValidator("Please provide a table name"));
        bindingContext.addValidationStatusProvider(tableNameValidationStatusProvider);

        // Hash key
        Group hashKeyGroup = CreateTablePageUtil.newGroup(comp, "Hash Key", 2);
        new Label(hashKeyGroup, SWT.READ_ONLY).setText("Hash Key Name:");
        final Text hashKeyText = CreateTablePageUtil.newTextField(hashKeyGroup);
        bindingContext.bindValue(SWTObservables.observeText(hashKeyText, SWT.Modify), hashKeyName);
        ChainValidator<String> hashKeyNameValidationStatusProvider = new ChainValidator<>(hashKeyName, new NotEmptyValidator("Please provide an attribute name for the hash key"));
        bindingContext.addValidationStatusProvider(hashKeyNameValidationStatusProvider);

        new Label(hashKeyGroup, SWT.READ_ONLY).setText("Hash Key Type:");
        final Combo hashKeyTypeCombo = new Combo(hashKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        hashKeyTypeCombo.setItems(DATA_TYPE_STRINGS);
        bindingContext.bindValue(SWTObservables.observeSelection(hashKeyTypeCombo), hashKeyType);
        hashKeyTypeCombo.select(0);

        // Range key
        Group rangeKeyGroup = CreateTablePageUtil.newGroup(comp, "Range Key", 2);
        final Button enableRangeKeyButton = new Button(rangeKeyGroup, SWT.CHECK);
        enableRangeKeyButton.setText("Enable Range Key");
        GridDataFactory.fillDefaults().span(2, 1).applyTo(enableRangeKeyButton);
        bindingContext.bindValue(SWTObservables.observeSelection(enableRangeKeyButton), enableRangeKey);
        final Label rangeKeyAttributeLabel = new Label(rangeKeyGroup, SWT.READ_ONLY);
        rangeKeyAttributeLabel.setText("Range Key Name:");
        final Text rangeKeyText = CreateTablePageUtil.newTextField(rangeKeyGroup);
        bindingContext.bindValue(SWTObservables.observeText(rangeKeyText, SWT.Modify), rangeKeyName);
        ChainValidator<String> rangeKeyNameValidationStatusProvider = new ChainValidator<>(rangeKeyName, enableRangeKey, new NotEmptyValidator(
                "Please provide an attribute name for the range key"));
        bindingContext.addValidationStatusProvider(rangeKeyNameValidationStatusProvider);

        final Label rangeKeyTypeLabel = new Label(rangeKeyGroup, SWT.READ_ONLY);
        rangeKeyTypeLabel.setText("Range Key Type:");
        final Combo rangeKeyTypeCombo = new Combo(rangeKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        rangeKeyTypeCombo.setItems(DATA_TYPE_STRINGS);
        bindingContext.bindValue(SWTObservables.observeSelection(rangeKeyTypeCombo), rangeKeyType);
        rangeKeyTypeCombo.select(0);
        enableRangeKeyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                usesRangeKey = enableRangeKeyButton.getSelection();
                rangeKeyAttributeLabel.setEnabled(usesRangeKey);
                rangeKeyText.setEnabled(usesRangeKey);
                rangeKeyTypeLabel.setEnabled(usesRangeKey);
                rangeKeyTypeCombo.setEnabled(usesRangeKey);
            }
        });
        enableRangeKeyButton.setSelection(false);
        rangeKeyAttributeLabel.setEnabled(usesRangeKey);
        rangeKeyText.setEnabled(usesRangeKey);
        rangeKeyTypeLabel.setEnabled(usesRangeKey);
        rangeKeyTypeCombo.setEnabled(usesRangeKey);

        FontData[] fontData = tableNameLabel.getFont().getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);

        // Table throughput
        Group throughputGroup = CreateTablePageUtil.newGroup(comp, "Table Throughput", 3);
        new Label(throughputGroup, SWT.READ_ONLY).setText("Read Capacity Units:");
        final Text readCapacityText = CreateTablePageUtil.newTextField(throughputGroup);
        readCapacityText.setText("" + CAPACITY_UNIT_MINIMUM);
        bindingContext.bindValue(SWTObservables.observeText(readCapacityText, SWT.Modify), readCapacity);
        ChainValidator<Long> readCapacityValidationStatusProvider = new ChainValidator<>(
                readCapacity, new RangeValidator(
                        "Please enter a read capacity of " + CAPACITY_UNIT_MINIMUM + " or more.", CAPACITY_UNIT_MINIMUM,
                        Long.MAX_VALUE));
        bindingContext.addValidationStatusProvider(readCapacityValidationStatusProvider);

        Label minimumReadCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
        minimumReadCapacityLabel.setText("(Minimum capacity " + CAPACITY_UNIT_MINIMUM + ")");
        minimumReadCapacityLabel.setFont(italicFont);

        new Label(throughputGroup, SWT.READ_ONLY).setText("Write Capacity Units:");
        final Text writeCapacityText = CreateTablePageUtil.newTextField(throughputGroup);
        writeCapacityText.setText("" + CAPACITY_UNIT_MINIMUM);
        Label minimumWriteCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
        minimumWriteCapacityLabel.setText("(Minimum capacity " + CAPACITY_UNIT_MINIMUM + ")");
        minimumWriteCapacityLabel.setFont(italicFont);
        bindingContext.bindValue(SWTObservables.observeText(writeCapacityText, SWT.Modify), writeCapacity);
        ChainValidator<Long> writeCapacityValidationStatusProvider = new ChainValidator<>(
                writeCapacity, new RangeValidator(
                        "Please enter a write capacity of " + CAPACITY_UNIT_MINIMUM + " or more.", CAPACITY_UNIT_MINIMUM,
                        Long.MAX_VALUE));
        bindingContext.addValidationStatusProvider(writeCapacityValidationStatusProvider);

        final Label throughputCapacityLabel = new Label(throughputGroup, SWT.WRAP);
        throughputCapacityLabel
                .setText("Amazon DynamoDB will reserve the necessary machine resources to meet your throughput needs based on the read and write capacity specified with consistent, low-latency performance.  You pay a flat, hourly rate based on the capacity you reserve.");
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 3;
        gridData.widthHint = 200;
        throughputCapacityLabel.setLayoutData(gridData);
        throughputCapacityLabel.setFont(italicFont);

        // Help info
        String pricingLinkText = "<a href=\"" + "http://aws.amazon.com/dynamodb/#pricing" + "\">" + "More information on Amazon DynamoDB pricing</a>. ";
        CreateTablePageUtil.newLink(new WebLinkListener(), pricingLinkText, throughputGroup);

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false)
                    return;

                IStatus status = (IStatus) value;
                if (status.isOK()) {
                    setErrorMessage(null);
                    setMessage(OK_MESSAGE, Status.OK);
                } else if (status.getSeverity() == Status.WARNING) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                }
                setPageComplete(status.isOK());
            }
        });
        setPageComplete(false);
        setControl(comp);
    }

}
