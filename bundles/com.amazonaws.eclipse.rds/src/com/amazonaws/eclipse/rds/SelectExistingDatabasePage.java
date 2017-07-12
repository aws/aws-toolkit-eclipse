/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;

class SelectExistingDatabasePage extends WizardPage {
    protected DataBindingContext bindingContext = new DataBindingContext();
    private Combo dbCombo;
    private final AmazonRDS rds;

    private static final String PAGE_NAME = "configureRdsDbWizardPage2";
    private final ImportDBInstanceDataModel wizardDataModel;

    protected SelectExistingDatabasePage(AmazonRDS rds, ImportDBInstanceDataModel wizardDataModel) {
        super(PAGE_NAME);
        this.rds = rds;
        this.wizardDataModel = wizardDataModel;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        setControl(composite);

        LoadExistingDatabasesRunnable runnable = new LoadExistingDatabasesRunnable(rds);
        try {
            getContainer().run(true, true, runnable);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * TODO: Handle edge cases:
         *    1 - not signed up for AWS
         *    2 - security credentials not configured
         *    3 - not signed up for RDS
         *    4 - no databases to import yet
         */

        /*
         * TODO: Filter out the databases that have already been imported...
         */

        new Label(composite, SWT.NONE).setText("RDS DB Instance:");

        dbCombo = new Combo(composite, SWT.READ_ONLY);
        // TODO: Add support for multiple regions
        // TODO: We need to display more information about each DB than just the ID (maybe in a table)
        Collection<List<DBInstance>> values = runnable.dbsByRegion.values();
        for (DBInstance db : values.iterator().next()) {
            dbCombo.add(db.getDBInstanceIdentifier());
            dbCombo.setData(db.getDBInstanceIdentifier(), db);
        }


//        new Label(composite, SWT.NONE).setText("Password:");
//        dbPasswordText = new Text(composite, SWT.BORDER);

        // Don't edit current security groups...
        // Optionally create a new one
        //   - shared between all connected instances?
        //   - "Remote Client/Tool Access" group
        //   - get correct CIDR range from client

        bindControls();
    }

    @SuppressWarnings("static-access")
    private void bindControls() {
        bindingContext.bindValue(
            new ControlDataObservableValue(SWTObservables.observeSelection(dbCombo), true),
            PojoObservables.observeValue(wizardDataModel, wizardDataModel.DB_INSTANCE),
            null, null);
    }

    // TODO: do we need progress (could use for regions)
    static class LoadExistingDatabasesRunnable implements IRunnableWithProgress {
        private final AmazonRDS rds;
        public volatile Map<String, List<DBInstance>> dbsByRegion;

        public LoadExistingDatabasesRunnable(AmazonRDS rds) {
            // TODO: Stop passing in the individual client and just use the client factory to query each region
            this.rds = rds;
        }

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            // TODO: we really need one RDS client for each region... but we'll fake it for now...

            // TODO: host name should be enough to determine if we've imported a db yet?
            dbsByRegion = new HashMap<>();
            System.out.println("Identified DBs in US-EAST-1: ");
            List<DBInstance> dbInstances = rds.describeDBInstances().getDBInstances();
            for (DBInstance db : dbInstances) {
                System.out.println(" - " + db.getDBName() + " : " + db.getDBInstanceIdentifier());
            }
            dbsByRegion.put("us-east-1", dbInstances);
        }
    }
}
