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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Human readable editor for container / jvm options
 */
public class ContainerConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("Xms",                   "Initial JVM Heap Size (-Xms argument)");
        humanReadableNames.put("Xmx",                   "Maximum JVM Heap Size (-Xmx argument)");
        humanReadableNames.put("XX:MaxPermSize",        "Maximum JVM PermGen Size (-XX:MaxPermSize argument)");
        humanReadableNames.put("JVM Options",           "Additional Tomcat JVM command line options");
        humanReadableNames.put("LogPublicationControl", "Enable log file rotation to Amazon S3");
    }

    private static final String[] fieldOrder = new String[] { "Xms", "Xmx", "XX:MaxPermSize", "JVM Options", "LogPublicationControl"};

    public ContainerConfigEditorSection(BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart, EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
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
        return "Container / JVM Options";
    }

    @Override
    protected String getSectionDescription() {
        return "These settings control command-line options for " + "your container and the underlying JVM.";
    }

    @Override
    protected void createSectionControls(Composite composite) {
        super.createSectionControls(composite);
        createDebugEnablementControl(composite);
    }

    private void createDebugEnablementControl(Composite composite) {

        final Button enablement = toolkit.createButton(composite, "Enable remote debugging", SWT.CHECK);       
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        layoutData.horizontalSpan = 2;
        enablement.setLayoutData(layoutData);
        
        final Label portLabel = toolkit.createLabel(composite, "Remote debugging port:");
        portLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        
        final Text portText = toolkit.createText(composite, "");
        layoutTextField(portText);
        
        ConfigurationOptionDescription jvmOptions = null;
        for ( ConfigurationOptionDescription opt : options ) {
            if ( "JVM Options".equals(opt.getName()) ) {
                jvmOptions = opt;
                break;
            }
        }
        if ( jvmOptions == null )
            throw new RuntimeException("Couldn't determine JVM options");

        /*
         * These bindings are complicated because we don't copy the value across
         * directly. Instead, we modify the JVM options string (or respond to
         * modification of it).
         */
        final IObservableValue jvmOptionsObservable = model.observeEntry(jvmOptions);
        final IObservableValue textObservable = SWTObservables.observeText(portText, SWT.Modify);
        final IObservableValue enablementObservable = SWTObservables.observeSelection(enablement);
                
        IValueChangeListener listener = new IValueChangeListener() {
            @Override
            public void handleValueChange(ValueChangeEvent event) {
                portText.setEnabled((Boolean) enablementObservable.getValue());
                portLabel.setEnabled((Boolean) enablementObservable.getValue());
            }
        };
        enablementObservable.addValueChangeListener(listener);
        
        setupBindingsForDebugPort(jvmOptionsObservable, textObservable);        
        setupBindingsForDebugEnablement(jvmOptionsObservable, enablementObservable);
        listener.handleValueChange(null);
    }

    protected void setupBindingsForDebugEnablement(final IObservableValue jvmOptionsObservable,
                                                   final IObservableValue enablementObservable) {
        UpdateValueStrategy debugEnabledModelToTarget = new UpdateValueStrategy(
                UpdateValueStrategy.POLICY_UPDATE);
        UpdateValueStrategy debugEnabledTargetToModel = new UpdateValueStrategy(
                UpdateValueStrategy.POLICY_UPDATE);
        debugEnabledModelToTarget.setConverter(new IConverter() {
            
            @Override
            public Object getToType() {
                return Boolean.class;
            }
            
            @Override
            public Object getFromType() {
                return String.class;
            }
            
            @Override
            public Object convert(Object fromObject) {
                return ((String)fromObject).contains("-Xdebug");
            }
        });
        
        
        debugEnabledTargetToModel.setConverter(new IConverter() {

            @Override
            public Object getToType() {
                return String.class;
            }

            @Override
            public Object getFromType() {
                return Boolean.class;
            }

            @Override
            public Object convert(Object fromObject) {
                String currentOptions = (String) jvmOptionsObservable.getValue();

                if ( (Boolean) fromObject ) {
                    if ( !currentOptions.contains("-Xdebug") ) {
                        currentOptions += " " + "-Xdebug";
                    }
                } else {
                    Matcher matcher = Pattern.compile("-Xdebug").matcher(currentOptions);
                    if ( matcher.find() )
                        currentOptions = matcher.replaceFirst("");
                    matcher = Pattern.compile("-Xrunjdwp:\\S+").matcher(currentOptions);
                    if ( matcher.find() )
                        currentOptions = matcher.replaceFirst("");                    
                }
                return currentOptions;
            }
        });
        
        bindingContext.bindValue(enablementObservable, jvmOptionsObservable, debugEnabledTargetToModel,
                debugEnabledModelToTarget).updateModelToTarget();
    }

    protected void setupBindingsForDebugPort(final IObservableValue jvmOptionsObservable,
                                             final IObservableValue textObservable) {
        UpdateValueStrategy portTargetToModel = new UpdateValueStrategy(
                UpdateValueStrategy.POLICY_UPDATE);
        portTargetToModel.setConverter(new IConverter() {
            
            @Override
            public Object getToType() {
                return String.class;
            }
            
            @Override
            public Object getFromType() {
                return String.class;
            }
            
            @Override
            public Object convert(Object fromObject) {
                String debugPort = (String) fromObject;
                String currentOptions = (String) jvmOptionsObservable.getValue();

                if ( debugPort != null && debugPort.length() > 0 ) {

                    if ( !currentOptions.contains("-Xrunjdwp:") ) {
                        currentOptions += " " + "-Xrunjdwp:transport=dt_socket,address=" + debugPort
                                + ",server=y,suspend=n";
                    } else {
                        Matcher matcher = Pattern.compile("(-Xrunjdwp:\\S*address=)\\d+").matcher(currentOptions);
                        if ( matcher.find() )
                            currentOptions = matcher.replaceFirst(matcher.group(1) + debugPort);                        
                    }

                } else {
                    Matcher matcher = Pattern.compile("-Xrunjdwp:\\S+").matcher(currentOptions);
                    if ( matcher.find() )
                        currentOptions = matcher.replaceFirst("");
                }

                return currentOptions;
            }
        });
        
        UpdateValueStrategy portModelToTarget = new UpdateValueStrategy(
                UpdateValueStrategy.POLICY_UPDATE);
        portModelToTarget.setConverter(new IConverter() {
            
            @Override
            public Object getToType() {
                return String.class;
            }
            
            @Override
            public Object getFromType() {
                return String.class;
            }
            
            @Override
            public Object convert(Object fromObject) {
                String debugPort = Environment.getDebugPort((String) fromObject);
                if (debugPort != null)
                    return debugPort;
                return "";
            }
        });
        
        bindingContext.bindValue(textObservable, jvmOptionsObservable, portTargetToModel, portModelToTarget)
                .updateModelToTarget();
    }
    

}
