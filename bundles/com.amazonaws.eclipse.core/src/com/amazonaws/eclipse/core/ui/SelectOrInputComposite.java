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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam;
import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.widget.RadioButtonComplex;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex.AwsResourceComboViewerComplexBuilder;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex.AwsResourceUiRefreshable;
import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.eclipse.core.widget.TextComplex;

/**
 * A basic composite that includes a combo box, a text field and two radio buttons that could be used to select
 * a resource from the combo box, or create a new resource with the name in the text field.
 *
 * @param T - The underlying resource type, like {@link Bucket}. This is also the data type bound to the ComboBox.
 * @param K - The data model for this composite.
 * @param P - The resource loading scope for loading the specified AWS resource. For most of the AWS resources,
 *            the {@link AwsResourceScopeParamBase} is sufficient which includes the account id and region id. But for
 *            some resource like {@link AliasConfiguration}, additional restriction, function name in this case, is needed.
 */
public abstract class SelectOrInputComposite<T, K extends SelectOrInputDataModel<T, P>, P extends AbstractAwsResourceScopeParam<P>>
        extends Composite implements AwsResourceUiRefreshable<T> {

    private final DataBindingContext bindingContext;
    private final K dataModel;
    private final String selectResourceLabelValue;
    private final String createResourceLabelValue;
    private final List<IValidator> createTextValidators = new ArrayList<>();

    private RadioButtonComplex selectRadioButton;
    private RadioButtonComplex createRadioButton;
    private AwsResourceComboViewerComplex<T, P> resourcesCombo;
    private TextComplex createText;

    private final NewResourceDoesNotExistValidator resourceNotExistsValidator = new NewResourceDoesNotExistValidator();

    protected SelectOrInputComposite(
            Composite parent,
            DataBindingContext bindingContext,
            K dataModel,
            String selectResourceLabelValue,
            String createResourceLabelValue,
            List<IValidator> createTextValidators) {
        super(parent, SWT.NONE);
        this.bindingContext = bindingContext;
        this.dataModel = dataModel;
        this.selectResourceLabelValue = selectResourceLabelValue;
        this.createResourceLabelValue = createResourceLabelValue;
        this.createTextValidators.add(resourceNotExistsValidator);
        this.createTextValidators.addAll(createTextValidators);
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        createControls();
    }

    /**
     * Refresh composite when changing any one of the parameters in the {@link AbstractAwsResourceScopeParam}
     */
    public void refreshComposite(P param, String defaultResourceName) {
        resourcesCombo.refreshResources(param, defaultResourceName);
    }

    @Override
    public void refreshWhenLoadingError(List<T> resources) {
        selectRadioButton.setValue(false);
        createRadioButton.setValue(true);
        createText.setEnabled(isEnabled());
        resourcesCombo.setEnabled(false);
        updateCreateTextValidator(resources);
    }

    @Override
    public void refreshWhenLoadingEmpty(List<T> resources) {
        selectRadioButton.setValue(false);
        createRadioButton.setValue(true);
        createText.setEnabled(isEnabled());
        resourcesCombo.setEnabled(false);
        updateCreateTextValidator(resources);
    }

    @Override
    public void refreshWhenLoadingSuccess(List<T> resources) {
        createText.setEnabled(createRadioButton.getRadioButton().getSelection() && isEnabled());
        resourcesCombo.setEnabled(selectRadioButton.getRadioButton().getSelection() && isEnabled());
        updateCreateTextValidator(resources);
    }

    private void updateCreateTextValidator(List<T> resources) {
        resourceNotExistsValidator.setResources(resources);
        // A hacky way to set a random value to explicitly trigger the validation.
        createText.setText(UUID.randomUUID().toString());
        createText.setText(dataModel.getDefaultResourceName());
    }

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

        this.resourcesCombo = new AwsResourceComboViewerComplexBuilder<T, P>()
                .composite(this)
                .bindingContext(bindingContext)
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        try {
                            @SuppressWarnings("unchecked")
                            T resource = (T) element;
                            return dataModel.getResourceName(resource);
                        } catch (ClassCastException cce) {
                            // Do nothing
                        }
                        return super.getText(element);
                    }
                })
                .pojoObservableValue(PojoProperties.value(P_EXISTING_RESOURCE)
                        .observe(dataModel))
                .resourceMetadata(dataModel)
                .resourceUiRefresher(this)
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

        this.createText = TextComplex.builder(this, bindingContext, PojoProperties.value(P_NEW_RESOURCE_NAME).observe(dataModel))
                .createLabel(false)
                .addValidators(createTextValidators)
                .build();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.selectRadioButton.getRadioButton().setEnabled(enabled);
        this.createRadioButton.getRadioButton().setEnabled(enabled);
        if (enabled) {
            onSelectRadioButtonSelected();
            onCreateRadioButtonSelected();
        } else {
            this.resourcesCombo.setEnabled(false);
            this.createText.setEnabled(false);
        }
    }

    private void onSelectRadioButtonSelected() {
        boolean selected = selectRadioButton.getRadioButton().getSelection();
        this.resourcesCombo.setEnabled(isEnabled() && selected);
        this.createText.setEnabled(isEnabled() && !selected);
    }

    private void onCreateRadioButtonSelected() {
        boolean selected = createRadioButton.getRadioButton().getSelection();
        this.resourcesCombo.setEnabled(isEnabled() && !selected);
        this.createText.setEnabled(isEnabled() && selected);
    }

    private final class NewResourceDoesNotExistValidator implements IValidator {
        private List<T> resources;

        public void setResources(List<T> resources) {
            this.resources = resources;
        }

        @Override
        public IStatus validate(Object value) {
            String resourceName = (String) value;
            T resource = dataModel.findResourceByName(resources, resourceName);
            return resource == null ? ValidationStatus.ok()
                    : ValidationStatus.error(String.format("%s %s already exists.", dataModel.getResourceType(), resourceName));
        }
    }
}