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
package com.amazonaws.eclipse.sdk.ui.wizard;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.core.ui.MavenConfigurationComposite;
import com.amazonaws.eclipse.core.ui.ProjectNameComposite;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.SdkSamplesManager;
import com.amazonaws.eclipse.sdk.ui.model.NewAwsJavaProjectWizardDataModel;

/**
 * The first page of the AWS New Project Wizard. Allows the user to select:
 * <li> Account credentials
 * <li> A collection of samples to include in the new project
 */
class NewAwsJavaProjectWizardPageOne extends WizardPage {

    private static final String PAGE_NAME = NewAwsJavaProjectWizardPageOne.class.getName();

    private final NewAwsJavaProjectWizardDataModel dataModel;
    private final DataBindingContext dataBindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    // Composites modules in this page.
    private ProjectNameComposite projectNameComposite;
    private AccountSelectionComposite accountSelectionComposite;
    private MavenConfigurationComposite mavenConfigurationComposite;
    private SdkSamplesComposite sdkSamplesComposite;

    private ScrolledComposite scrolledComp;

    public NewAwsJavaProjectWizardPageOne(NewAwsJavaProjectWizardDataModel dataModel) {
        super(PAGE_NAME);
        setTitle("Create an AWS Java project");
        setDescription("Create a new AWS Java project in the workspace");
        this.dataModel = dataModel;
        this.dataBindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                populateValidationStatus();
            }
        });
    }

    private GridLayout initGridLayout(GridLayout layout, boolean margins) {
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        if ( margins ) {
            layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        } else {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        return layout;
    }

    @Override
    public void createControl(final Composite parent) {

        initializeDialogUnits(parent);
        Composite composite  = initCompositePanel(parent);
        createProjectNameComposite(composite);
        createMavenConfigurationComposite(composite);
        createAccountSelectionComposite(composite);
        createSamplesComposite(composite);

        setControl(scrolledComp);
    }

    private Composite initCompositePanel(Composite parent) {
        scrolledComp = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComp);

        final Composite composite = new Composite(scrolledComp, SWT.NULL);

        composite.setFont(parent.getFont());
        composite.setLayout(initGridLayout(new GridLayout(1, false), true));
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        scrolledComp.setContent(composite);

        scrolledComp.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = scrolledComp.getClientArea();
                scrolledComp.setMinSize(composite.computeSize(r.width, SWT.DEFAULT));
            }
        });

        return composite;
    }

    protected void createProjectNameComposite(Composite composite) {
        projectNameComposite = new ProjectNameComposite(
                composite, dataBindingContext, dataModel.getProjectNameDataModel());
    }

    protected void createSamplesComposite(Composite composite) {
        Group group = newGroup(composite, "AWS SDK for Java Samples");
        sdkSamplesComposite = new SdkSamplesComposite(group, dataModel.getSdkSamples());
    }

    protected void createAccountSelectionComposite(Composite composite) {
        Group group = newGroup(composite, "AWS Credentials");
        accountSelectionComposite = new AccountSelectionComposite(group, SWT.NONE);
        accountSelectionComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataModel.setAccountInfo(AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(
                        accountSelectionComposite.getSelectedAccountId()));
            }
        });
        if (accountSelectionComposite.getSelectedAccountId() != null)
            dataModel.setAccountInfo(AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(
                accountSelectionComposite.getSelectedAccountId()));
    }

    protected void createMavenConfigurationComposite(Composite composite) {
        Group group = newGroup(composite, "Maven configuration");
        mavenConfigurationComposite = new MavenConfigurationComposite(group,
                dataBindingContext, dataModel.getMavenConfigurationDataModel());
    }

    private void populateValidationStatus() {
        IStatus status = getValidationStatus();
        if (status == null) return;

        if (status.getSeverity() == IStatus.OK) {
            this.setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            setErrorMessage(status.getMessage());
            super.setPageComplete(false);
        }
    }

    private IStatus getValidationStatus() {
        if (aggregateValidationStatus == null) return null;
        Object value = aggregateValidationStatus.getValue();
        if (!(value instanceof IStatus)) return null;
        return (IStatus)value;
    }

    /**
     * Composite displaying the samples available in an SDK.
     */
    private static class SdkSamplesComposite extends Composite {
        private final List<SdkSample> sdkSamples;
        private final List<Button> buttons = new ArrayList<>();

        public SdkSamplesComposite(Composite parent, List<SdkSample> sdkSamples) {
            super(parent, SWT.NONE);
            this.sdkSamples = sdkSamples;
            createControls();
        }

        private void createControls() {
            for ( Control c : this.getChildren()) {
                c.dispose();
            }

            this.setLayout(new GridLayout());
            List<SdkSample> totalSamples = SdkSamplesManager.getSamples();

            for (SdkSample sample : totalSamples) {
                if (sample.getName() == null
                 || sample.getDescription() == null) {
                    // Sanity check - skip samples without names and descriptions.
                    continue;
                }

                Button button = new Button(this, SWT.CHECK | SWT.WRAP);
                button.setText(sample.getName());
                button.setData(sample);
                buttons.add(button);
                Label label = new Label(this, SWT.WRAP);
                label.setText(sample.getDescription());
                GridData gridData = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
                gridData.horizontalIndent = 25;
                label.setLayoutData(gridData);
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        onButtonSelected(event);
                    }
                });
            }
        }

        private void onButtonSelected(SelectionEvent event) {
            Button sourceButton  = (Button) event.getSource();
            if (sourceButton.getSelection()) {
                this.sdkSamples.add((SdkSample)sourceButton.getData());
            } else {
                this.sdkSamples.remove(sourceButton.getData());
            }
        }
    }
}
