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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

/**
 * Environment config editor section in basic config editor for setting
 * "Environment Type" property, corresponding to the "aws:elasticbeanstalk:environment" namespace.
 */
public class EnvironmentTypeConfigEditorSection extends HumanReadableConfigEditorSection {

    private final String CHANGE_ENVIRONMENT_TYPE_WARNING = "Are you sure to change the environment type? "
            + "This change will cause your environment to restart.  "
            + "and could be unavailable for several minutes.";

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("EnvironmentType", "Environment Type");
    }

    private static final String[] fieldOrder = new String[] { "EnvironmentType", };

    public EnvironmentTypeConfigEditorSection(BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model,
            Environment environment, DataBindingContext bindingContext) {
        super(basicEnvironmentConfigurationEditorPart, model, environment, bindingContext);
    }

    @Override
    protected Map<String, String> getHumanReadableNames() {
        return humanReadableNames;
    }


    @Override
    protected void createCombo(Composite parent, ConfigurationOptionDescription option) {
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        final Combo combo = new Combo(parent, SWT.READ_ONLY);
        combo.setItems(option.getValueOptions().toArray(new String[option.getValueOptions().size()]));
        IObservableValue modelv = model.observeEntry(option);
        ISWTObservableValue widget = SWTObservables.observeSelection(combo);
        parentEditor.getDataBindingContext().bindValue(widget, modelv, new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));
        final String oldEnvironmentType = (String)modelv.getValue();

        // After you do the confirmation, we will update the environment and refresh the layout.
        combo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean confirmation = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Change Environment Type",
                        CHANGE_ENVIRONMENT_TYPE_WARNING);
                if (confirmation == true) {
                    parentEditor.destroyOldLayouts();
                    UpdateEnvironmentRequest rq = generateUpdateEnvironmentTypeRequest();
                    if (rq != null) {
                        UpdateEnvironmentAndRefreshLayoutJob job = new UpdateEnvironmentAndRefreshLayoutJob(environment, rq);
                        job.schedule();
                    }
                } else {
                    combo.setText(oldEnvironmentType);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }

        });
    }

    @Override
    protected String[] getFieldOrder() {
        return fieldOrder;
    }

    @Override
    protected String getSectionName() {
        return "Environment Type";
    }

    @Override
    protected String getSectionDescription() {
        return "Select an environment type, either load balanced and auto scaled or single instance. "
                  + "A load-balanced, auto-scaled environment automatically distributes traffic across "
                  + "multiple Amazon EC2 instances and can stop and start instances based on demand. "
                  + "A single-instance environment includes just a single Amazon EC2 instance, which costs less.";
    }

    @Override
    protected Section getSection(Composite parent) {
        return toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION | Section.NO_TITLE);
    }

    private UpdateEnvironmentRequest generateUpdateEnvironmentTypeRequest() {
        UpdateEnvironmentRequest rq = null;
        Collection<ConfigurationOptionSetting> settings = model.createConfigurationOptions();
        for (ConfigurationOptionSetting setting : settings) {
            if (setting.getNamespace().equals(ConfigurationOptionConstants.ENVIRONMENT_TYPE) && setting.getOptionName().equals("EnvironmentType")) {
                rq = new UpdateEnvironmentRequest();
                rq.setEnvironmentName(environment.getEnvironmentName());
                rq.setOptionSettings(Arrays.asList(setting));
                return rq;
            }
        }
        return null;
    }

    public class UpdateEnvironmentAndRefreshLayoutJob extends Job {

        private Environment environment;
        private UpdateEnvironmentRequest request;

        /**
         * @param name
         */
        public UpdateEnvironmentAndRefreshLayoutJob(Environment environment, UpdateEnvironmentRequest request) {
            super("Updating environment " + request.getEnvironmentName());
            this.environment = environment;
            this.request = request;
        }


        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId()).getElasticBeanstalkClientByEndpoint(
                        environment.getRegionEndpoint());
                client.updateEnvironment(request);
            } catch (Exception e) {
                return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage(), e);
            } finally {
                // Guarantee the layout get redrawn.
                parentEditor.refresh(null);
            }
            return Status.OK_STATUS;
        }

    }

}
