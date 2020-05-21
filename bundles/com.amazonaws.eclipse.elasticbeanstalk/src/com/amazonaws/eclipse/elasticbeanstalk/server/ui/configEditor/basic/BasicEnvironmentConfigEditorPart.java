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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Status;
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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.internal.ImageResource;

import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.AbstractEnvironmentConfigEditorPart;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.RefreshListener;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Basic environment configuration editor with reduced options and
 * human-readable names.
 */
@SuppressWarnings({ "unchecked", "restriction" })
public class BasicEnvironmentConfigEditorPart extends AbstractEnvironmentConfigEditorPart implements
        RefreshListener {

    /*
     * Map of which namespace controls need to be grouped into which editor.
     */
    private final Map<Collection<String>, HumanReadableConfigEditorSection> editorSectionsByNamespace = new HashMap<>();

    private static final Collection<String> serverNamespaces = new HashSet<>();
    private static final Collection<String> loadBalancingNamespaces = new HashSet<>();
    private static final Collection<String> healthCheckNamespaces = new HashSet<>();
    private static final Collection<String> sessionsNamespaces = new HashSet<>();
    private static final Collection<String> autoscalingNamespaces = new HashSet<>();
    private static final Collection<String> triggerNamespaces = new HashSet<>();
    private static final Collection<String> notificationsNamespaces = new HashSet<>();
    private static final Collection<String> containerNamespaces = new HashSet<>();
    private static final Collection<String> applicationEnvironmentNamespaces = new HashSet<>();
    private static final Collection<String> environmentNamespaces = new HashSet<>();
    private static final Collection<String> rollingDeploymentsNamespaces = new HashSet<>();


    static {
        serverNamespaces.add(ConfigurationOptionConstants.LAUNCHCONFIGURATION);
        loadBalancingNamespaces.add(ConfigurationOptionConstants.LOADBALANCER);
        healthCheckNamespaces.add(ConfigurationOptionConstants.HEALTHCHECK);
        healthCheckNamespaces.add(ConfigurationOptionConstants.APPLICATION);
        sessionsNamespaces.add(ConfigurationOptionConstants.POLICIES);
        autoscalingNamespaces.add(ConfigurationOptionConstants.ASG);
        triggerNamespaces.add(ConfigurationOptionConstants.TRIGGER);
        notificationsNamespaces.add(ConfigurationOptionConstants.SNS_TOPICS);
        containerNamespaces.add(ConfigurationOptionConstants.JVMOPTIONS);
        containerNamespaces.add(ConfigurationOptionConstants.HOSTMANAGER);
        applicationEnvironmentNamespaces.add(ConfigurationOptionConstants.ENVIRONMENT);
        environmentNamespaces.add(ConfigurationOptionConstants.ENVIRONMENT_TYPE);
        rollingDeploymentsNamespaces.add(ConfigurationOptionConstants.COMMAND);
    }

    private static final Collection<NamespaceGroup> sectionGroups = new ArrayList<>();
    static {
        sectionGroups.add(new NamespaceGroup("Server", Position.LEFT, serverNamespaces));
        sectionGroups.add(new NamespaceGroup("Load Balancing", Position.LEFT, loadBalancingNamespaces, healthCheckNamespaces, sessionsNamespaces));
        sectionGroups.add(new NamespaceGroup("Environment Type", Position.RIGHT, environmentNamespaces));
        sectionGroups.add(new NamespaceGroup("Rolling Deployments", Position.RIGHT, rollingDeploymentsNamespaces));
        sectionGroups.add(new NamespaceGroup("Auto Scaling", Position.RIGHT, autoscalingNamespaces, triggerNamespaces));
        sectionGroups.add(new NamespaceGroup("Notifications", Position.RIGHT, notificationsNamespaces));
        sectionGroups.add(new NamespaceGroup("Container", Position.CENTER, containerNamespaces, applicationEnvironmentNamespaces));
    }

    private static final Collection<String>[] sectionOrder = new Collection[] {
            serverNamespaces,
            loadBalancingNamespaces,healthCheckNamespaces, sessionsNamespaces,
            environmentNamespaces,
            rollingDeploymentsNamespaces,
            autoscalingNamespaces, triggerNamespaces,
            notificationsNamespaces,
            containerNamespaces, applicationEnvironmentNamespaces,  };

    /**
     * Each time we create our control section, we create one composite for each
     * group of namespaces.
     */
    private Map<String, Composite> compositesByNamespace;

    public BasicEnvironmentConfigEditorPart() {
        super();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        editorSectionsByNamespace.put(serverNamespaces, new ServerConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(loadBalancingNamespaces, new LoadBalancingConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(healthCheckNamespaces, new HealthCheckConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(sessionsNamespaces, new SessionConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(autoscalingNamespaces, new AutoScalingConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(triggerNamespaces, new ScalingTriggerConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(notificationsNamespaces, new NotificationsConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(containerNamespaces, new ContainerConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(applicationEnvironmentNamespaces, new EnvironmentPropertiesConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(environmentNamespaces, new EnvironmentTypeConfigEditorSection(this, model, environment, bindingContext));
        editorSectionsByNamespace.put(rollingDeploymentsNamespaces, new RollingDeploymentsConfigEditorSection(this, model, environment, bindingContext));

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
        Composite leftColumnComp = toolkit.createComposite(columnComp);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 0;
        leftColumnComp.setLayout(layout);
        leftColumnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        // right column
        Composite rightColumnComp = toolkit.createComposite(columnComp);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 0;
        rightColumnComp.setLayout(layout);
        rightColumnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        // "center" column -- composite below the two above, spanning both columns
        Composite centerColumnComp = toolkit.createComposite(columnComp);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 0;
        centerColumnComp.setLayout(layout);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
        layoutData.horizontalSpan = 2;
        centerColumnComp.setLayoutData(layoutData);

        compositesByNamespace = new HashMap<>();
        for ( NamespaceGroup namespaceGroup : sectionGroups ) {

            Composite parentComp = null;
            switch (namespaceGroup.getPosition()) {
            case LEFT:
                parentComp = leftColumnComp;
                break;
            case RIGHT:
                parentComp = rightColumnComp;
                break;
            case CENTER:
                parentComp = centerColumnComp;
                break;
            }

            Section section = toolkit.createSection(parentComp, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                    | ExpandableComposite.TITLE_BAR | ExpandableComposite.FOCUS_TITLE);
            section.setText(namespaceGroup.getName());

            layout = new GridLayout();
            layout.numColumns = 1;
            layout.verticalSpacing = 0;
            section.setLayout(layout);
            section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

            Composite comp = toolkit.createComposite(section);
            layout = new GridLayout();
            int numColumns = 0;
            switch (namespaceGroup.getPosition()) {
            case LEFT:
            case RIGHT:
                numColumns = 1;
                break;
            case CENTER:
                numColumns = 2;
                break;
            }

            layout.numColumns = numColumns;
            layout.verticalSpacing = 0;
            comp.setLayout(layout);
            comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
            section.setClient(comp);

            for (String namespace : namespaceGroup.getIncludedNamespaces()) {
                compositesByNamespace.put(namespace, comp);
            }
        }

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

        managedForm.getForm().getToolBarManager().update(true);
        managedForm.reflow(true);
    }

    /**
     * Refreshes the editor with the latest values
     */
    @Override
    public void refresh(String templateName) {
        model.refresh(templateName);
    }

    @Override
    public void destroyOldLayouts() {
        // Not allow refresh action during the destroying of controls
        refreshAction.setEnabled(false);
        for (Composite composite : compositesByNamespace.values()) {
            for (Control control : composite.getChildren()) {
                control.dispose();
            }
        }
        refreshAction.setEnabled(true);
    }

    private List<HumanReadableConfigEditorSection> createEditorSections(List<ConfigurationOptionDescription> options) {

        List<HumanReadableConfigEditorSection> editorSections = new ArrayList<>();

        for ( Collection<String> namespaces : sectionOrder ) {
            ArrayList<ConfigurationOptionDescription> optionsInEditorSection = new ArrayList<>();
            for ( ConfigurationOptionDescription o : options ) {
                if ( namespaces.contains(o.getNamespace()) && editorSectionsByNamespace.containsKey(namespaces) ) {
                    if ( optionsInEditorSection.isEmpty() ) {
                        HumanReadableConfigEditorSection editor = editorSectionsByNamespace.get(namespaces);
                        editor.setOptions(optionsInEditorSection);
                        editorSections.add(editor);
                        editor.setParentComposite(compositesByNamespace.get(o.getNamespace()));
                    }
                    optionsInEditorSection.add(o);
                }
            }
        }

        return editorSections;
    }

    @Override
    public void refreshStarted() {
        getEditorSite().getShell().getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {
                managedForm.getForm().setText(getTitle() + " (loading...)");
            }
        });
    }

    @Override
    public void refreshFinished() {
        getEditorSite().getShell().getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {

                if ( compositesByNamespace.values().iterator().next().getChildren().length == 0 ) {
                    final List<HumanReadableConfigEditorSection> editorSections = createEditorSections(model
                            .getOptions());
                    for ( HumanReadableConfigEditorSection section : editorSections ) {

                        // Skip creating "Auto Scaling" and "Rolling Deployments" sections if the
                        // environment is deployed to single-instance
                        if (section instanceof RollingDeploymentsConfigEditorSection ||
                                section instanceof AutoScalingConfigEditorSection) {
                            Object envTypeValue = model.getEntry(
                                    new ConfigurationOptionDescription()
                                            .withNamespace(ConfigurationOptionConstants.ENVIRONMENT_TYPE)
                                            .withName("EnvironmentType"));
                            if ("SingleInstance".equals(envTypeValue)) {
                                continue;
                            }
                        }

                        section.setServerEditorPart(BasicEnvironmentConfigEditorPart.this);
                        section.init(getEditorSite(), getEditorInput());
                        section.createSection(section.getParentComposite());
                        managedForm.reflow(true);
                    }

                }
                dirty = false;
                BasicEnvironmentConfigEditorPart.this.doSaveAs();
                managedForm.getForm().setText(getTitle());
            }
        });
    }

    @Override
    public void refreshError(Throwable e) {
        ElasticBeanstalkPlugin.getDefault().getLog().log(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Error creating editor", e));
    }

    private static enum Position {
        LEFT, RIGHT, CENTER
    };

    /**
     * Simple data class to avoid too many levels of collection nesting.
     */
    private static final class NamespaceGroup {

        private final Collection<String> includedNamespaces;
        final String name;
        final Position position;

        public NamespaceGroup(String name, Position pos, Collection<String>... namespaces) {
            this.name = name;
            includedNamespaces = new HashSet<>();
            for ( Collection<String> namespace : namespaces ) {
                includedNamespaces.addAll(namespace);
            }
            position = pos;
        }

        public String getName() {
            return name;
        }

        public Collection<String> getIncludedNamespaces() {
            return includedNamespaces;
        }

        public Position getPosition() {
            return position;
        }
    }
}
