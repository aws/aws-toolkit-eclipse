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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;

/**
 * Human readable editor section to configure autoscaling parameters.
 */
public class AutoScalingConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("MinSize", "Minimum Instance Count");
        humanReadableNames.put("MaxSize", "Maximum Instance Count");
        humanReadableNames.put("Availability Zones", "Availability Zones");
        humanReadableNames.put("Cooldown", "Scaling Cooldown Time (seconds)");
    }

    private static final String[] fieldOrder = new String[] { "MinSize", "MaxSize", "Availability Zones", "Cooldown", };

    public AutoScalingConfigEditorSection(
            BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
        super(basicEnvironmentConfigurationEditorPart, model, environment, bindingContext);
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
        return "Auto Scaling";
    }

    @Override
    protected String getSectionDescription() {
        return "Auto-scaling automatically launches or terminates EC2 "
                + "instances based on defined metrics and thresholds called triggers. "
                + "Auto-scaling will also launch a new EC2 instance in the event of a "
                + "failure. These settings allow you to control auto-scaling behavior.";
    }
    
    @Override
    protected Section getSection(Composite parent) {
        Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.NO_TITLE);        
        return section;
    }
}
