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
package com.amazonaws.eclipse.core.ui;

import static com.amazonaws.eclipse.core.model.SelectOrInputDataModel.P_CREATE_NEW_RESOURCE;
import static com.amazonaws.eclipse.core.model.SelectOrInputDataModel.P_EXISTING_RESOURCE;
import static com.amazonaws.eclipse.core.model.SelectOrInputDataModel.P_NEW_RESOURCE_NAME;
import static com.amazonaws.eclipse.core.model.SelectOrInputDataModel.P_SELECT_EXISTING_RESOURCE;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;
import com.amazonaws.eclipse.core.widget.RadioButtonComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;

/**
 * Composite that includes a combo box for selecting an existing resource,
 * an input text field for creating a new resource, and two radio buttons
 * for selecting these two options.
 * This class is intended to be extended by a concrete class for the generic T.
 */
public abstract class SelectOrInputComposite<T, K extends SelectOrInputDataModel<T>> extends Composite {

    protected final DataBindingContext bindingContext;
    protected final K dataModel;
    protected final String selectResourceLabelValue;
    protected final String createResourceLabelValue;
    protected final ILabelProvider selectComboLabelProvider;
    protected final List<IValidator> createTextValidators;

    protected Region currentRegion;
    protected CancelableThread loadResourceInRegionThread;

    protected RadioButtonComplex selectRadioButton;
    protected RadioButtonComplex createRadioButton;
    protected ComboViewerComplex<T> selectComboViewer;
    protected TextComplex createText;

    protected SelectOrInputComposite(
            Composite parent,
            DataBindingContext bindingContext,
            K dataModel,
            String selectResourceLabelValue,
            String createResourceLabelValue,
            ILabelProvider selectComboLabelProvider,
            List<IValidator> createTextValidators) {
        super(parent, SWT.NONE);
        this.bindingContext = bindingContext;
        this.dataModel = dataModel;
        this.selectResourceLabelValue = selectResourceLabelValue;
        this.createResourceLabelValue = createResourceLabelValue;
        this.selectComboLabelProvider = selectComboLabelProvider;
        this.createTextValidators = createTextValidators;
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        createControls();
    }

    /**
     * Refresh widget when changing region with the default resource.
     */
    public void refreshInRegion(Region newRegion, String defaultResourceName) {
        this.currentRegion = newRegion;
        onRefreshInRegion(newRegion, defaultResourceName);
        CancelableThread.cancelThread(loadResourceInRegionThread);
        loadResourceInRegionThread = newLoadResourceInRegionThread(defaultResourceName);
        loadResourceInRegionThread.start();
    }

    // Subclass must implement this method for showing the combo box with only one fake item that
    // indicates the resources in the new region are being loaded
    protected abstract void onRefreshInRegion(Region newRegion, String defaultResourceName);

    // Subclass must implement this method for returning a concrete CancelableThread that loads
    // AWS Resources and show the default resource if exists.
    protected abstract CancelableThread newLoadResourceInRegionThread(String defaultResourceName);

    @SuppressWarnings("unchecked")
    private void createControls() {
        this.selectRadioButton = RadioButtonComplex.builder()
                .composite(this)
                .dataBindingContext(bindingContext)
                .defaultValue(dataModel.isSelectExistingResource())
                .labelValue(selectResourceLabelValue)
                .pojoObservableValue(PojoProperties.value(P_SELECT_EXISTING_RESOURCE, Boolean.class)
                        .observe(dataModel))
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onSelectRadioButtonSelected();
                    }
                })
                .build();

        this.selectComboViewer = ComboViewerComplex.<T>builder()
                .composite(this)
                .bindingContext(bindingContext)
                .labelProvider(selectComboLabelProvider)
                .pojoObservableValue(PojoProperties.value(P_EXISTING_RESOURCE)
                        .observe(dataModel))
                .build();

        this.createRadioButton = RadioButtonComplex.builder()
                .composite(this)
                .dataBindingContext(bindingContext)
                .defaultValue(dataModel.isCreateNewResource())
                .labelValue(createResourceLabelValue)
                .pojoObservableValue(PojoProperties.value(P_CREATE_NEW_RESOURCE, Boolean.class)
                        .observe(dataModel))
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onCreateRadioButtonSelected();
                    }
                })
                .build();

        this.createText = TextComplex.builder()
                .composite(this)
                .createLabel(false)
                .dataBindingContext(bindingContext)
                .defaultValue("NewResource")
                .pojoObservableValue(PojoProperties.value(P_NEW_RESOURCE_NAME, String.class)
                        .observe(dataModel))
                .addValidators(createTextValidators)
                .build();
    }

    private void onSelectRadioButtonSelected() {
        this.selectComboViewer.getComboViewer().getCombo().setEnabled(true);
        this.createText.setEnabled(false);
    }

    private void onCreateRadioButtonSelected() {
        this.selectComboViewer.getComboViewer().getCombo().setEnabled(false);
        this.createText.setEnabled(true);
    }

}
