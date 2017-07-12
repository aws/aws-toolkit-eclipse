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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Human readable config editor section for environment variables.
 */
public class EnvironmentPropertiesConfigEditorSection extends HumanReadableConfigEditorSection {

    public EnvironmentPropertiesConfigEditorSection(
            BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
        super(basicEnvironmentConfigurationEditorPart, model, environment, bindingContext);
    }

    @Override
    protected Map<String, String> getHumanReadableNames() {
        return new HashMap<>();
    }

    @Override
    protected String[] getFieldOrder() {
        List<String> optionNames = new ArrayList<>();
        for (ConfigurationOptionDescription o : options) {
            optionNames.add(o.getName());
        }
        return optionNames.toArray(new String[optionNames.size()]);
    }

    @Override
    protected String getSectionName() {
        return "Environment Properties";
    }

    @Override
    protected String getSectionDescription() {
        return "These properties are passed to your application as Java system properties.";
    }

}
