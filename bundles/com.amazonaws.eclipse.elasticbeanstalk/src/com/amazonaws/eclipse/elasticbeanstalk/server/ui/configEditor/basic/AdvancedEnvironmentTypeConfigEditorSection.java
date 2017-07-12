/*
 * Copyright 2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.basic;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.AbstractEnvironmentConfigEditorPart;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigEditorSection;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Environment config editor section in advanced config editor for setting.
 * "Environment Type" property, corresponding to the "aws:elasticbeanstalk:environment" namespace.
 */
public class AdvancedEnvironmentTypeConfigEditorSection extends EnvironmentConfigEditorSection {

    public AdvancedEnvironmentTypeConfigEditorSection(AbstractEnvironmentConfigEditorPart parentEditor, EnvironmentConfigDataModel model,
            Environment environment, DataBindingContext bindingContext, String namespace, List<ConfigurationOptionDescription> options) {
        super(parentEditor, model, environment, bindingContext, namespace, options);

    }

    /**
     * Create a read only label instead of combo
     */
    @Override
    protected void createCombo(Composite parent, ConfigurationOptionDescription option) {
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        final Label typeLabel = new Label(parent, SWT.READ_ONLY);
        IObservableValue modelv = model.observeEntry(option);
        ISWTObservableValue widget = SWTObservables.observeText(typeLabel);
        parentEditor.getDataBindingContext().bindValue(widget, modelv,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));

    }

}
