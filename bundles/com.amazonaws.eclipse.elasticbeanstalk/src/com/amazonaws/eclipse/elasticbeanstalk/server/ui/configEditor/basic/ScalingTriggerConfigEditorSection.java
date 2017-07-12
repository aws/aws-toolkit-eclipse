/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;

/**
 * Human readable editor section for scaling trigger configuration
 */
public class ScalingTriggerConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("MeasureName", "Trigger Measurement");
        humanReadableNames.put("Statistic", "Trigger Statistic");
        humanReadableNames.put("Unit", "Unit of Measurement");
        humanReadableNames.put("Period", "Measurement Period (minutes)");
        humanReadableNames.put("BreachDuration", "Breach Duration (minutes)");
        humanReadableNames.put("UpperThreshold", "Upper Threshold");
        humanReadableNames.put("UpperBreachScaleIncrement", "Scale-up Increment");
        humanReadableNames.put("LowerThreshold", "Lower Threshold");
        humanReadableNames.put("LowerBreachScaleIncrement", "Scale-down Increment");
    }

    private static final String[] fieldOrder = new String[] { "MeasureName", "Statistic", "Unit", "Period",
            "BreachDuration", "UpperThreshold", "UpperBreachScaleIncrement", "LowerThreshold",
            "LowerBreachScaleIncrement" };

    public ScalingTriggerConfigEditorSection(
            BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
        super(basicEnvironmentConfigurationEditorPart, model, environment, bindingContext);
    }

    @Override
    protected Section getSection(Composite parent) {
        Section section = toolkit.createSection(parent, SWT.NONE);
        return section;
    }

    @Override
    protected Map<String, String> getHumanReadableNames() {
        return humanReadableNames;
    }

    @Override
    protected String[] getFieldOrder() {
        return fieldOrder;
    }

    @Override
    protected String getSectionName() {
        return "Scaling Trigger";
    }

    @Override
    protected String getSectionDescription() {
        return "";
    }
}