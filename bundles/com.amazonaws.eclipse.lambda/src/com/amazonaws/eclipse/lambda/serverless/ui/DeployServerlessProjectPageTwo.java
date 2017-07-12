/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.serverless.ui;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

import com.amazonaws.eclipse.cloudformation.model.ParametersDataModel;
import com.amazonaws.eclipse.cloudformation.ui.ParametersComposite;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.DeployServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeployServerlessProjectPageTwo extends WizardPage {
    private final DeployServerlessProjectDataModel dataModel;
    private DataBindingContext bindingContext;
    private AggregateValidationStatus aggregateValidationStatus;

    private static final String OK_MESSAGE = "Provide values for template parameters.";
    private Composite comp;
    private ScrolledComposite scrolledComp;

    public DeployServerlessProjectPageTwo(DeployServerlessProjectDataModel dataModel) {
        super("Fill in stack template parameters");
        setTitle("Fill in stack template parameters");
        setDescription(OK_MESSAGE);
        this.dataModel = dataModel;
        initDataModel();
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

    // Fill in previous parameters from the existing stack
    private void fillinParameters() {
        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClientByRegion(
                dataModel.getRegionDataModel().getRegion().getId());
        if (dataModel.getStackDataModel().isSelectExistingResource()) {
            DescribeStacksResult describeStacks = cloudFormation.describeStacks(new DescribeStacksRequest()
                    .withStackName(dataModel.getStackDataModel().getStackName()));
            Stack existingStack = null;
            if ( describeStacks.getStacks().size() == 1 ) {
                existingStack = describeStacks.getStacks().get(0);
            }
            if (existingStack != null) {
                for ( Parameter param : existingStack.getParameters() ) {
                    boolean noEcho = false;

                    // This is a pain, but any "noEcho" parameters get returned as asterisks in the service response.
                    // The customer must fill these values out again, even for a running stack.
                    for ( TemplateParameter templateParam : dataModel.getParametersDataModel().getTemplateParameters()) {
                        if (templateParam.getNoEcho() && templateParam.getParameterKey().equals(param.getParameterKey())) {
                            noEcho = true;
                            break;
                        }
                    }

                    if ( !noEcho ) {
                        dataModel.getParametersDataModel().getParameterValues().put(
                                param.getParameterKey(), param.getParameterValue());
                    }
                }
            }
        }
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
            fillinParameters();
            createContents();
        }
        super.setVisible(visible);
    }

    private void initDataModel() {
        try {
            ParametersDataModel parametersDataModel = dataModel.getParametersDataModel();
            File serverlessTemplate = FunctionProjectUtil.getServerlessTemplateFile(dataModel.getProject());

            ObjectMapper mapper = new ObjectMapper();
            Map templateModel = mapper.readValue(serverlessTemplate, Map.class);
            parametersDataModel.setTemplate(templateModel);
        } catch (IOException e) {
            LambdaPlugin.getDefault().reportException(e.getMessage(), e);
        }
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
