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
package com.amazonaws.eclipse.explorer.rds;

import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.rds.ImportWizard;
import com.amazonaws.eclipse.rds.RDSDriverDefinitionConstants;
import com.amazonaws.eclipse.rds.RDSPlugin;
import com.amazonaws.services.rds.model.DBInstance;

public class RDSExplorerActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
        if (selection.getFirstElement() == RDSExplorerNodes.RDS_ROOT_NODE) {
            menu.add(new OpenRdsConsoleAction());
        }
    }

    private final class OpenRdsConsoleAction extends Action {
        public OpenRdsConsoleAction() {
            this.setText("Go to RDS Management Console");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_EXTERNAL_LINK));
        }

        @Override
        public void run() {
            BrowserUtils.openExternalBrowser("http://console.aws.amazon.com/rds");
        }
    }

    public static class ConfigureConnectionProfileAction extends Action {
        private final DBInstance dbInstance;

        public ConfigureConnectionProfileAction(DBInstance dbInstance) {
            this.dbInstance = dbInstance;
            this.setText("Connect...");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_GEAR));

            String engine = dbInstance.getEngine();
            if (engine.equals("mysql") || engine.startsWith("oracle-")) {
                this.setEnabled(true);
            } else {
                // Don't enable this action if we don't understand the database type
                this.setEnabled(false);
            }
        }

        @Override
        public void run() {
            for (final IConnectionProfile profile : ProfileManager.getInstance().getProfiles()) {
                Properties properties = profile.getBaseProperties();
                String profileInstanceId = properties.getProperty(RDSDriverDefinitionConstants.DB_INSTANCE_ID);
                String profileRegionId = properties.getProperty(RDSDriverDefinitionConstants.DB_REGION_ID);
                String profileAccountId = properties.getProperty(RDSDriverDefinitionConstants.DB_ACCCOUNT_ID);

                if (dbInstance.getDBInstanceIdentifier().equals(profileInstanceId) &&
                    RegionUtils.getCurrentRegion().getId().equals(profileRegionId) &&
                    AwsToolkitCore.getDefault().getCurrentAccountId().equals(profileAccountId)) {

                    RDSPlugin.connectAndReveal(profile);
                    return;
                }
            }

            ImportWizard importWizard = new ImportWizard(dbInstance);
            WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(), importWizard);
            wizardDialog.open();
        }
    }
}
