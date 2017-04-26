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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueEditor.STRING;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueEditor.NUMBER;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.S;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.N;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.MultiValueEditorDialog;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * Simple table dialog to allow user to enter multiple values for an
 * attribute.
 */
class MultiValueAttributeEditorDialog extends MultiValueEditorDialog {

    private int scalarDataType;
    public MultiValueAttributeEditorDialog(final Shell parentShell, final AttributeValue attributeValue, int selectedType) {
        super(parentShell, "Edit values", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "", MessageDialog.NONE, new String[] { "Save set", "Save single value", "Cancel"}, 0);
        this.values.addAll(AttributeValueUtil.getValuesFromAttribute(attributeValue));
        
        String dataTypeDescription;
        switch (selectedType) {
        case STRING:
            dataTypeDescription = "(String set)";
            scalarDataType = S;
            break;
        case NUMBER:
            dataTypeDescription = "(Number set)";
            scalarDataType = N;
            break;
        default:
            dataTypeDescription = "";
            break;
        }
        addColumnTextDescription(dataTypeDescription);
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        this.getButton(1).setEnabled(values.size() == 1);
    }
    
    @Override
    protected void modifyValue(TableItem item, int column, int index, Text text) {
        super.modifyValue(item, column, index, text);
        this.getButton(1).setEnabled(values.size() == 1);
        this.getButtonBar().update(); 
    }
    
    @Override
    protected void lockTableEditor(int index) {
        /* Also lock the Save single value button */
        this.getButton(1).setEnabled(false);
        super.lockTableEditor(index);
    }
    
    @Override
    protected void unlockTableEditor() {
        /* Also unlock the Save single value button */
        this.getButton(1).setEnabled(values.size() == 1);
        super.unlockTableEditor();
    }
    
    @Override
    protected boolean validateAttributeValue(String attributeValue) {
        return AttributeValueUtil.validateScalarAttributeInput(attributeValue, scalarDataType, true);
    }
    
}
