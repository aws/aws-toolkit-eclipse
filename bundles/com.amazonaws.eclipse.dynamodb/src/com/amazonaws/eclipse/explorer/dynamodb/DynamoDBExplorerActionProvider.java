package com.amazonaws.eclipse.explorer.dynamodb;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.testtool.StartTestToolWizard;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolManager;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;

/**
 * Provides right-click context actions for items in the DynamoDB section of the
 * AWS resource explorer.
 */
public class DynamoDBExplorerActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection) getActionSite().getStructuredViewer().getSelection();
        if ( selection.size() != 1 ) {
            return;
        }

        menu.add(new CreateTableAction());

        if ( selection.getFirstElement() instanceof DynamoDBTableNode ) {
            String tableName = ((DynamoDBTableNode) selection.getFirstElement()).getTableName();
            menu.add(new DeleteTableAction(tableName));
            menu.add(new Separator());
            menu.add(new TablePropertiesAction(tableName));
        } else {
            if ("local".equals(RegionUtils.getCurrentRegion().getId())) {
                if (TestToolManager.INSTANCE.isRunning()) {
                    menu.add(new StopTestToolAction());
                } else if (TestToolManager.INSTANCE.isJava7Available()) {
                    menu.add(new StartTestToolAction());
                }
            }
        }
    }

    private static class StartTestToolAction extends Action {
        @Override
        public String getDescription() {
            return "Start the DynamoDB Local Test Tool";
        }

        @Override
        public String getToolTipText() {
            return getDescription();
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_DATABASE);
        }

        @Override
        public String getText() {
            return "Start DynamoDB Local";
        }

        @Override
        public void run() {
            WizardDialog dialog = new WizardDialog(
                Display.getCurrent().getActiveShell(),
                new StartTestToolWizard()
            );
            dialog.open();

            if (null != DynamoDBContentProvider.getInstance()) {
                DynamoDBContentProvider.getInstance().refresh();
            }
        }
    }

    private static class StopTestToolAction extends Action {
        @Override
        public String getDescription() {
            return "Stop the DynamoDB Local Test Tool";
        }

        @Override
        public String getToolTipText() {
            return getDescription();
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_DATABASE);
        }

        @Override
        public String getText() {
            return "Stop DynamoDB Local";
        }

        @Override
        public void run() {
            TestToolManager.INSTANCE.stopVersion();
            if (null != DynamoDBContentProvider.getInstance()) {
                DynamoDBContentProvider.getInstance().refresh();
            }
        }
    }

    private static class CreateTableAction extends Action {

        @Override
        public String getDescription() {
            return "Create a new table";
        }

        @Override
        public String getToolTipText() {
            return getDescription();
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD);
        }

        @Override
        public String getText() {
            return "Create Table";
        }

        @Override
        public void run() {
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateTableWizard());
            dialog.open();

            if ( null != DynamoDBContentProvider.getInstance() ) {
                DynamoDBContentProvider.getInstance().refresh();
            }
        }
    }

    private static class DeleteTableAction extends Action {

        private final String tableName;

        public DeleteTableAction(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String getDescription() {
            return "Delete table";
        }

        @Override
        public ImageDescriptor getImageDescriptor() {
            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
        }

        @Override
        public String getToolTipText() {
            return getDescription();
        }

        @Override
        public String getText() {
            return "Delete table";
        }

        @Override
        public void run() {
            if ( new DeleteTableConfirmation().open() == 0 ) {
                final String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
                new Job("Deleting table") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            AwsToolkitCore.getClientFactory(accountId).getDynamoDBV2Client()
                                    .deleteTable(new DeleteTableRequest().withTableName(tableName));
                        } catch ( AmazonClientException e ) {
                            return new Status(IStatus.ERROR, DynamoDBPlugin.PLUGIN_ID, "Failed to delete table", e);
                        }

                        if ( null != DynamoDBContentProvider.getInstance() ) {
                            DynamoDBContentProvider.getInstance().refresh();
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

    private static class TablePropertiesAction extends Action {

        private final String tableName;

        public TablePropertiesAction(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String getDescription() {
            return getText();
        }

        @Override
        public String getToolTipText() {
            return getDescription();
        }

        @Override
        public String getText() {
            return "Table Properties";
        }

        @Override
        public void run() {
            final TablePropertiesDialog tablePropertiesDialog = new TablePropertiesDialog(tableName);
            if (tablePropertiesDialog.open() == 0) {
                final String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
                new Job("Updating table " + tableName) {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            AwsToolkitCore.getClientFactory(accountId).getDynamoDBV2Client()
                                    .updateTable(tablePropertiesDialog.getUpdateRequest());
                        } catch ( AmazonClientException e ) {
                            return new Status(IStatus.ERROR, DynamoDBPlugin.PLUGIN_ID, "Failed to update table", e);
                        }

                        return Status.OK_STATUS;
                    }

                }.schedule();
            }
        }
    }

    private static class DeleteTableConfirmation extends MessageDialog {

        public DeleteTableConfirmation() {
            super(Display.getCurrent().getActiveShell(), "Confirm table deletion", AwsToolkitCore.getDefault()
                    .getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                    "Are you sure you want to delete this table?", MessageDialog.CONFIRM,
                    new String[] { "OK", "Cancel" }, 0);
        }

    }
}
