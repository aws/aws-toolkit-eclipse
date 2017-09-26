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

import static com.amazonaws.eclipse.core.model.SelectOrInputDataModel.P_EXISTING_RESOURCE;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.model.SelectOrCreateDataModel;
import com.amazonaws.eclipse.core.ui.dialogs.AbstractInputDialog;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex.AwsResourceComboViewerComplexBuilder;
import com.amazonaws.eclipse.core.widget.AwsResourceComboViewerComplex.AwsResourceUiRefreshable;
import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.s3.model.Bucket;

/**
 * A basic composite that includes a label, a combo box and a button that could be used to select
 * a resource from the combo box, or create a new one by the button.
 *
 * @param T - The underlying resource type, like {@link Bucket}. This is also the data type bound to the ComboBox.
 * @param K - The data model for this composite.
 * @param P - The resource loading scope for loading the specified AWS resource. For most of the AWS resources,
 *            the {@link AwsResourceScopeParamBase} is sufficient which includes the account id and region id. But for
 *            some resource like {@link AliasConfiguration}, additional restriction, function name in this case, is needed.
 */
public abstract class SelectOrCreateComposite<T, K extends SelectOrCreateDataModel<T, P>, P extends AbstractAwsResourceScopeParam<P>>
        extends Composite implements AwsResourceUiRefreshable<T> {
    private final DataBindingContext bindingContext;
    private final K dataModel;
    private final String selectResourceLabelValue;

    private AwsResourceComboViewerComplex<T, P> resourcesCombo;
    private Button createButton;

    public SelectOrCreateComposite(
            Composite parent,
            DataBindingContext bindingContext,
            K dataModel,
            String selectResourceLabelValue) {
        super(parent, SWT.NONE);
        this.bindingContext = bindingContext;
        this.dataModel = dataModel;
        this.selectResourceLabelValue = selectResourceLabelValue;
        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.setLayout(new GridLayout(3, false));
        createControls();
    }

    public void refreshComposite(P param, String defaultResourceName) {
        resourcesCombo.refreshResources(param, defaultResourceName);
    }

    protected abstract AbstractInputDialog<T> createResourceDialog(P param);

    private void createControls() {
        this.resourcesCombo = new AwsResourceComboViewerComplexBuilder<T, P>()
            .composite(this)
            .bindingContext(bindingContext)
            .labelValue(selectResourceLabelValue)
            .pojoObservableValue(PojoProperties.value(P_EXISTING_RESOURCE).observe(dataModel))
            .labelProvider(new LabelProvider() {
                @Override
                public String getText(Object element) {
                    try {
                        @SuppressWarnings("unchecked")
                        T resource = (T) element;
                        return dataModel.getResourceName(resource);
                    } catch (ClassCastException cce) {
                        return super.getText(element);
                    }
                }
            })
            .resourceMetadata(dataModel)
            .resourceUiRefresher(this)
            .build();
        this.resourcesCombo.getComboViewer().getCombo().setEnabled(false);

        this.createButton = WizardWidgetFactory.newPushButton(this, "Create");
        this.createButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onCreateButtonSelected();
            }
        });
    }

    private void onCreateButtonSelected() {
        AbstractInputDialog<T> dialog = createResourceDialog(resourcesCombo.getCurrentResourceScope());
        int returnCode = dialog.open();

        if (returnCode == Window.OK) {
            T newResource = dialog.getCreatedResource();
            dataModel.setCreateNewResource(true);
            resourcesCombo.cacheNewResource(resourcesCombo.getCurrentResourceScope(), newResource);

            if (isEnabled() && !resourcesCombo.getComboViewer().getCombo().isEnabled()) {
                resourcesCombo.setEnabled(true);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.resourcesCombo.setEnabled(enabled);
        this.createButton.setEnabled(enabled);
    }

    @Override
    public void refreshWhenLoadingError(List<T> resources) {
        // Disable the combo box, but not disabling the enabler validator.
        resourcesCombo.getComboViewer().getCombo().setEnabled(false);
    }

    @Override
    public void refreshWhenLoadingEmpty(List<T> resources) {
        // Disable the combo box, but not disabling the enabler validator.
        resourcesCombo.getComboViewer().getCombo().setEnabled(false);
    }

    @Override
    public void refreshWhenLoadingSuccess(List<T> resources) {
        resourcesCombo.getComboViewer().getCombo().setEnabled(isEnabled());
    }
}
