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
package com.amazonaws.eclipse.core.widget;

import static com.amazonaws.util.ValidationUtils.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam;
import com.amazonaws.eclipse.core.model.AwsResourceMetadata;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.validator.ResourcesLoadingValidator;

/**
 * A Combo viewer complex for showing AWS resources.
 *
 * @param T - The AWS Resource type, it is also the Combo Box data type.
 * @param P - The AWS Resource scope for loading the AWS resources of type T.
 */
public class AwsResourceComboViewerComplex<T, P extends AbstractAwsResourceScopeParam<P>>
        extends ComboViewerComplex<T> {
    private CompletableFuture<List<T>> loadResourcesJob;
    private P resourceScopeParam;
    private String defaultResourceName;
    private Map<P, List<T>> cache = new ConcurrentHashMap<>();

    private final AwsResourceMetadata<T, P> dataModel;
    private final ResourcesLoadingValidator<T, P> resourceLoadingValidator;
    private final AwsResourceUiRefreshable<T> refresher;

    protected AwsResourceComboViewerComplex(Composite parent, ILabelProvider labelProvider, Collection<T> items,
            T defaultItem, DataBindingContext bindingContext, IObservableValue pojoObservableValue,
            List<IValidator> validators, String labelValue, int comboSpan, List<ISelectionChangedListener> listeners,
            AwsResourceMetadata<T, P> dataModel, ResourcesLoadingValidator<T, P> resourceLoadingValidator, AwsResourceUiRefreshable<T> refresher) {
        super(parent, labelProvider, items, defaultItem, bindingContext, pojoObservableValue, validators, labelValue, comboSpan, listeners);
        this.dataModel = dataModel;
        this.resourceLoadingValidator = resourceLoadingValidator;
        this.refresher = refresher;
    }

    public void refreshResources(P param, String defaultResourceName) {
        this.resourceScopeParam = param;
        this.defaultResourceName = defaultResourceName;

        resourceLoadingValidator.setRegion(RegionUtils.getRegion(param.getRegionId()));

        getComboViewer().setInput(Collections.singletonList(dataModel.getLoadingItem()));
        getComboViewer().setSelection(new StructuredSelection(dataModel.getLoadingItem()));
        getComboViewer().getCombo().setEnabled(false);

        // Cancel previous resources loading job in favor of the new one.
        if (loadResourcesJob != null && !loadResourcesJob.isDone()) {
            loadResourcesJob.cancel(true);
        }

        loadResourcesJob = CompletableFuture.supplyAsync(this::getAwsResources);
        loadResourcesJob.exceptionally(ex -> {
                resourceLoadingValidator.setErrorMessage(ex.getMessage());
                return null;
            })
            .thenAccept(this::refreshUI);
    }

    private List<T> getAwsResources() {
        return cache.computeIfAbsent(resourceScopeParam, param -> dataModel.loadAwsResources(param.copy()));
    }

    private void refreshUI(List<T> resources) {
        Display.getDefault().syncExec(() -> {
            // Error occurs
            if (resources == null) {
                getComboViewer().setInput(Collections.singletonList(dataModel.getErrorItem()));
                getComboViewer().setSelection(new StructuredSelection(dataModel.getErrorItem()));
                refresher.refreshWhenLoadingError(resources);
            } else if (resources.isEmpty()) {
                getComboViewer().setInput(Collections.singletonList(dataModel.getNotFoundItem()));
                getComboViewer().setSelection(new StructuredSelection(dataModel.getNotFoundItem()));
                refresher.refreshWhenLoadingEmpty(resources);
            } else {
                T defaultResource = dataModel.findResourceByName(resources, defaultResourceName);
                if (defaultResource == null) {
                  defaultResource = resources.get(0);
                }
                getComboViewer().setInput(resources);
                getComboViewer().setSelection(new StructuredSelection(defaultResource));
                refresher.refreshWhenLoadingSuccess(resources);
            }
        });
    }

    public P getCurrentResourceScope() {
        return this.resourceScopeParam;
    }

    public void cacheNewResource(P param, T newResource) {
        cache.putIfAbsent(param, new ArrayList<>()).add(newResource);

        if (this.loadResourcesJob == null || this.loadResourcesJob.isDone()) {
            getComboViewer().setInput(cache.get(param));
            getComboViewer().setSelection(new StructuredSelection(newResource));
        } else {
            loadResourcesJob.cancel(true);
            getComboViewer().setInput(Arrays.asList(newResource));
            getComboViewer().setSelection(new StructuredSelection(newResource));
        }
    }

    public static final class AwsResourceComboViewerComplexBuilder<T, P extends AbstractAwsResourceScopeParam<P>>
            extends ComboViewerComplexBuilderBase<T, AwsResourceComboViewerComplex<T, P>, AwsResourceComboViewerComplexBuilder<T, P>> {
        private AwsResourceMetadata<T, P> resourceMetadata;
        private ResourcesLoadingValidator<T, P> resourceLoadingValidator;
        private AwsResourceUiRefreshable<T> resourceRefresher;

        @Override
        protected AwsResourceComboViewerComplex<T, P> newType() {
            resourceLoadingValidator = new ResourcesLoadingValidator<>(resourceMetadata);
            validators.add(resourceLoadingValidator);
            return new AwsResourceComboViewerComplex<>(
                    parent, labelProvider, items, defaultItem,
                    bindingContext, pojoObservableValue, validators,
                    labelValue, comboSpan, listeners, resourceMetadata, resourceLoadingValidator, resourceRefresher);
        }

        public AwsResourceComboViewerComplexBuilder<T, P> resourceMetadata(AwsResourceMetadata<T, P> resourceMetadata) {
            this.resourceMetadata = resourceMetadata;
            return this;
        }

        public AwsResourceComboViewerComplexBuilder<T, P> resourceUiRefresher(AwsResourceUiRefreshable<T> refresher) {
            this.resourceRefresher = refresher;
            return this;
        }

        @Override
        protected void validateParameters() {
            super.validateParameters();
            assertNotNull(resourceMetadata, "Data model");
            assertNotNull(resourceRefresher, "Resources refresher");
        }
    }

    // Refresh UI when resources are updated.
    public static interface AwsResourceUiRefreshable<T> {
        void refreshWhenLoadingError(List<T> resources);
        void refreshWhenLoadingEmpty(List<T> resources);
        void refreshWhenLoadingSuccess(List<T> resources);
    }
}
