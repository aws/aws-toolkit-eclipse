/*
 * Copyright 2011-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.rds;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.rds.connectionfactories.DatabaseConnectionFactory;
import com.amazonaws.eclipse.rds.util.CheckIpUtil;
import com.amazonaws.services.rds.model.DBInstance;

public class ConfigureImportOptionsPage extends WizardPage {
    /** Pattern for matching a CIDR IP range, ex: 72.130.0.0/32 */
    private static Pattern CIDR_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+$");

    private final DataBindingContext bindingContext = new DataBindingContext();
    private final ImportDBInstanceDataModel wizardDataModel;
    private Text dbPasswordText;
    private Text cidrIpRangeText;
    private Button configurePermissionsRadio;
    private Text driverText;
    private ISWTObservableValue driverObservable;
    private Button useExistingDriverRadio;
    private Combo existingDriverCombo;
    private Button selectDriverButton;
    private Button createNewDriverRadio;


    protected ConfigureImportOptionsPage(ImportDBInstanceDataModel wizardDataModel) {
        super("configureRdsDbWizardPage3");
        this.wizardDataModel = wizardDataModel;
    }


    private void createPermissionsSection(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Group group = new Group(composite, SWT.NONE);
        group.setText("Security Group Permissions:");
        group .setLayout(new GridLayout(2, false));

        configurePermissionsRadio = new Button(group, SWT.CHECK);
        configurePermissionsRadio.setText("Configure security group permissions automatically");
        configurePermissionsRadio.setSelection(true);
        configurePermissionsRadio.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        Label label = new Label(group, SWT.WRAP);
        label.setText("Security group permissions must be configured to allow incoming connections in order to connect to your RDS DB instance.  "
                + "If you choose not to open the security group permissions automatically, you MUST configure the security group permissions manually before you can connect to your DB instance.");
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1);
        gridData.widthHint = 500;
        gridData.horizontalIndent = 20;
        label.setLayoutData(gridData);


        Label cidrIpRangeLabel = new Label(group, SWT.NONE);
        cidrIpRangeLabel.setText("CIDR IP Range:");
        gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.horizontalIndent = 20;
        cidrIpRangeLabel.setLayoutData(gridData);

        cidrIpRangeText = new Text(group, SWT.BORDER);
        GridData gridData2 = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gridData2.widthHint = 150;
        cidrIpRangeText.setLayoutData(gridData2);

        new CheckOutgoingIpRange().schedule();
    }

    private void createBasicOptions(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Connection:");
        group.setLayout(new FillLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite composite = new Composite(group, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        DBInstance dbInstance = wizardDataModel.getDbInstance();

        new Label(composite, SWT.NONE).setText("DB Instance:");
        new Label(composite, SWT.NONE).setText(dbInstance.getDBInstanceIdentifier());

        new Label(composite, SWT.NONE).setText("Endpoint:");
        new Label(composite, SWT.NONE).setText(dbInstance.getEndpoint().getAddress() + ":" + dbInstance.getEndpoint().getPort());

        new Label(composite, SWT.NONE).setText("Engine:");
        new Label(composite, SWT.NONE).setText(dbInstance.getEngine() + " (" + dbInstance.getEngineVersion() + ")");

        new Label(composite, SWT.NONE).setText("User:");
        new Label(composite, SWT.NONE).setText(dbInstance.getMasterUsername());

        new Label(composite, SWT.NONE).setText("Password:");
        dbPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
        dbPasswordText.setLayoutData(new GridData(150, SWT.DEFAULT));
        dbPasswordText.setFocus();
    }

    private final class CheckOutgoingIpRange extends Job {
        private String cidr = "0.0.0.0/0";

        private CheckOutgoingIpRange() {
            super("Checking outgoing IP range");
        }

        @Override
        protected IStatus run(IProgressMonitor arg0) {
            try {
                cidr = CheckIpUtil.checkIp() + "/32";
                return Status.OK_STATUS;
            } catch (IOException ioe) {
                return new Status(IStatus.WARNING, RDSPlugin.PLUGIN_ID, "Unable to determine outgoing IP address", ioe);
            } finally {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        cidrIpRangeText.setText(cidr);
                    }
                });
            }
        }
    }

    public void createJdbcDriverSection(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("JDBC Driver:");
        group.setLayout(new FillLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));


        Composite composite = new Composite(group, SWT.NONE);
        composite.setLayout(new GridLayout(3, false));

        useExistingDriverRadio = new Button(composite, SWT.RADIO);
        useExistingDriverRadio.setText("Use existing driver definition:");
        useExistingDriverRadio.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));

        existingDriverCombo = new Combo(composite, SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1);
        gridData.horizontalIndent = 20;
        gridData.widthHint = 200;
        existingDriverCombo.setLayoutData(gridData);

        DatabaseConnectionFactory connectionFactory = DatabaseConnectionFactory.createConnectionFactory(wizardDataModel);
        for (DriverInstance driverInstance : DriverManager.getInstance().getDriverInstancesByTemplate(connectionFactory.getDriverTemplate())) {
            existingDriverCombo.add(driverInstance.getName());
            existingDriverCombo.setData(driverInstance.getName(), driverInstance);
        }

        createNewDriverRadio = new Button(composite, SWT.RADIO);
        createNewDriverRadio.setText("Create new driver definition:");
        createNewDriverRadio.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));

        Label driverJarLabel = new Label(composite, SWT.NONE);
        driverJarLabel.setText("Driver Jar:");
        GridData driverJarGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        driverJarGridData.horizontalIndent = 20;
        driverJarLabel.setLayoutData(driverJarGridData);

        driverText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        driverText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        driverObservable = SWTObservables.observeText(driverText, SWT.Modify);

        selectDriverButton = new Button(composite, SWT.PUSH);
        selectDriverButton.setText("Select Jar");

        selectDriverButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.MULTI);
                fileDialog.setFilterExtensions(new String[] {"jar"});
                if (fileDialog.open() == null) return;

                File file = new File(fileDialog.getFileName());
                if (fileDialog.getFilterPath() != null) {
                    file = new File(fileDialog.getFilterPath(), fileDialog.getFileName());
                }

                driverText.setText(file.getAbsolutePath());
                wizardDataModel.setJdbcDriver(file);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

        if (existingDriverCombo.getItemCount() > 0) {
            useExistingDriverRadio.setSelection(true);
            existingDriverCombo.select(0);
        } else {
            createNewDriverRadio.setSelection(true);
        }
    }

    @Override
    public void createControl(Composite parent) {
        this.setDescription("Specify options for connecting to your Amazon RDS database.");
        this.setTitle("Configure RDS Database Connection");

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        setControl(composite);

        createBasicOptions(composite);
        createJdbcDriverSection(composite);
        createPermissionsSection(composite);

        setPageComplete(false);
        bindControls();
    }

    @SuppressWarnings("static-access")
    private void bindControls() {
        ISWTObservableValue dbPasswordTextObservable = SWTObservables.observeText(dbPasswordText, SWT.Modify);
        ISWTObservableValue cidrIpTextObservable = SWTObservables.observeText(cidrIpRangeText, SWT.Modify);

        ISWTObservableValue configurePermissionsRadioObservable = SWTObservables.observeSelection(configurePermissionsRadio);
        ISWTObservableValue useExistingDriverRadioObservable = SWTObservables.observeSelection(useExistingDriverRadio);
        ISWTObservableValue createNewDriverObservable = SWTObservables.observeSelection(createNewDriverRadio);
        ISWTObservableValue existingDriverComboObservable = SWTObservables.observeSelection(existingDriverCombo);
        IObservableValue existingDriverComboDataObservable = new ControlDataObservableValue(existingDriverComboObservable, true);

        bindingContext.bindValue(dbPasswordTextObservable,
                PojoObservables.observeValue(wizardDataModel, wizardDataModel.DB_PASSWORD),
                null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
        bindingContext.bindValue(cidrIpTextObservable,
                PojoObservables.observeValue(wizardDataModel, wizardDataModel.CIDR_IP_RANGE),
                null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
        bindingContext.bindValue(useExistingDriverRadioObservable,
                PojoObservables.observeValue(wizardDataModel, wizardDataModel.USE_EXISTING_DRIVER_DEFINITION),
                null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
        bindingContext.bindValue(configurePermissionsRadioObservable,
                PojoObservables.observeValue(wizardDataModel, wizardDataModel.CONFIGURE_PERMISSIONS),
                null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));

        bindingContext.bindValue(existingDriverComboDataObservable,
                PojoObservables.observeValue(wizardDataModel, wizardDataModel.DRIVER_DEFINITION),
                null, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));

        IValueChangeListener valueChangeListener = new IValueChangeListener() {
            @Override
            public void handleValueChange(ValueChangeEvent event) {
                validateUserInput();
            }
        };

        dbPasswordTextObservable.addValueChangeListener(valueChangeListener);
        cidrIpTextObservable.addValueChangeListener(valueChangeListener);
        configurePermissionsRadioObservable.addValueChangeListener(valueChangeListener);
        useExistingDriverRadioObservable.addValueChangeListener(valueChangeListener);
        createNewDriverObservable.addValueChangeListener(valueChangeListener);

        driverObservable.addValueChangeListener(valueChangeListener);

        bindingContext.updateModels();
    }

    private void validateUserInput() {
        boolean complete = dbPasswordText.getText().length() > 0;

        if (configurePermissionsRadio.getSelection()) {
            cidrIpRangeText.setEnabled(true);
            Matcher matcher = CIDR_PATTERN.matcher(cidrIpRangeText.getText());
            complete &= matcher.matches();
        } else {
            cidrIpRangeText.setEnabled(false);
        }

        if (useExistingDriverRadio.getSelection()) {
            driverText.setEnabled(false);
            existingDriverCombo.setEnabled(true);
            selectDriverButton.setEnabled(false);

            if (existingDriverCombo.getSelectionIndex() < 0) {
                complete = false;
            }
        } else {
            driverText.setEnabled(true);
            existingDriverCombo.setEnabled(false);
            selectDriverButton.setEnabled(true);

            if (driverText == null || driverText.getText() == null || driverText.getText().length() == 0) {
                complete = false;
            }
        }

        setPageComplete(complete);
    }
}
