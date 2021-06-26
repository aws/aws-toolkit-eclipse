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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.wst.server.ui.internal.ImageResource;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.ExportConfigurationJob;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.UpdateEnvironmentConfigurationJob;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.basic.AdvancedEnvironmentTypeConfigEditorSection;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.ValidateConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.ValidateConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.ValidationMessage;

/**
 * Advanced environment configuration editor part.
 */
@SuppressWarnings("restriction")
public class EnvironmentConfigEditorPart extends AbstractEnvironmentConfigEditorPart implements RefreshListener {

    private AwsAction exportTemplateAction;
    private AwsAction importTemplateAction;

    private Composite leftColumnComp;
    private Composite rightColumnComp;

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);
        model.addRefreshListener(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        managedForm = new ManagedForm(parent);
        setManagedForm(managedForm);
        ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        toolkit.decorateFormHeading(form.getForm());
        form.setText("Environment Configuration");
        form.setImage(ImageResource.getImage(ImageResource.IMG_SERVER));
        form.getBody().setLayout(new GridLayout());

        Composite columnComp = toolkit.createComposite(form.getBody());
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = 10;
        columnComp.setLayout(layout);
        columnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        Label restartNotice = toolkit.createLabel(columnComp, RESTART_NOTICE, SWT.WRAP);
        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, false, false);
        layoutData.horizontalSpan = 2;
        layoutData.widthHint = 600; // required for wrapping
        restartNotice.setLayoutData(layoutData);

        // left column
        leftColumnComp = toolkit.createComposite(columnComp);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 0;
        leftColumnComp.setLayout(layout);
        leftColumnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        // right column
        rightColumnComp = toolkit.createComposite(columnComp);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 0;
        rightColumnComp.setLayout(layout);
        rightColumnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        refreshAction = new AwsAction(
                AwsToolkitMetricType.EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR,
                "Refresh", SWT.None) {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh");
            }
            @Override
            protected void doRun() {
                refresh(null);
                actionFinished();
            }
        };

        managedForm.getForm().getToolBarManager().add(refreshAction);

        exportTemplateAction = new AwsAction(
                AwsToolkitMetricType.EXPLORER_BEANSTALK_EXPORT_TEMPLATE,
                "Export current values as template", SWT.None) {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return ImageDescriptor.createFromFile(ElasticBeanstalkPlugin.class, "/icons/export.gif");
            }

            @Override
            protected void doRun() {
                exportAsTemplate();
                actionFinished();
            }
        };

        importTemplateAction = new AwsAction(
                AwsToolkitMetricType.EXPLORER_BEANSTALK_IMPORT_TEMPLATE,
                "Import template values into editor", SWT.None) {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return ImageDescriptor.createFromFile(ElasticBeanstalkPlugin.class, "/icons/import.gif");
            }

            @Override
            protected void doRun() {
                importTemplate();
                actionFinished();
            }

        };

        managedForm.getForm().getToolBarManager().add(importTemplateAction);
        managedForm.getForm().getToolBarManager().add(exportTemplateAction);
        managedForm.getForm().getToolBarManager().update(true);

        managedForm.reflow(true);

        refresh(null);
    }



    /**
     * Imports a template's config settings into the editor.
     */
    protected void importTemplate() {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        DescribeApplicationsResult result = client.describeApplications(new DescribeApplicationsRequest()
                .withApplicationNames(environment.getApplicationName()));
        final Collection<String> existingTemplateNames = new HashSet<>();
        for ( ApplicationDescription app : result.getApplications() ) {
            for ( String templateName : app.getConfigurationTemplates() ) {
                existingTemplateNames.add(templateName);
            }
        }

        final ImportTemplateDialog dialog = new ImportTemplateDialog(getSite().getShell(), existingTemplateNames);
        dialog.open();

        if ( dialog.getReturnCode() == MessageDialog.OK ) {
            refresh(dialog.getTemplateName());
        }
    }

    /**
     * Refreshes the editor with the latest values
     */
    @Override
    public void refresh(String templateName) {
        model.refresh(templateName);
    }

    /**
     * Exports the current model as a template, prompting the user for a name.
     */
    private void exportAsTemplate() {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        DescribeApplicationsResult result = client.describeApplications(new DescribeApplicationsRequest()
                .withApplicationNames(environment.getApplicationName()));
        final Collection<String> existingTemplateNames = new HashSet<>();
        for ( ApplicationDescription app : result.getApplications() ) {
            for ( String templateName : app.getConfigurationTemplates() ) {
                existingTemplateNames.add(templateName);
            }
        }

        try(final ExportTemplateDialog dialog = new ExportTemplateDialog(getSite().getShell(), existingTemplateNames,
                "newTemplate" + System.currentTimeMillis())) {

                  dialog.open();
                  if ( dialog.getReturnCode() == MessageDialog.OK ) {

                    new ExportConfigurationJob(environment, dialog.getTemplateName(), dialog.getTemplateDescription(), model.createConfigurationOptions(),
         	                dialog.isCreatingNew()).schedule();
        	        }
        } catch (Exception exception) {
		        System.out.println(exception);
	      }
    }

    /**
     * Creates and returns the appropriate sections for the configuration
     * options given, one per namespace.
     */
    private List<EnvironmentConfigEditorSection> createEditorSections(List<ConfigurationOptionDescription> options) {
        List<EnvironmentConfigEditorSection> editorSections = new ArrayList<>();
        Map<String, List<ConfigurationOptionDescription>> optionsByNamespace = new HashMap<>();
        for ( ConfigurationOptionDescription o : options ) {
            if ( !optionsByNamespace.containsKey(o.getNamespace()) ) {
                ArrayList<ConfigurationOptionDescription> optionsInNamespace = new ArrayList<>();
                optionsByNamespace.put(o.getNamespace(), optionsInNamespace);
                // We use our customized environment type section
                if (o.getNamespace().equals(ConfigurationOptionConstants.ENVIRONMENT_TYPE) && o.getName().equals("EnvironmentType")) {
                    editorSections.add(new AdvancedEnvironmentTypeConfigEditorSection(this, model, environment, bindingContext, o.getNamespace(),
                            optionsInNamespace));
                } else {
                    editorSections.add(new EnvironmentConfigEditorSection(this, model, environment, bindingContext, o.getNamespace(), optionsInNamespace));
                }
            }
            optionsByNamespace.get(o.getNamespace()).add(o);
        }
        return editorSections;
    }

    @Override
    public void refreshStarted() {
        /*
         * Although we are likely already in the UI thread, not executing this
         * in the context of the parent shell's UI thread leads to race
         * conditions with the RefreshThread's own composite manipulation.
         */
        getEditorSite().getShell().getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {
                exportTemplateAction.setEnabled(false);
                importTemplateAction.setEnabled(false);
                managedForm.getForm().setText(getTitle() + " (loading...)");
            }
        });
    }

    @Override
    public void refreshFinished() {

        getEditorSite().getShell().getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {

                // Every time we redraw the layouts.
                destroyOldLayouts();
                final List<EnvironmentConfigEditorSection> editorSections = createEditorSections(model.getOptions());

                int numLeft = 0;
                int numRight = 0;
                for (EnvironmentConfigEditorSection section : editorSections) {
                    section.setServerEditorPart(EnvironmentConfigEditorPart.this);
                    section.init(getEditorSite(), getEditorInput());

                    if (numLeft <= numRight) {
                        section.createSection(leftColumnComp);
                        numLeft += section.getNumControls();
                    } else {
                        section.createSection(rightColumnComp);
                        numRight += section.getNumControls();
                    }

                    managedForm.reflow(true);
                }

                managedForm.getForm().setText(getTitle());
                exportTemplateAction.setEnabled(true);
                importTemplateAction.setEnabled(true);
            }
        });

    }

    @Override
    public void refreshError(Throwable e) {
        ElasticBeanstalkPlugin.getDefault().getLog().log(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Error creating editor", e));
    }

    /**
     * Called on every editor part by the parent multi-page editor when the user
     * saves. An OK status means a save can take place; an error results in a
     * pop-up error dialog.
     */
    @Override
    public IStatus[] getSaveStatus() {

        if ( !isDirty() )
            return new IStatus[] { Status.OK_STATUS };

        // Don't allow a save unless the form has no errors
        Object aggregateFormStatus = aggregateValidationStatus.getValue();
        if ( aggregateFormStatus instanceof IStatus == false )
            return new IStatus[] { Status.OK_STATUS };
        else if ( ((IStatus) aggregateFormStatus).getSeverity() == IStatus.ERROR ) {
            return new IStatus[] { (IStatus) aggregateFormStatus };
        }

        try {
            AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                    .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());


            ValidateConfigurationSettingsResult validation = client
                    .validateConfigurationSettings(new ValidateConfigurationSettingsRequest()
                            .withApplicationName(environment.getApplicationName())
                            .withEnvironmentName(environment.getEnvironmentName())
                                    .withOptionSettings(model.createConfigurationOptions()));

            for ( ValidationMessage status : validation.getMessages() ) {
                if ( status.getSeverity().toLowerCase().equals("error") ) {
                    return new IStatus[] { new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, status.getMessage()) };
                }
            }

            // We can't save these values unless the environment is available
            List<EnvironmentDescription> envs = client.describeEnvironments(
                    new DescribeEnvironmentsRequest().withEnvironmentNames(environment.getEnvironmentName()))
                    .getEnvironments();
            if ( envs.size() > 1 ) {
                return new IStatus[] { new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Environment busy") };
            }
            if ( !envs.get(0).getStatus().equals(EnvironmentStatus.Ready.toString()) ) {
                return new IStatus[] { new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                        "Environment must be available before saving") };
            }
        } catch ( Exception e ) {
            return new IStatus[] { new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage(), e) };
        }

        return new IStatus[] { Status.OK_STATUS };
    }

    /**
     * Performs the save operation, implemented as a job.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        if (dirty) {
            UpdateEnvironmentRequest rq = new UpdateEnvironmentRequest();
            rq.setEnvironmentName(environment.getEnvironmentName());
            Collection<ConfigurationOptionSetting> settings = model.createConfigurationOptions();
            rq.setOptionSettings(settings);
            UpdateEnvironmentConfigurationJob job = new UpdateEnvironmentConfigurationJob(environment, rq);
            job.schedule();
            dirty = false;
        }
    }

    @Override
    public void destroyOldLayouts() {
        // Not allow refresh action during the destroying of controls
        refreshAction.setEnabled(false);
        if (leftColumnComp != null) {
            for (Control control : leftColumnComp.getChildren()) {
                control.dispose();
            }
        }

        if (rightColumnComp != null) {
            for (Control control : rightColumnComp.getChildren()) {
                control.dispose();
            }
        }
        refreshAction.setEnabled(true);
    }

}
