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
package com.amazonaws.eclipse.dynamodb.editor;


import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.N;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.S;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.NS;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.SS;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * Editor for attribute values including multi-value pop-up and data type
 * selection.
 */
final class AttributeValueEditor extends Composite {

    /*
     * Data types for the drop-down box.
     */
    static final String[] DATA_TYPE_ITEMS = new String[] { "String", "Number" };
    static final int STRING = 0;
    static final int NUMBER = 1;
    
    Text editorText;
    Button multiValueEditorButton;
    Button dataTypeButton;
    Combo dataTypeCombo;

    public AttributeValueEditor(Composite parent, int style, ControlEditor editor, int controlHeight,
            AttributeValue attributeValue) {
        super(parent, style);

        this.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 2;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        this.setLayout(layout);

        // Text field for typing in new values
        editorText = new Text(this, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).indent(2, 2).applyTo(editorText);

        // Button for invoking a multi-value editor
        multiValueEditorButton = new Button(this, SWT.None);
        multiValueEditorButton.setText("...");
        GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).applyTo(this.multiValueEditorButton);
        multiValueEditorButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        // Button for changing data type
        dataTypeButton = new Button(this, SWT.None);
        int selectedType;
        if ( attributeValue.getN() != null || attributeValue.getNS() != null ) {
            dataTypeButton.setImage(DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_ONE));
            selectedType = NUMBER;
        } else {
            // Default image and selected type is STRING
            dataTypeButton.setImage(DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_A));
            selectedType = STRING;
        }

        GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).applyTo(this.dataTypeButton);
        dataTypeButton.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        // Combo for selecting a data type once the above button is clicked.
        dataTypeCombo = new Combo(this, SWT.READ_ONLY | SWT.DROP_DOWN);
        GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).exclude(true)
                .applyTo(this.dataTypeCombo);
        dataTypeCombo.setVisible(false);
        dataTypeCombo.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        dataTypeCombo.setItems(DATA_TYPE_ITEMS);
        dataTypeCombo.select(selectedType);

        if ( editor != null ) {
            Point comboSize = dataTypeCombo.computeSize(SWT.DEFAULT, controlHeight);
            editor.minimumWidth = comboSize.x;
            editor.minimumHeight = comboSize.y;
        }

        configureDataTypeControlSwap(dataTypeButton, dataTypeCombo, this);
    }

    /**
     * Swaps the display of the two controls given when either is selected.
     */
    static void configureDataTypeControlSwap(final Button dataTypeButton, final Combo dataTypeCombo, final Composite parent) {
        dataTypeButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                parent.setRedraw(false);
                dataTypeButton.setVisible(false);
                GridDataFactory.createFrom((GridData) dataTypeButton.getLayoutData()).exclude(true)
                        .applyTo(dataTypeButton);

                dataTypeCombo.setVisible(true);
                GridDataFactory.createFrom((GridData) dataTypeCombo.getLayoutData()).exclude(false)
                        .applyTo(dataTypeCombo);

                parent.layout();
                dataTypeCombo.setListVisible(true);
                parent.setRedraw(true);
            }
        });

        dataTypeCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                parent.setRedraw(false);
                dataTypeCombo.setVisible(false);
                GridDataFactory.createFrom((GridData) dataTypeCombo.getLayoutData()).exclude(true)
                        .applyTo(dataTypeCombo);

                dataTypeButton.setVisible(true);
                GridDataFactory.createFrom((GridData) dataTypeButton.getLayoutData()).exclude(false)
                        .applyTo(dataTypeButton);

                if ( dataTypeCombo.getSelectionIndex() == STRING ) {
                    dataTypeButton.setImage(DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_A));
                } else {
                    dataTypeButton.setImage(DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_ONE));
                }

                parent.layout();
                parent.setRedraw(true);
            }
        });
    }
    
    /**
     *  Returns the currently selected data type.
     */
    public int getSelectedDataType(boolean isSetType) {
        int dataType;
        switch (this.dataTypeCombo.getSelectionIndex()) {
        case STRING:
            dataType = isSetType ? SS : S;
            break;
        case NUMBER:
            dataType = isSetType ? NS : N;
            break;
        default:
            throw new RuntimeException("Unexpected selection index "
                    + this.dataTypeCombo.getSelectionIndex());
        }
        return dataType;
    }
}
