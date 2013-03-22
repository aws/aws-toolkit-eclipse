/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateRequest;

/**
 * Job to export an environment configuration to a template
 */
public final class ExportConfigurationJob extends Job {

    private final Environment environment;
    private final String templateDescription;
    private final Collection<ConfigurationOptionSetting> createConfigurationOptions;
    private final String templateName;
    private final boolean isCreatingNew;

    /**
     * @param environment
     *            The server environment
     * @param templateName
     *            The name of the template
     * @param templateDescription
     *            An optional description of the template
     * @param createConfigurationOptions
     *            The full set of options that define the template
     * @param isCreatingNew
     *            Whether to create a new template or update an existing one
     */
    public ExportConfigurationJob(Environment environment, String templateName, String templateDescription,
            Collection<ConfigurationOptionSetting> createConfigurationOptions, boolean isCreatingNew) {
        super("Exporting configuration template");
        this.environment = environment;
        this.templateDescription = templateDescription;
        this.createConfigurationOptions = createConfigurationOptions;
        this.templateName = templateName;
        this.isCreatingNew = isCreatingNew;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                    .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
            if ( isCreatingNew ) {
                client.createConfigurationTemplate(new CreateConfigurationTemplateRequest()
                        .withApplicationName(environment.getApplicationName()).withDescription(templateDescription)
                        .withTemplateName(templateName).withOptionSettings(createConfigurationOptions));
            } else {
                client.updateConfigurationTemplate(new UpdateConfigurationTemplateRequest()
                        .withApplicationName(environment.getApplicationName()).withDescription(templateDescription)
                        .withTemplateName(templateName).withOptionSettings(createConfigurationOptions));
            }
        } catch ( Exception e ) {
            return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }
}
