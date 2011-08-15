/*
 * Copyright 2011 Amazon Technologies, Inc.
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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.services.rds.model.DBInstance;

public class ConfigureImportOptionsPage extends WizardPage {
    private final DataBindingContext bindingContext = new DataBindingContext();
    private final ImportDBInstanceDataModel wizardDataModel;
    private Text dbPasswordText;
    private OracleExtendedOptions extendedOptions;

    protected ConfigureImportOptionsPage(ImportDBInstanceDataModel wizardDataModel) {
        super("configureRdsDbWizardPage3");
        this.wizardDataModel = wizardDataModel;
    }

    private void createSecurityWarning(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Label warningLabel = new Label(composite, SWT.WRAP);
        warningLabel.setText("Note: Connecting to your database will automatically configure a security group ingress to allow incomming connections.");
    }

    private void createBasicOptions(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Connection Options:");
        group.setLayout(new FillLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));


        Composite composite = new Composite(group, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        new Label(composite, SWT.NONE).setText("User:");
        new Label(composite, SWT.NONE).setText(wizardDataModel.getDbInstance().getMasterUsername());

        new Label(composite, SWT.NONE).setText("Password:");
        dbPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
        dbPasswordText.setLayoutData(new GridData(150, SWT.DEFAULT));
    }

    private static class OracleExtendedOptions {
        private static final String ORACLE_DRIVER_DOWNLOAD_LINK = "http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html";
        private Text driverText;
        private ISWTObservableValue driverObservable;
        private final ImportDBInstanceDataModel wizardDataModel;

        public OracleExtendedOptions(ImportDBInstanceDataModel wizardDataModel) {
            this.wizardDataModel = wizardDataModel;
        }

        public void createExtendedOptions(Composite parent) {
            Group group = new Group(parent, SWT.NONE);
            group.setText("Oracle Driver");
            group.setLayout(new FillLayout());
            group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));


            Composite composite = new Composite(group, SWT.NONE);
            composite.setLayout(new GridLayout(3, false));

            /*
             * TODO: We really only need to show this the first time, before we have a driver created.
             */

            new Label(composite, SWT.NONE).setText("Oracle Driver:");
            driverText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
            driverText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            driverObservable = SWTObservables.observeText(driverText, SWT.Modify);


            Button selectDriverButton = new Button(composite, SWT.PUSH);
            selectDriverButton.setText("Select Jar");

            selectDriverButton.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    FileDialog fileDialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.MULTI);
                    if (fileDialog.open() == null) return;

                    String file = fileDialog.getFileName();
                    driverText.setText(file);
                    wizardDataModel.setJdbcDriver(new File(file));
                }

                public void widgetDefaultSelected(SelectionEvent e) {}
            });



            Label driverDescription = new Label(composite, SWT.WRAP);
            GridData driverDescriptionLayoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
            driverDescriptionLayoutData.widthHint = 300;
            driverDescriptionLayoutData.horizontalSpan = 3;
            driverDescription.setLayoutData(driverDescriptionLayoutData);
            driverDescription.setText("To connect to an Oracle database, first accept the license, then download the Oracle Thin JDBC driver, and select the Oracle JDBC driver Jar in the text field above.");

            Link link = new Link(composite, SWT.WRAP);
            link.setText("<a href=\"" + ORACLE_DRIVER_DOWNLOAD_LINK + "\">Oracle Thin JDBC Driver Downloads</a>");
            GridDataFactory.swtDefaults().span(3, 1).align(SWT.FILL, SWT.TOP).grab(true, false).minSize(300, SWT.DEFAULT).applyTo(link);
            link.addListener(SWT.Selection, new WebLinkListener());
        }

        public IStatus validateExtendedOptions() {
            if (driverText == null || driverText.getText() == null || driverText.getText().length() == 0) {
                return new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, "No driver specified");
            }

            return Status.OK_STATUS;
        }

        public IObservableValue getObservable() {
            return driverObservable;
        }
    }



    public void createControl(Composite parent) {
        this.setDescription("Specify options for connecting to your Amazon RDS database.");
        this.setTitle("Configure RDS Database Connection");

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        setControl(composite);

        createBasicOptions(composite);

        /*
         * If this is an Oracle DB, then we should ask for the location of the
         * Oracle Driver to use (and provide help on where to find it!)
         */

        DBInstance dbInstance = wizardDataModel.getDbInstance();


        // Oracle requires a Jar we can't distribute...
        if (dbInstance.getEngine().startsWith("oracle")) {
            extendedOptions = new OracleExtendedOptions(wizardDataModel);
            extendedOptions.createExtendedOptions(composite);
        }

        createSecurityWarning(composite);

        this.setPageComplete(false);
        bindControls();
    }

    @SuppressWarnings("static-access")
    private void bindControls() {
        ISWTObservableValue swtObservable = SWTObservables.observeText(dbPasswordText, SWT.Modify);

        bindingContext.bindValue(
            swtObservable,
            PojoObservables.observeValue(wizardDataModel, wizardDataModel.DB_PASSWORD),
            null, null);

        IValueChangeListener valueChangeListener = new IValueChangeListener() {
            public void handleValueChange(ValueChangeEvent event) {
                validateUserInput();
            }
        };

        swtObservable.addValueChangeListener(valueChangeListener);
        if (extendedOptions != null) {
            extendedOptions.getObservable().addValueChangeListener(valueChangeListener);
        }
    }

    private void validateUserInput() {
        boolean complete = dbPasswordText.getText().length() > 0;

        if (extendedOptions != null) {
            IStatus status = extendedOptions.validateExtendedOptions();
            if (status.isOK() == false) complete = false;
        }

        setPageComplete(complete);
    }
}