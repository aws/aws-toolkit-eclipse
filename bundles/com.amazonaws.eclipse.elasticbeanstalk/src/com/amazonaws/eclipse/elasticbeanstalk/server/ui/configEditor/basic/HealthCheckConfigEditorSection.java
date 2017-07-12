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

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;

/**
 * Environment config editor section for setting "Server" properties, roughly
 * corresponding to the aws:autoscaling:launchconfiguration" namespace.
 */
public class HealthCheckConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("HealthyThreshold", "Healthy Check Count Threshold");
        humanReadableNames.put("Interval", "Health Check Interval (seconds)");
        humanReadableNames.put("Timeout", "Health Check Timeout (seconds)");
        humanReadableNames.put("UnhealthyThreshold", "Unhealthy Check Count Threshold");
        humanReadableNames.put("Application Healthcheck URL", "Application Health Check URL");
    }

    private static final String[] fieldOrder = new String[] {
        "Application Healthcheck URL", "Interval", "Timeout", "HealthyThreshold", "UnhealthyThreshold",
    };


    public HealthCheckConfigEditorSection(
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
        return "EC2 Instance Health Check";
    }

    @Override
    protected String getSectionDescription() {
        return "These settings allow you to configure how AWS Elastic Beanstalk determines whether an EC2 instance is healthy or not.";
    }
}
