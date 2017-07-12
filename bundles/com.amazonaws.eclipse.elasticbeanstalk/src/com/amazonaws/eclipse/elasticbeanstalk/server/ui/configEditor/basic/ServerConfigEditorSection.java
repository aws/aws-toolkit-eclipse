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
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairRefreshListener;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Environment config editor section for setting "Server" properties, roughly
 * corresponding to the aws:autoscaling:launchconfiguration namespace.
 */
public class ServerConfigEditorSection extends HumanReadableConfigEditorSection {

    private static final Map<String, String> humanReadableNames = new HashMap<>();
    static {
        humanReadableNames.put("EC2KeyName", "Existing Key Pair");
        humanReadableNames.put("SecurityGroups", "EC2 Security Groups");
        humanReadableNames.put("InstanceType", "EC2 Instance Type");
        humanReadableNames.put("MonitoringInterval", "Monitoring Interval");
        humanReadableNames.put("ImageId", "Custom AMI ID");
    }

    private static final String[] fieldOrder = new String[] { "InstanceType", "SecurityGroups", "EC2KeyName",
            "MonitoringInterval", "ImageId" };

    public ServerConfigEditorSection(BasicEnvironmentConfigEditorPart basicEnvironmentConfigurationEditorPart,
            EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
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
        return "Server";
    }

    @Override
    protected String getSectionDescription() {
        return "These settings allow you to control your environment's servers and enable login.";
    }

    @Override
    protected Section getSection(Composite parent) {
        return toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION | Section.NO_TITLE);
    }

    @Override
    protected void createSectionControls(Composite composite) {
        for ( String field : getFieldOrder() ) {
            for ( ConfigurationOptionDescription o : options ) {
                if ( field.equals(o.getName()) ) {
                    if ( field.equals("EC2KeyName") ) {
                        createKeyPairControl(composite, o);
                    } else {
                        createOptionControl(composite, o);
                    }
                }
            }
        }
    }

    /**
     * Creates the controls to allow the user to select a key pair with a gui.
     */
    private void createKeyPairControl(Composite parent, ConfigurationOptionDescription option) {
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        final KeyPairComposite keyPairWidget = new KeyPairComposite(parent, this.environment.getAccountId());

        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        layoutData.widthHint = 200;
        layoutData.heightHint = 200;
        keyPairWidget.setLayoutData(layoutData);

        final IObservableValue modelv = model.observeEntry(option);
        final IChangeListener listener = new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                for ( int i = 0; i < keyPairWidget.getViewer().getTree().getItemCount(); i++ ) {
                    KeyPairInfo keyPair = (KeyPairInfo) keyPairWidget.getViewer().getTree().getItem(i).getData();
                    if ( keyPair.getKeyName().equals(modelv.getValue()) ) {
                        keyPairWidget.getViewer().getTree().select(keyPairWidget.getViewer().getTree().getItem(i));
                        // Selection listeners aren't notified when we
                        // select like this, so fire an event.
                        keyPairWidget.getViewer().getTree().notifyListeners(SWT.Selection, null);
                        return;
                    }
                }
                keyPairWidget.getViewer().getTree().deselectAll();
            }
        };
        modelv.addChangeListener(listener);

        /*
         * The table is initially empty, so we need to add a hook to fire our
         * change listener when it gets refreshed in order to correctly select
         * the current key pair.
         */
        keyPairWidget.getKeyPairSelectionTable().addRefreshListener(new KeyPairRefreshListener() {
            @Override
            public void keyPairsRefreshed() {
                listener.handleChange(null);
            }
        });

        final IObservableValue keyPairSelection = new WritableValue();
        keyPairWidget.getKeyPairSelectionTable().addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (keyPairWidget.getKeyPairSelectionTable().getSelectedKeyPair() == null)
                    keyPairSelection.setValue("");
                else
                    keyPairSelection.setValue(keyPairWidget.getKeyPairSelectionTable().getSelectedKeyPair().getKeyName());
            }
        });

        bindingContext.bindValue(keyPairSelection, modelv,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));
    }

}
