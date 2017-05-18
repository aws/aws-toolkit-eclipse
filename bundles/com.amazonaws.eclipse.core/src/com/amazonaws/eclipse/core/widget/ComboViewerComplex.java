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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
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

    private ComboViewerComplex(
            Composite parent,
            ILabelProvider labelProvider,
            Collection<T> items,
            T defaultItem,
            DataBindingContext bindingContext,
            IObservableValue pojoObservableValue,
            IValidator validator,
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
        bindingContext.bindValue(viewerObservableValue, pojoObservableValue);

        if (validator != null) {
            ChainValidator<T> comboViewerValidationStatusProvider = new ChainValidator<>(viewerObservableValue, validator);
            bindingContext.addValidationStatusProvider(comboViewerValidationStatusProvider);
        }

        if (defaultItem != null) {
            comboViewer.setSelection(new StructuredSelection(defaultItem));
        }

        if (listeners != null && !listeners.isEmpty()) {
            for (ISelectionChangedListener listener : listeners) {
                comboViewer.addSelectionChangedListener(listener);
            }
        }
    }

    public ComboViewer getComboViewer() {
        return this.comboViewer;
    }

    public static <T> ComboViewerComplexBuilder<T> builder() {
        return new ComboViewerComplexBuilder<T>();
    }

    public static final class ComboViewerComplexBuilder<T> {
        private Composite parent;
        private ILabelProvider labelProvider;
        private Collection<T> items = Collections.emptyList();
        private T defaultItem;
        private DataBindingContext bindingContext;
        private IObservableValue pojoObservableValue;
        private IValidator validator;
        private String labelValue;
        private int comboSpan = 1;
        private List<ISelectionChangedListener> listeners;

        public ComboViewerComplex<T> build() {
            validateParameters();
            return new ComboViewerComplex<T>(
                    parent, labelProvider, items, defaultItem,
                    bindingContext, pojoObservableValue, validator,
                    labelValue, comboSpan, listeners);
        }

        public ComboViewerComplexBuilder<T> composite(Composite parent) {
            this.parent = parent;
            return this;
        }

        public ComboViewerComplexBuilder<T> labelProvider(ILabelProvider labelProvider) {
            this.labelProvider = labelProvider;
            return this;
        }

        public ComboViewerComplexBuilder<T> items(Collection<T> items) {
            this.items = items;
            return this;
        }

        public ComboViewerComplexBuilder<T> defaultItem(T defaultItme) {
            this.defaultItem = defaultItme;
            return this;
        }

        public ComboViewerComplexBuilder<T> bindingContext(DataBindingContext bindingContext) {
            this.bindingContext = bindingContext;
            return this;
        }

        public ComboViewerComplexBuilder<T> validator(IValidator validator) {
            this.validator = validator;
            return this;
        }

        public ComboViewerComplexBuilder<T> pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return this;
        }

        public ComboViewerComplexBuilder<T> labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public ComboViewerComplexBuilder<T> comboSpan(int comboSpan) {
            this.comboSpan = comboSpan;
            return this;
        }

        public ComboViewerComplexBuilder<T> listeners(List<ISelectionChangedListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        private void validateParameters() {
            assertNotNull(parent, "Parent composite");
            assertNotNull(labelProvider, "LabelProvider");
            assertNotNull(pojoObservableValue, "PojoObservableValue");
            assertNotNull(items, "Item collection");
        }
    }
}
