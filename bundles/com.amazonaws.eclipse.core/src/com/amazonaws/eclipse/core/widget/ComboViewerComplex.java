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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newComboViewer;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;
import static com.amazonaws.util.ValidationUtils.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.databinding.ChainValidator;

/**
 * A JFace ComboView widget along with a Label and data binding feature. Instead of
 * observing the value of the combo box item, it observes the attached data directly
 * and binds it to the model.
 */
public class ComboViewerComplex<T> {
    private final ComboViewer comboViewer;
    private final IObservableValue enabler = new WritableValue();

    protected ComboViewerComplex(
            Composite parent,
            ILabelProvider labelProvider,
            Collection<T> items,
            T defaultItem,
            DataBindingContext bindingContext,
            IObservableValue pojoObservableValue,
            List<IValidator> validators,
            String labelValue,
            int comboSpan,
            List<ISelectionChangedListener> listeners) {

        if (labelValue != null) {
            newLabel(parent, labelValue);
        }
        comboViewer = newComboViewer(parent, comboSpan);
        comboViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboViewer.setLabelProvider(labelProvider);
        comboViewer.setInput(items);

        IViewerObservableValue viewerObservableValue = ViewerProperties.singleSelection().observe(comboViewer);
        Binding binding = bindingContext.bindValue(viewerObservableValue, pojoObservableValue);

        enabler.setValue(true);
        ChainValidator<T> validatorChain = new ChainValidator<>(viewerObservableValue, enabler, validators);
        bindingContext.addValidationStatusProvider(validatorChain);

        if (defaultItem != null && items.contains(defaultItem)) {
            comboViewer.setSelection(new StructuredSelection(defaultItem));
        } else if (!items.isEmpty()) {
            comboViewer.setSelection(new StructuredSelection(items.iterator().next()));
        }

        for (ISelectionChangedListener listener : listeners) {
            comboViewer.addSelectionChangedListener(listener);
        }
        comboViewer.getCombo().addDisposeListener(e -> {
            bindingContext.removeBinding(binding);
            bindingContext.removeValidationStatusProvider(validatorChain);
            viewerObservableValue.dispose();
            validatorChain.dispose();
        });
    }

    public ComboViewer getComboViewer() {
        return this.comboViewer;
    }

    public void setEnabled(boolean enabled) {
        comboViewer.getCombo().setEnabled(enabled);
        enabler.setValue(enabled);
    }

    public void selectItem(T item) {
        Collection<T> items = (Collection<T>) comboViewer.getInput();
        if (item != null && items.contains(item)) {
            comboViewer.setSelection(new StructuredSelection(item));
        }
    }

    public static <T> ComboViewerComplexBuilder<T> builder() {
        return new ComboViewerComplexBuilder<>();
    }

    public static class ComboViewerComplexBuilder<T> extends ComboViewerComplexBuilderBase<T, ComboViewerComplex<T>, ComboViewerComplexBuilder<T>>{
        @Override
        protected ComboViewerComplex<T> newType() {
            return new ComboViewerComplex<>(
                    parent, labelProvider, items, defaultItem,
                    bindingContext, pojoObservableValue, validators,
                    labelValue, comboSpan, listeners);
        }
    }

    public static abstract class ComboViewerComplexBuilderBase<T, TypeToBuild extends ComboViewerComplex<T>, TypeBuilder extends ComboViewerComplexBuilderBase<T, TypeToBuild, TypeBuilder>> {
        protected Composite parent;
        protected ILabelProvider labelProvider;
        protected Collection<T> items = new ArrayList<>();
        protected T defaultItem;
        protected DataBindingContext bindingContext;
        protected IObservableValue pojoObservableValue;
        protected List<IValidator> validators = new ArrayList<>();
        protected String labelValue;
        protected int comboSpan = 1;
        protected List<ISelectionChangedListener> listeners = new ArrayList<>();

        protected abstract TypeToBuild newType();

        public TypeToBuild build() {
            validateParameters();
            return newType();
        }

        public TypeBuilder composite(Composite parent) {
            this.parent = parent;
            return getBuilder();
        }

        public TypeBuilder labelProvider(ILabelProvider labelProvider) {
            this.labelProvider = labelProvider;
            return getBuilder();
        }

        public TypeBuilder items(Collection<T> items) {
            this.items.addAll(items);
            return getBuilder();
        }

        public TypeBuilder defaultItem(T defaultItme) {
            this.defaultItem = defaultItme;
            return getBuilder();
        }

        public TypeBuilder bindingContext(DataBindingContext bindingContext) {
            this.bindingContext = bindingContext;
            return getBuilder();
        }

        public TypeBuilder addValidators(IValidator... validators) {
            this.validators.addAll(Arrays.asList(validators));
            return getBuilder();
        }

        public TypeBuilder pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return getBuilder();
        }

        public TypeBuilder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return getBuilder();
        }

        public TypeBuilder comboSpan(int comboSpan) {
            this.comboSpan = comboSpan;
            return getBuilder();
        }

        public TypeBuilder addListeners(ISelectionChangedListener... listeners) {
            return addListeners(Arrays.asList(listeners));
        }

        public TypeBuilder addListeners(List<ISelectionChangedListener> listeners) {
            this.listeners.addAll(listeners);
            return getBuilder();
        }

        @SuppressWarnings("unchecked")
        private TypeBuilder getBuilder() {
            return (TypeBuilder)this;
        }

        protected void validateParameters() {
            assertNotNull(parent, "Parent composite");
            assertNotNull(labelProvider, "LabelProvider");
            assertNotNull(pojoObservableValue, "PojoObservableValue");
        }
    }
}
