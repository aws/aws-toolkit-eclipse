/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.Collection;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.databinding.NotInListValidator;

/**
 * Simple wizard to export an environment configuration template.
 */
public class ExportTemplateDialog extends MessageDialog {

    private String templateName;
    private Collection<String> existingTemplateNames;
    private DataBindingContext bindingContext = new DataBindingContext();
    private IObservableValue isCreatingNew = new WritableValue();
    private IObservableValue newTemplateName = new WritableValue();
    private IObservableValue existingTemplateName = new WritableValue();
    private IObservableValue templateDescription = new WritableValue();
    private AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
            AggregateValidationStatus.MAX_SEVERITY);

    public boolean isCreatingNew() {
        return (Boolean) isCreatingNew.getValue();
    }

    public String getTemplateDescription() {
        return (String) templateDescription.getValue();
    }

    public String getTemplateName() {
        if ( isCreatingNew() ) {
            return (String) newTemplateName.getValue();
        } else {
            return (String) existingTemplateName.getValue();
        }
    }

    public ExportTemplateDialog(Shell parentShell, Collection<String> existingTemplateNames, String defaultTemplateName) {
        super(parentShell, "Export configuration template", null, "Choose a name and description for your template",
                MessageDialog.NONE, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        this.templateName = defaultTemplateName;
        this.existingTemplateNames = existingTemplateNames;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        parent.setLayout(new FillLayout());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        Group templateNameGroup = new Group(composite, SWT.None);
        templateNameGroup.setLayout(new GridLayout(2, false));
        GridData groupData = new GridData();
        groupData.horizontalSpan = 2;
        templateNameGroup.setLayoutData(groupData);

        // Update existing template
        final Button updateExistingRadioButton = new Button(templateNameGroup, SWT.RADIO);
        updateExistingRadioButton.setText("Update an existing template");
        final Combo existingTemplateNamesCombo = new Combo(templateNameGroup, SWT.READ_ONLY);
        existingTemplateNamesCombo.setEnabled(false);
        if ( existingTemplateNames.isEmpty() ) {
            updateExistingRadioButton.setEnabled(false);
        } else {
            existingTemplateNamesCombo
                    .setItems(existingTemplateNames.toArray(new String[existingTemplateNames.size()]));
            existingTemplateNamesCombo.select(0);
        }

        // Create new template -- default option
        Button createNewRadioButton = new Button(templateNameGroup, SWT.RADIO);
        createNewRadioButton.setText("Create a new template");
        final Text templateNameText = new Text(templateNameGroup, SWT.BORDER);
        templateNameText.setText(templateName);
        templateNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        updateExistingRadioButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                templateNameText.setEnabled(!updateExistingRadioButton.getSelection());
                existingTemplateNamesCombo.setEnabled(updateExistingRadioButton.getSelection());
            }

        });

        // Description
        new Label(composite, SWT.NONE).setText("Template description: ");
        final Text templateDescriptionText = new Text(composite, SWT.BORDER);
        templateDescriptionText.setText("");
        templateDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Data binding
        bindingContext.bindValue(SWTObservables.observeSelection(createNewRadioButton), isCreatingNew);
        isCreatingNew.setValue(true);

        bindingContext.bindValue(SWTObservables.observeSelection(existingTemplateNamesCombo), existingTemplateName)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeText(templateNameText, SWT.Modify), newTemplateName);
        bindingContext.bindValue(SWTObservables.observeText(templateDescriptionText, SWT.Modify), templateDescription);

        WritableSet inUseNames = new WritableSet();
        inUseNames.addAll(existingTemplateNames);
        ChainValidator<String> validator = new ChainValidator<>(newTemplateName, isCreatingNew,
                new NotEmptyValidator("Template name cannot be empty"), new NotInListValidator<String>(inUseNames,
                        "Template name already in use"));
        bindingContext.addValidationStatusProvider(validator);

        // Decorate the new name field with error status
        ControlDecoration decoration = new ControlDecoration(templateNameText, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText("Invalid value");
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        new DecorationChangeListener(decoration, validator.getValidationStatus());

        return composite;
    }

    /**
     * We need to add our button enabling listener here, because they haven't
     * been created yet in createCustomArea
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                Button okButton = getButton(0);
                if ( okButton != null ) {
                    if ( status.getSeverity() == IStatus.OK ) {
                        okButton.setEnabled(true);
                    } else {
                        okButton.setEnabled(false);
                    }
                }
            }
        });
        
        getButton(0).setEnabled(false);
    }
}
