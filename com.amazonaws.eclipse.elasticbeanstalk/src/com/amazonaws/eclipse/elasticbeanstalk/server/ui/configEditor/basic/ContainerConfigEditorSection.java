/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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

import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;

/**
 * Human readable editor for container / jvm options
 */
public class ContainerConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<String, String>();
    static {
        humanReadableNames.put("Xms", "Initial JVM Heap Size (-Xms argument)");
        humanReadableNames.put("Xmx", "Maximum JVM Heap Size (-Xmx argument)");
        humanReadableNames.put("XX:MaxPermSize", "Initial JVM Heap Size (-XX:MaxPermSize argument)");
        humanReadableNames.put("Xmx", "Initial JVM Heap Size (-Xmx argument)");
        humanReadableNames.put("JVM Options", "Additional Tomcat JVM command line options");
        humanReadableNames.put("LogPublicationControl", "Enable log file rotation to Amazon S3");
    }

    private static final String[] fieldOrder = new String[] { "Xmx", "Xmx", "XX:MaxPermSize", "JVM Options",
            "LogPublicationControl" };

    public ContainerConfigEditorSection(BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model, DataBindingContext bindingContext) {
        super(basicEnvironmentConfigurationEditorPart, model, bindingContext);
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
        return "Container / JVM Options";
    }

    @Override
    protected String getSectionDescription() {
        return "These settings control command-line options for " + "your container and the underlying JVM.";
    }

}
