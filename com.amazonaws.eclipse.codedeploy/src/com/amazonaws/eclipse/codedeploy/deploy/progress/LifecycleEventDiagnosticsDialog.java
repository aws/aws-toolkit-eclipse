package com.amazonaws.eclipse.codedeploy.deploy.progress;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.services.codedeploy.model.Diagnostics;

public class LifecycleEventDiagnosticsDialog extends Dialog {

    private final Diagnostics lifecycleEventDiagnostics;

    private static final int WIDTH_HINT = 300;

    public LifecycleEventDiagnosticsDialog(Shell parentShell,
            Diagnostics lifecycleEventDiagnostics) {
        super(parentShell);

        if (lifecycleEventDiagnostics == null) {
            throw new NullPointerException("lifecycleEventDiagnostics must not be null.");
        }
        this.lifecycleEventDiagnostics = lifecycleEventDiagnostics;
    }

    /**
     * To customize the dialog title
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Lifecycle Event Diagnostic Information");
    }

    /**
     * To customize the dialog button
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID,
                "Close", false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);

        Label label_ErrorCode = new Label(container, SWT.NONE);
        label_ErrorCode.setText("Error code: " + toUIString(lifecycleEventDiagnostics.getErrorCode()));

        Label label_ErrorMessage_Title = new Label(container, SWT.NONE);
        label_ErrorMessage_Title.setText("Error message: ");

        Text text_ErrorMessage = new Text(container, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        GridData gridData0 = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData0.heightHint = 100;
        gridData0.widthHint = WIDTH_HINT;
        text_ErrorMessage.setLayoutData(gridData0);
        text_ErrorMessage.setText(toUIString(lifecycleEventDiagnostics.getMessage()));

        Label label_ScriptName = new Label(container, SWT.NONE);
        label_ScriptName.setText("Script name: " + toUIString(lifecycleEventDiagnostics.getScriptName()));

        Label label_LogTail_Title = new Label(container, SWT.NONE);
        label_LogTail_Title.setText("Log tail: ");

        Text text_LogTail = new Text(container, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
        GridData gridData1 = new GridData(SWT.FILL, SWT.TOP, true, true);
        gridData1.heightHint = 250;
        gridData1.widthHint = WIDTH_HINT;
        text_LogTail.setLayoutData(gridData1);
        text_LogTail.setText(toUIString(lifecycleEventDiagnostics.getLogTail()));

        return container;
    }

    private static String toUIString(Object object) {
        return object == null ? "n/a" : object.toString();
    }

}
