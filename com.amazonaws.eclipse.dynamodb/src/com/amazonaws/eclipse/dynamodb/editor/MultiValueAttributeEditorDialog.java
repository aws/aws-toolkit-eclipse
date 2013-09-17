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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.MultiValueEditorDialog;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * Simple table dialog to allow use user to enter multiple values for an
 * attribute.
 */
class MultiValueAttributeEditorDialog extends MultiValueEditorDialog {

    public MultiValueAttributeEditorDialog(final Shell parentShell, final AttributeValue attributeValue) {
        super(parentShell, "Edit values", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "", MessageDialog.NONE, new String[] { "Save set", "Save single value", "Cancel"}, 0);
        this.values.addAll(AttributeValueUtil.getValuesFromAttribute(attributeValue));
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
    
}
