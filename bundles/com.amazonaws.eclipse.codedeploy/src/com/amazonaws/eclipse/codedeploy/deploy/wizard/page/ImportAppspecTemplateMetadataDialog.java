package com.amazonaws.eclipse.codedeploy.deploy.wizard.page;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.codedeploy.appspec.AppspecTemplateRegistry;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateMetadataModel;

public class ImportAppspecTemplateMetadataDialog extends Dialog {

    private Button importButton;
    private Button browseButton;
    private Text filePathTextBox;

    public ImportAppspecTemplateMetadataDialog(Shell parentShell) {
        super(parentShell);
    }

    /**
     * To customize the dialog title
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Import Appspec Template Metadata");
    }

    /**
     * To customize the dialog button
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID,
                "Cacel", false);

        importButton = createButton(parent, IDialogConstants.CLIENT_ID,
                "Import", false);
        importButton.setEnabled(false);
        importButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                final String filepath = filePathTextBox.getText();

                new Job("Importing appspec template metadata") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        monitor.beginTask("Load and validate template metadata",
                                IProgressMonitor.UNKNOWN);

                        try {
                            final AppspecTemplateMetadataModel newTemplate = AppspecTemplateRegistry.getInstance()
                                    .importCustomTemplateMetadata(new File(filepath));

                            Display.getDefault().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    String message = String.format(
                                            "Template [%s] imported!", newTemplate.getTemplateName());
                                    MessageDialog.openInformation(
                                            ImportAppspecTemplateMetadataDialog.this.getShell(),
                                            "Import Success", message);
                                }
                            });

                        } catch (final Exception e) {
                            Display.getDefault().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    String message = "Failed to load template metadata. "
                                            + e.getMessage();
                                    if (e.getCause() != null) {
                                        message += " ( " + e.getCause().getMessage() + ")";
                                    }
                                    MessageDialog.openError(
                                            ImportAppspecTemplateMetadataDialog.this.getShell(),
                                            "Failed to load template metadata", message);
                                }
                            });
                            return Status.CANCEL_STATUS;

                        } finally {
                            Display.getDefault().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    ImportAppspecTemplateMetadataDialog.this.close();
                                }
                            });

                        }

                        monitor.done();
                        return Status.OK_STATUS;
                    }
                }.schedule();

            }
        });
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        Label label = new Label(container, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 2;
        label.setLayoutData(gridData);
        label.setText("Select the location of the template metadata file to import");

        filePathTextBox = new Text(container, SWT.BORDER | SWT.READ_ONLY);
        filePathTextBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        browseButton = new Button(container, SWT.PUSH);
        GridData rightAlign = new GridData(SWT.RIGHT, SWT.TOP, false, false);
        rightAlign.widthHint = 100;
        browseButton.setLayoutData(rightAlign);
        browseButton.setText("Browse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog dialog = new FileDialog(
                        ImportAppspecTemplateMetadataDialog.this.getShell(), SWT.OPEN);
                String filePath = dialog.open();

                if (filePath != null) {
                    filePathTextBox.setText(filePath);
                    importButton.setEnabled(true);
                }
            }
        });

        return container;
    }

}
