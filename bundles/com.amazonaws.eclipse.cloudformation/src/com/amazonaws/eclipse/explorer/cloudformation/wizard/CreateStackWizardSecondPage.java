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
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.amazonaws.eclipse.cloudformation.ui.ParametersComposite;

/**
 * The second page in the create wizard second page
 */
public class CreateStackWizardSecondPage extends WizardPage {

    private final CreateStackWizardDataModel dataModel;
    private DataBindingContext bindingContext;
    private AggregateValidationStatus aggregateValidationStatus;

    private static final String OK_MESSAGE = "Provide values for template parameters.";
    private Composite comp;
    private ScrolledComposite scrolledComp;

    protected CreateStackWizardSecondPage(CreateStackWizardDataModel dataModel) {
        super("Fill in stack template parameters");
        setTitle("Fill in stack template parameters");
        setDescription(OK_MESSAGE);
        this.dataModel = dataModel;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    public void createControl(final Composite parent) {
        scrolledComp = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComp);

        comp = new Composite(scrolledComp, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(comp);
        scrolledComp.setContent(comp);

        scrolledComp.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                if (comp != null) {
                    Rectangle r = scrolledComp.getClientArea();
                    scrolledComp.setMinSize(comp.computeSize(r.width, SWT.DEFAULT));
                }
            }
        });

        setControl(scrolledComp);
    }

    private void createContents() {
        for ( Control c : comp.getChildren() ) {
            c.dispose();
        }

        bindingContext = new DataBindingContext();
        aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
                AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent event) {
                populateValidationStatus();
            }
        });

        new ParametersComposite(comp, dataModel.getParametersDataModel(), bindingContext);

        comp.layout();
        Rectangle r = scrolledComp.getClientArea();
        scrolledComp.setMinSize(comp.computeSize(r.width, SWT.DEFAULT));
    }

    @Override
    public void setVisible(boolean visible) {
        if ( visible ) {
            createContents();
        }
        super.setVisible(visible);
    }

    private void populateValidationStatus() {
        Object value = aggregateValidationStatus.getValue();
        if ( value instanceof IStatus == false )
            return;

        IStatus status = (IStatus) value;
        if ( status.isOK() ) {
            setErrorMessage(null);
            setMessage(OK_MESSAGE, Status.OK);
        } else if ( status.getSeverity() == Status.WARNING ) {
            setErrorMessage(null);
            setMessage(status.getMessage(), Status.WARNING);
        } else if ( status.getSeverity() == Status.ERROR ) {
            setErrorMessage(status.getMessage());
        }

        setPageComplete(status.isOK());
    }
}
