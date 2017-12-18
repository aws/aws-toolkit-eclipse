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

import static com.amazonaws.util.ValidationUtils.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.RegionDataModel;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;

/**
 * A reusable composite for Region selection.
 */
public class RegionComposite extends Composite {
    private static final Region DEFAULT_REGION = RegionUtils.getRegion("us-east-1");
    private final DataBindingContext bindingContext;
    private final RegionDataModel dataModel;
    private final String serviceName;
    private final String labelValue;
    private final List<ISelectionChangedListener> listeners;

    private ComboViewerComplex<Region> regionComboComplex;

    private RegionComposite(
            Composite parent,
            DataBindingContext bindingContext,
            RegionDataModel dataModel,
            String serviceName,
            String labelValue,
            List<ISelectionChangedListener> listeners) {
        super(parent, SWT.NONE);
        this.bindingContext = bindingContext;
        this.dataModel = dataModel;
        this.serviceName = serviceName;
        this.labelValue = labelValue;
        this.listeners = listeners;
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        this.setLayout(new GridLayout(3, false));
        createControl();
    }

    public Region getCurrentSelectedRegion() {
        IStructuredSelection selection = (IStructuredSelection) regionComboComplex.getComboViewer().getSelection();
        return (Region) selection.getFirstElement();
    }

    private void createControl() {
        List<Region> regions = serviceName == null ? RegionUtils.getRegions()
                : RegionUtils.getRegionsForService(serviceName);
        regionComboComplex = ComboViewerComplex.<Region>builder()
                .composite(this)
                .bindingContext(bindingContext)
                .labelValue(labelValue)
                .items(regions)
                .defaultItem(Optional.ofNullable(dataModel.getRegion()).orElse(DEFAULT_REGION))
                .addListeners(listeners)
                .pojoObservableValue(PojoProperties.value(RegionDataModel.class, RegionDataModel.P_REGION, Region.class).observe(dataModel))
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof Region) {
                            Region region = (Region) element;
                            return region.getName();
                        }
                        return super.getText(element);
                    }
                })
                .comboSpan(2)
                .build();
    }

    public void selectAwsRegion(Region region) {
        regionComboComplex.getComboViewer().setSelection(new StructuredSelection(Optional.ofNullable(region).orElse(DEFAULT_REGION)));
    }

    public static RegionCompositeBuilder builder() {
        return new RegionCompositeBuilder();
    }

    public static final class RegionCompositeBuilder {
        private Composite parent;
        private DataBindingContext bindingContext;
        private RegionDataModel dataModel;
        private String serviceName;
        private String labelValue = "Select region:";
        private final List<ISelectionChangedListener> listeners = new ArrayList<>();

        public RegionComposite build() {
            validateParameters();
            return new RegionComposite(parent, bindingContext, dataModel, serviceName, labelValue, listeners);
        }

        public RegionCompositeBuilder parent(Composite parent) {
            this.parent = parent;
            return this;
        }

        public RegionCompositeBuilder bindingContext(DataBindingContext bindingContext) {
            this.bindingContext = bindingContext;
            return this;
        }

        public RegionCompositeBuilder dataModel(RegionDataModel dataModel) {
            this.dataModel = dataModel;
            return this;
        }

        public RegionCompositeBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public RegionCompositeBuilder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public RegionCompositeBuilder addListener(ISelectionChangedListener listener) {
            this.listeners.add(listener);
            return this;
        }

        private void validateParameters() {
            assertNotNull(parent, "Parent composite");
            assertNotNull(bindingContext, "BindingContext");
            assertNotNull(dataModel, "RegionDataModel");
        }
    }
}
