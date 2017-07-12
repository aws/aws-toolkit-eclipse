/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.util.Iterator;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.NotEmptyValidator;

/**
 * Abstract base class with utilities common to all deploy wizard pages.
 */
public abstract class AbstractDeployWizardPage extends WizardFragment {

    @Override
    public boolean hasComposite() {
        return true;
    }

    protected DeployWizardDataModel wizardDataModel;

    /** Binding context for UI controls and deploy wizard data model */
    protected DataBindingContext bindingContext;

    /** Collective status of all validators in our binding context */
    protected AggregateValidationStatus aggregateValidationStatus;

    protected IWizardHandle wizardHandle;

    /**
     * Generic selection listener used by radio buttons created by this class to
     * notify the page to update controls and re-run binding validators.
     */
    protected final SelectionListener selectionListener = new SelectionListener() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
        @Override
        public void widgetSelected(SelectionEvent e) {
            radioButtonSelected(e.getSource());
            runValidators();
        }
    };

    /**
     * Initializes the data validators with a fresh state. Subclasses should
     * call this before performing data binding to ensure they don't have stale
     * handlers. Because these fragments persist in the workbench and the
     * objects are reused, this process must be performed somewhere in the
     * lifecycle other than the constructor.
     */
    protected final void initializeValidators() {
        bindingContext = new DataBindingContext();
        aggregateValidationStatus =
            new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    protected IChangeListener changeListener;

    /**
     * Subclasses can override this callback method to be notified when the value of a radio button
     * changes so that any additional UI updates can be made.
     */
    protected void radioButtonSelected(Object sourceButton) {
    }

    protected AbstractDeployWizardPage(DeployWizardDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;

        changeListener = new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false) return;

                wizardHandle.setMessage(getPageDescription(), IStatus.OK);

                IStatus status = (IStatus)value;
                setComplete(status.getSeverity() == IStatus.OK);
            }
        };
    }

    /**
     * Returns the page title for this fragment.
     */
    public abstract String getPageTitle();

    /**
     * Returns the "OK" status message for this fragment.
     */
    public abstract String getPageDescription();

    @Override
    public void enter() {
        if (wizardHandle != null) {
            wizardHandle.setTitle(getPageTitle());
            wizardHandle.setMessage(getPageDescription(), IStatus.OK);
        }
        if (aggregateValidationStatus != null)
            aggregateValidationStatus.addChangeListener(changeListener);
    }

    @Override
    public void exit() {
        if (aggregateValidationStatus != null)
            aggregateValidationStatus.removeChangeListener(changeListener);
    }

    @Override
    public void performCancel(IProgressMonitor monitor) throws CoreException {
        setComplete(false);
        exit();
    }

    @Override
    public void performFinish(IProgressMonitor monitor) throws CoreException {
        setComplete(false);
        exit();
    }

    /**
     * Runs all the validators for the current binding context.
     */
    protected void runValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

    /*
     * Widget Helper Methods
     */

    public static ControlDecoration newControlDecoration(Control control, String message) {
        ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
        decoration.setDescriptionText(message);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        return decoration;
    }

    public static Group newGroup(Composite parent, String text) {
        return newGroup(parent, text, 1);
    }

    public static Group newGroup(Composite parent, String text, int colspan) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(text);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        group.setLayoutData(gridData);
        group.setLayout(new GridLayout(1, false));
        return group;
    }

    public static Text newText(Composite parent) {
        return newText(parent, "");
    }

    public static Text newText(Composite parent, String value) {
        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        text.setText(value);
        return text;
    }

    public static Label newLabel(Composite parent, String text) {
        return newLabel(parent, text, 1);
    }

    public static Label newFillingLabel(Composite parent, String text) {
        return newFillingLabel(parent, text, 1);
    }

    public static Label newFillingLabel(Composite parent, String text, int colspan) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        gridData.widthHint = 100;
        label.setLayoutData(gridData);
        return label;
    }

    public static Label newLabel(Composite parent, String text, int colspan) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.horizontalSpan = colspan;
        label.setLayoutData(gridData);
        return label;
    }

    public static Label newLabel(Composite parent, String text, int colspan,
            int horizontalAlignment, int verticalAlignment) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData gridData = new GridData(horizontalAlignment, verticalAlignment, false, false);
        gridData.horizontalSpan = colspan;
        label.setLayoutData(gridData);
        return label;
    }

    public static Link newLink(Composite composite, String message) {
        Link link = new Link(composite, SWT.WRAP);
        WebLinkListener webLinkListener = new WebLinkListener();
        link.addListener(SWT.Selection, webLinkListener);
        link.setText(message);
        return link;
    }

    public static Combo newCombo(Composite parent) {
        return newCombo(parent, 1);
    }

    public static Combo newCombo(Composite parent, int colspan) {
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        combo.setLayoutData(gridData);
        return combo;
    }

    public static Button newCheckbox(Composite parent, String text, int colspan) {
        Button button = new Button(parent, SWT.CHECK);
        button.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        button.setLayoutData(gridData);
        return button;
    }

    public static UpdateValueStrategy newUpdateValueStrategy(ControlDecoration decoration, Button button) {
        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setAfterConvertValidator(new NotEmptyValidator(decoration, button));
        return strategy;
    }

    protected Button newRadioButton(Composite parent, String text, int colspan) {
        return newRadioButton(parent, text, colspan, false);
    }

    protected Button newRadioButton(Composite parent, String text, int colspan, boolean selected) {
        return newRadioButton(parent, text, colspan, selected, selectionListener);
    }

    public static Button newRadioButton(Composite parent, String text, int colspan,
            boolean selected, SelectionListener selectionListener) {
        Button radioButton = new Button(parent, SWT.RADIO);
        radioButton.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        radioButton.setLayoutData(gridData);
        radioButton.addSelectionListener(selectionListener);
        radioButton.setSelection(selected);
        return radioButton;
    }

    /**
     * Customize the link's layout data
     */
    public void adjustLinkLayout(Link link, int colspan) {
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        gridData.horizontalSpan = colspan;
        link.setLayoutData(gridData);
    }
}
