/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.diagnostic.ui;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorSupportProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.AbstractStatusAreaProvider;
import org.eclipse.ui.statushandlers.StatusAdapter;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.diagnostic.utils.AwsErrorReportUtils;
import com.amazonaws.eclipse.core.diagnostic.utils.EmailMessageLauncher;
import com.amazonaws.eclipse.core.diagnostic.utils.PlatformEnvironmentDataCollector;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.ui.EmailLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.ui.overview.Toolkit;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.services.errorreport.model.ErrorDataModel;
import com.amazonaws.services.errorreport.model.ErrorReportDataModel;

/**
 * A custom AbstractStatusAreaProvider implementation that provides additional
 * UI components for users to directly report errors that are associated with
 * "com.amazonaws.*" plugins.
 *
 * NOTE: this class won't work if it directly extends ErrorSupportProvider,
 * since the default workbench error dialog will only check the
 * {@link #validFor(StatusAdapter)} method if it finds an instance of
 * AbstractStatusAreaProvider.
 *
 * @see http://wiki.eclipse.org/Status_Handling_Best_Practices#
 *      Developing_an_ErrorSupportProvider
 *
 * @see "org.eclipse.ui.internal.statushandlers.InternalDialog"
 * @see "org.eclipse.ui.internal.statushandlers.SupportTray"
 */
public class AwsToolkitErrorSupportProvider extends AbstractStatusAreaProvider {

    /** http://www.regular-expressions.info/email.html */
    private static final String VALID_EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$";

    private static final String COM_DOT_AMAZONAWS_DOT = "com.amazonaws.";

    /**
     * The ErrorSupportProvider that was previously configured in the JFace
     * Policy. Status events that don't match the criteria of
     * AwsToolkitErrorSupportProvider will be handled by this provider instead
     * (it it's not null).
     */
    private final ErrorSupportProvider overriddenProvider;

    public AwsToolkitErrorSupportProvider(ErrorSupportProvider overriddenProvider) {
        this.overriddenProvider = overriddenProvider;
    }

    /**
     * Returns true if the given status should be processed by either the
     * AWS-specific error reporting support or the ErrorSupportProvider that was
     * overridden by this class.
     * <p>
     * The workbench's internal error dialog won't create the support area if
     * this method returns false.
     *
     * @see AbstractStatusAreaProvider#validFor(StatusAdapter)
     */
    @Override
    public boolean validFor(StatusAdapter statusAdapter) {
        IStatus status = statusAdapter.getStatus();
        if (status == null) return false;

        return isAwsErrorStatus(status) || validForOverriddenProvider(status);

    }

    /**
     * Returns true if the status has ERROR-level severity and that it's
     * associated with "com.amazonaws.*" plugins.
     */
    private static boolean isAwsErrorStatus(IStatus status) {
        return status != null
               &&
               status.getSeverity() == Status.ERROR
               &&
               status.getPlugin() != null
               &&
               status.getPlugin().startsWith(COM_DOT_AMAZONAWS_DOT);
    }

    /**
     * Returns true if the status is valid for the error provider that was
     * overridden by this provider.
     */
    private boolean validForOverriddenProvider(IStatus status) {
        if (overriddenProvider == null)
            return false;

        // ErrorSupportProvider#validFor(IStatus) is not added to Eclipse
        // platform until 3.7 version.
        try {
            Method validFor = overriddenProvider.getClass().getMethod("validFor", IStatus.class);
            return (Boolean) validFor.invoke(overriddenProvider, status);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create the custom support area that will be injected in the workbench
     * default error dialog.
     */
    @Override
    public Control createSupportArea(Composite parent, final StatusAdapter statusAdapter) {
        final IStatus status = statusAdapter.getStatus();

        // If the status is not associated with "com.amazonaws.*", we check
        // whether it should be handled by the overridden provider.
        if ( !isAwsErrorStatus(status) ) {
            return validForOverriddenProvider(status)?
                    overriddenProvider.createSupportArea(parent, status)
                    :
                    parent;
        }

        GridData parentData = new GridData(SWT.FILL, SWT.FILL, true, true);
        parentData.widthHint = 300;
        parentData.minimumWidth = 300;
        parentData.heightHint = 250;
        parentData.minimumHeight = 250;
        parent.setLayoutData(parentData);

        GridLayout layout = new GridLayout(1, false);
        layout.marginBottom = 15;
        parent.setLayout(layout);

        Group userInputGroup = new Group(parent, SWT.NONE);

        GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, true);
        userInputGroup.setLayoutData(groupData);

        userInputGroup.setLayout(new GridLayout(1, false));


        /* User email input */
        final Label label_email = new Label(userInputGroup, SWT.WRAP);
        label_email.setText("(Optional) Please provide a valid email:");
        label_email.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        final Text email = new Text(userInputGroup, SWT.BORDER | SWT.SINGLE);
        email.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Pre-populate the default user email if it's found in the preference store
        email.setText(AwsToolkitCore.getDefault().getPreferenceStore()
                .getString(PreferenceConstants.P_ERROR_REPORT_DEFAULT_USER_EMAIL));


        /* User description input */
        Label label_description = new Label(userInputGroup, SWT.WRAP);
        label_description.setText("(Optional) Please provide more details to help us investigate:");
        label_description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        final Text description = new Text(userInputGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        WizardWidgetFactory.newLink(userInputGroup, new WebLinkListener(), String.format(
                "You can also cut a <a href=\"%s\">Github Issue</a> for tracking the problem. Since it will be public, please exclude sensitive data from the report.",
                "https://github.com/aws/aws-toolkit-eclipse/issues/new"), 1);

        /* OK button */
        final Button reportBtn = new Button(parent, SWT.PUSH);
        reportBtn.setText("Report this bug to AWS");
        reportBtn.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

        reportBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                final String userEmail = email.getText();
                final String userDescription = description.getText();

                Job job = new Job("Sending error report to AWS...") {

                    @Override
                    protected IStatus run(IProgressMonitor arg0) {

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                        final ErrorReportDataModel errorReportDataModel = new ErrorReportDataModel()
                                .error(new ErrorDataModel()
                                        .stackTrace(AwsErrorReportUtils.getStackTraceFromThrowable(status.getException()))
                                        .errorMessage(status.getMessage()))
                                .platformData(PlatformEnvironmentDataCollector.getData())
                                .userEmail(userEmail)
                                .userDescription(userDescription)
                                .timeOfError(sdf.format(new Date()));

                        if (status.getException() instanceof AwsActionException) {
                            errorReportDataModel.setCommandRun(((AwsActionException) status.getException()).getActionName());
                        }

                        try {
                            AwsErrorReportUtils.reportBugToAws(errorReportDataModel);
                        } catch (Exception error) {
                            // Show a message box with mailto: link as fallback
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    showFailureDialog(Display.getDefault().getActiveShell(), errorReportDataModel);
                                }
                            });

                            AwsToolkitCore.getDefault().logInfo(
                                    "Unable to send error report. " + error.getMessage());
                            return Status.CANCEL_STATUS;
                        }

                        // If success, save the email as default
                        AwsToolkitCore.getDefault().getPreferenceStore().setValue(
                                PreferenceConstants.P_ERROR_REPORT_DEFAULT_USER_EMAIL,
                                userEmail);

                        // Show a confirmation message
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                showSuccessDialog(Display.getDefault().getActiveShell());
                            }
                        });

                        AwsToolkitCore.getDefault().logInfo("Successfully sent the error report.");

                        return Status.OK_STATUS;
                    }
                };

                job.setUser(true);
                job.schedule();
                reportBtn.setEnabled(false);
            }
        });

        // Add simple validation to the email field
        email.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                String userInput = email.getText();

                // It's either empty, or a valid email address
                if (userInput.isEmpty()
                        || userInput.matches(VALID_EMAIL_REGEX)) {
                    label_email.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                    reportBtn.setEnabled(true);
                } else {
                    label_email.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    reportBtn.setEnabled(false);
                }
            }
        });

        return parent;
    }

    private static void showSuccessDialog(Shell parentShell) {
        MessageDialog.openInformation(parentShell,
                "Successfully sent error report",
                "Thanks for reporting the error. " +
                "Our team will investigate this as soon as possible.");
    }

    private static void showFailureDialog(Shell parentShell, ErrorReportDataModel errorData) {
        MessageDialog dialog = new ErrorReportFailureMessageDialog(parentShell,
                errorData);
        dialog.open();
    }

    private static class ErrorReportFailureMessageDialog extends MessageDialog {

        private static final String COPY_TO_CLIPBOARD_LABEL = "Copy error report data to clipboard";

        private final ErrorReportDataModel errorData;

        public ErrorReportFailureMessageDialog(Shell parentShell, ErrorReportDataModel errorData) {
            super(parentShell,
                    "Failed to send error report",
                    AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                    "Failed to send error report data to AWS.",
                    MessageDialog.NONE, new String[] { "Ok", COPY_TO_CLIPBOARD_LABEL }, 0);

            this.errorData = errorData;
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            // Add the mailto: link
            Link link = new Link(parent, SWT.WRAP);
            link.setText("Please contact us via email "
                    + Toolkit.createAnchor(
                            EmailMessageLauncher.AWS_ECLIPSE_ERRORS_AT_AMZN,
                            EmailMessageLauncher.AWS_ECLIPSE_ERRORS_AT_AMZN));

            WizardWidgetFactory.newLink(parent, new WebLinkListener(), String.format(
                    "You can also cut a <a href=\"%s\">Github Issue</a> for tracking the problem. Since it will be public, please exclude sensitive data from the report.",
                    "https://github.com/aws/aws-toolkit-eclipse/issues/new"), 1);

            EmailLinkListener emailLinkListener
                = new EmailLinkListener(EmailMessageLauncher.createEmptyErrorReportEmail());
            link.addListener(SWT.Selection, emailLinkListener);

            return parent;
        }

        /**
         * We need to override this method in order to suppress closing the
         * dialog after the user clicks the
         * "Copy error report data to clipboard" button.
         */
        @Override
        protected void buttonPressed(int buttonId) {
            if (buttonId == 1) {
                Clipboard clipboard = null;
                try {
                    clipboard = new Clipboard(Display.getDefault());
                    clipboard.setContents(new Object[] { errorData.toString() },
                            new Transfer[] { TextTransfer.getInstance() });
                } finally {
                    if (clipboard != null) {
                        clipboard.dispose();
                    }
                }

                return;
            }

            super.buttonPressed(buttonId);
        }
    }

}
