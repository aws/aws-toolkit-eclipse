package com.amazonaws.eclipse.dynamodb.editor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * A generic class that includes basic dialog template and value validation.
 */
public class AttributeValueInputDialog extends MessageDialog {
    
    final List<String> attributeNames;
    final Map<String, Integer> attributeDataTypes;
    final Map<String, String> attributeValues;
    final boolean cancelable;
    Label valueValidationWarningLabel;
    
    public AttributeValueInputDialog(final String dialogTitle, 
                                     final String dialogMessage,
                                     final boolean cancelable,
                                     final List<String> attributeNames,
                                     final Map<String, Integer> attributeDataTypes,
                                     final Map<String, String> initialTextualValue) {
        /* Use the current active shell and the default dialog template. */
        super(Display.getCurrent().getActiveShell(), 
              dialogTitle,
              AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON), 
              dialogMessage,
              MessageDialog.NONE, 
              cancelable ? new String[] {"OK", "Cancel"} : new String[] {"OK"}, 
              0);
        /* Hide the title bar which includes the close button */
        if ( !cancelable ) {
            setShellStyle(SWT.NONE);
        }
        this.cancelable = cancelable;
        /* Defensive hard copy */
        List<String> attributeNamesCopy = new LinkedList<>();
        attributeNamesCopy.addAll(attributeNames);
        this.attributeNames = Collections.unmodifiableList(attributeNamesCopy);
        Map<String, Integer> attributeDataTypesCopy = new HashMap<>();
        attributeDataTypesCopy.putAll(attributeDataTypes);
        this.attributeDataTypes = Collections.unmodifiableMap(attributeDataTypesCopy);
        /* Set initial values */
        this.attributeValues = new HashMap<>();
        if ( null != initialTextualValue ) {
            this.attributeValues.putAll(initialTextualValue);
        }
        /* Empty string for attribute not provided with initial value. */
        for (String attributeName : attributeNames) {
            if ( !attributeValues.containsKey(attributeName) ) {
                attributeValues.put(attributeName, "");
            }
        }
    }
    
    @Override
    protected void handleShellCloseEvent() {
        if ( !cancelable ) {
            /* Suppress colse event */
            return;
        } else {
            super.handleShellCloseEvent();
        }
    };
    
    @Override
    protected Control createCustomArea(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        
        for (final String attributeName : attributeNames) {
            Label nameLabel = new Label(comp, SWT.READ_ONLY);
            nameLabel.setText(attributeName + ":");
            final Text inputValueText = new Text(comp, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(inputValueText);
            /* Initial value */
            inputValueText.setText(attributeValues.get(attributeName));
            /* Modify listener that updates the UI and the underlying data model. */
            inputValueText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    attributeValues.put(attributeName, inputValueText.getText());
                    updateDialogUI();
                }
            });
        }
        
        new Label(comp, SWT.NONE);
        valueValidationWarningLabel = new Label(comp, SWT.WRAP);
        valueValidationWarningLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(valueValidationWarningLabel);
        updateDialogUI();
        return comp;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateDialogUI();
    }

    public String getInputValue(String attributeName) {
        return attributeValues.get(attributeName);
    }
    
    private void updateDialogUI() {
        if ( getButton(0) == null )
            return;
        
        /* Check whether we should show validation warning. */
        boolean showValueValidationWarning = false;
        for (String attributeName : attributeNames) {
            if ( attributeValues.get(attributeName) != null &&
                    !attributeValues.get(attributeName).isEmpty() ) {
                /* Only shows the warning when at least one input is not empty. */
                showValueValidationWarning = true;
                break;
            }
        }
        /* Validate each attribute value. */
        for (String attributeName : attributeNames) {
            String currentValue = attributeValues.get(attributeName);
            int dataType = attributeDataTypes.get(attributeName);
            if ( !AttributeValueUtil.validateScalarAttributeInput(currentValue, dataType, false) ) {
                if ( showValueValidationWarning ) {
                    valueValidationWarningLabel.setText(AttributeValueUtil
                            .getScalarAttributeValidationWarning(
                                    attributeName,
                                    dataType));
                } else {
                    valueValidationWarningLabel.setText("");
                }
                getButton(0).setEnabled(false);
                return;
            }
        }
        /* If all values are valid. */
        getButton(0).setEnabled(true);
        valueValidationWarningLabel.setText("");
        return;
    }
}
