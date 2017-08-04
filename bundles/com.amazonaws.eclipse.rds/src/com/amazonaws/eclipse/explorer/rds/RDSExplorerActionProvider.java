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
package com.amazonaws.eclipse.explorer.rds;

import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.explorer.rds.RDSExplorerNodes.RdsRootElement;
import com.amazonaws.eclipse.rds.ImportWizard;
import com.amazonaws.eclipse.rds.RDSDriverDefinitionConstants;
import com.amazonaws.eclipse.rds.RDSPlugin;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

public class RDSExplorerActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
        if (selection.getFirstElement() instanceof RdsRootElement) {
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
        private DBInstance dbInstance;

        public ConfigureConnectionProfileAction(DBInstance dbInstance) {
            this.dbInstance = dbInstance;
            this.setText("Connect...");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_GEAR));
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

            AmazonRDS rds = AwsToolkitCore.getClientFactory().getRDSClient();
            DescribeDBInstancesResult result = rds.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstance.getDBInstanceIdentifier()));
            if (result.getDBInstances().isEmpty()) {
                String title = "DB Instance Not Available";
                String message = "The DB Instance you selected is no longer available.";
                openErrorDialog(title, message);
                return;
            }

            dbInstance = result.getDBInstances().get(0);
            if (dbInstance.getPubliclyAccessible() == false) {
                String title = "DB Instance Not Publicly Accessible";
                String message = "The DB Instance you selected is not publically accessible.  "
                        + "For more information about making your DB Instance publically accessible, see the Amazon RDS Developer Guide.";
                openErrorDialog(title, message);
                return;
            }

            ImportWizard importWizard = new ImportWizard(dbInstance);
            WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(), importWizard);
            wizardDialog.open();
        }
    }

    private static void openErrorDialog(String title, String message) {
        new MessageDialog(Display.getDefault().getActiveShell(),
                title, null, message, MessageDialog.ERROR, new String[] { "OK" }, 0).open();
    }
}
