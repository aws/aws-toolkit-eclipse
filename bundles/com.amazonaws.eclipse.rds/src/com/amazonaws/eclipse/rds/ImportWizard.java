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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.rds.model.DBInstance;

public class ImportWizard extends Wizard implements IImportWizard {

    private final ImportDBInstanceDataModel wizardDataModel = new ImportDBInstanceDataModel();

    public ImportWizard(DBInstance dbInstanceToImport) {
        if (dbInstanceToImport == null)
            throw new NullPointerException("dbInstanceToImport must not be null.");

        this.setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_WIZARD_CONFIGURE_DATABASE));
        this.setWindowTitle("Configure RDS Database Connection");
        this.setNeedsProgressMonitor(true);

        wizardDataModel.setDbInstance(dbInstanceToImport);
        addPage(new ConfigureImportOptionsPage(wizardDataModel));
    }

    @Override
    public boolean performFinish() {
        try {
            ConfigureRDSDBConnectionRunnable runnable = new ConfigureRDSDBConnectionRunnable(wizardDataModel);
            getContainer().run(true, true, runnable);
            return runnable.didCompleteSuccessfully();
        } catch (Throwable t) {
            String errorMessage = "Unable to connect to RDS database";
            if (t.getMessage() != null) errorMessage += ": " + t.getMessage();
            Status status = new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, errorMessage, t);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }

        return false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {}

}
