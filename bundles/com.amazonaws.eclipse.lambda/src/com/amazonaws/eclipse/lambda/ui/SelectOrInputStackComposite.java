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
package com.amazonaws.eclipse.lambda.ui;

import static com.amazonaws.eclipse.lambda.model.SelectOrInputStackDataModel.LOADING;
import static com.amazonaws.eclipse.lambda.model.SelectOrInputStackDataModel.NONE_FOUND;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.cloudformation.CloudFormationUtils;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.core.ui.SelectOrInputComposite;
import com.amazonaws.eclipse.lambda.model.SelectOrInputStackDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.validator.StackNameValidator;
import com.amazonaws.services.cloudformation.model.StackSummary;

public class SelectOrInputStackComposite extends SelectOrInputComposite<StackSummary, SelectOrInputStackDataModel> {

    private final static StackNotExistsValidator stackNotExistsValidator = new StackNotExistsValidator();

    public SelectOrInputStackComposite(
            Composite parent,
            DataBindingContext bindingContext,
            SelectOrInputStackDataModel dataModel) {

        super(parent, bindingContext, dataModel, "Choose an existing Stack:", "Create a new Stack:",
                new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof StackSummary) {
                            StackSummary stack = (StackSummary) element;
                            return stack.getStackName();
                        }
                        return super.getText(element);
                    }
                },
                new ArrayList<IValidator>() {
                    private static final long serialVersionUID = 1L;
                {
                    add(stackNotExistsValidator);
                    add(new StackNameValidator());
                }});
    }

    @Override
    protected void onRefreshInRegion(Region newRegion, String defaultResourceName) {
        if (selectComboViewer != null) {
            selectComboViewer.getComboViewer().setInput(new StackSummary[] { LOADING });
            selectComboViewer.getComboViewer().setSelection(new StructuredSelection(LOADING));
            selectComboViewer.getComboViewer().getCombo().setEnabled(false);
        }
    }

    @Override
    protected CancelableThread newLoadResourceInRegionThread(String defaultResourceName) {
        return new LoadCFStackInFunctionRegionThread(defaultResourceName);
    }

    private final class LoadCFStackInFunctionRegionThread extends CancelableThread {
        private final String defaultStack;

        LoadCFStackInFunctionRegionThread(String defaultStack) {
            this.defaultStack = defaultStack;
        }

        @Override
        public void run() {
            final List<StackSummary> stacksInRegion = CloudFormationUtils.listExistingStacks(currentRegion.getId());

            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    try {
                        synchronized (LoadCFStackInFunctionRegionThread.this) {
                            if (!isCanceled()) {
                                if (stacksInRegion.isEmpty()) {
                                    selectComboViewer.getComboViewer().setInput(new StackSummary[] {NONE_FOUND});
                                    selectComboViewer.getComboViewer().setSelection(new StructuredSelection(NONE_FOUND));
                                    selectRadioButton.getRadioButton().setEnabled(false);
                                    selectRadioButton.setValue(false);
                                    createRadioButton.setValue(true);
                                    createText.setEnabled(true);
                                } else {
                                    StackSummary defaultStack = findDefaultStack(stacksInRegion);
                                    if (defaultStack == null) {
                                        defaultStack = stacksInRegion.get(0);
                                    }
                                    selectComboViewer.getComboViewer().setInput(stacksInRegion);
                                    selectComboViewer.getComboViewer().setSelection(new StructuredSelection(defaultStack));
                                    selectRadioButton.getRadioButton().setEnabled(true);
                                    if (selectRadioButton.getRadioButton().getSelection()) {
                                        selectComboViewer.getComboViewer().getCombo().setEnabled(true);
                                    }
                                }
                                createText.setText(String.format("%s-stack-%d", dataModel.getDefaultStackNamePrefix(), System.currentTimeMillis()));
                                stackNotExistsValidator.setStacks(stacksInRegion);
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }

        private StackSummary findDefaultStack(List<StackSummary> stacks) {
            if (defaultStack == null) {
                return null;
            }
            for (StackSummary stack : stacks) {
                if (stack.getStackName().equals(defaultStack)) {
                    return stack;
                }
            }
            return null;
        }
    }

    private static final class StackNotExistsValidator implements IValidator {
        private List<StackSummary> stacks;

        public void setStacks(List<StackSummary> stacks) {
            this.stacks = stacks;
        }

        @Override
        public IStatus validate(Object value) {
            String stackName = (String) value;
            if (stacks == null || stacks.isEmpty()) {
                return ValidationStatus.ok();
            }
            for (StackSummary stack : stacks) {
                if (stack.getStackName().equals(stackName)) {
                    return ValidationStatus.error(String.format("Stack %s already exists.", stackName));
                }
            }
            return ValidationStatus.ok();
        }
    }
}
