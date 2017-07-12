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
 * Load Balancing section of the human-readable environment config editor.
 */
public class LoadBalancingConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("LoadBalancerHTTPPort", "HTTP Port");
        humanReadableNames.put("LoadBalancerHTTPSPort", "HTTPS Port");
        humanReadableNames.put("SSLCertificateId", "SSL Certificate Id");
    }

    private static final String[] fieldOrder = new String[] { "LoadBalancerHTTPPort", "LoadBalancerHTTPSPort",
            "SSLCertificateId" };

    public LoadBalancingConfigEditorSection(
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
        return "Load Balancing";
    }

    @Override
    protected String getSectionDescription() {
        return "These settings allow you to control the behavior of your environment's load balancer.";
    }
    
    @Override
    protected Section getSection(Composite parent) {
        return toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION | Section.NO_TITLE);
    }
}
