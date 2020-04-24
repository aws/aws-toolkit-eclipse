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
package com.amazonaws.eclipse.explorer.s3.actions;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.S3ObjectSummaryTable;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Action to generate a pre-signed URL for accessing an object.
 */
public class GeneratePresignedUrlAction extends AwsAction {

    private final S3ObjectSummaryTable table;

    public GeneratePresignedUrlAction(S3ObjectSummaryTable s3ObjectSummaryTable) {
        super(AwsToolkitMetricType.EXPLORER_S3_GENERATE_PRESIGNED_URL);
        table = s3ObjectSummaryTable;
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_HTML_DOC));
    }

    @Override
    public String getText() {
        return "Generate Pre-signed URL";
    }

    @Override
    protected void doRun() {
        DateSelectionDialog dialog = new DateSelectionDialog(Display.getDefault().getActiveShell());
        if ( dialog.open() != Window.OK ) {
            actionCanceled();
            actionFinished();
            return;
        }

        try {
            S3ObjectSummary selectedObject = table.getSelectedObjects().iterator().next();
            GeneratePresignedUrlRequest rq = new GeneratePresignedUrlRequest(selectedObject.getBucketName(),
                    selectedObject.getKey()).withExpiration(dialog.getDate());
            if ( dialog.getContentType() != null && dialog.getContentType().length() > 0 ) {
                rq.setResponseHeaders(new ResponseHeaderOverrides().withContentType(dialog.getContentType()));
            }

            URL presignedUrl = table.getS3Client().generatePresignedUrl(rq);

            final Clipboard cb = new Clipboard(Display.getDefault());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { presignedUrl.toString() }, new Transfer[] { textTransfer });
            actionSucceeded();
        } catch (Exception e) {
            actionFailed();
            AwsToolkitCore.getDefault().reportException(e.getMessage(), e);
        } finally {
            actionFinished();
        }
    }

    @Override
    public boolean isEnabled() {
        return table.getSelectedObjects().size() == 1;
    }

    private final class DateSelectionDialog extends MessageDialog {

        public String getContentType() {
            return contentType;
        }

        public Date getDate() {
            return calendar.getTime();
        }

        private Calendar calendar = Calendar.getInstance();
        private String contentType;

        @Override
        protected Control createDialogArea(Composite parent) {
            // create the top level composite for the dialog area
            Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            composite.setLayout(layout);
            GridData data = new GridData(GridData.FILL_BOTH);
            data.horizontalSpan = 2;
            composite.setLayoutData(data);
            createCustomArea(composite);
            return composite;
        }

        @Override
        protected Control createCustomArea(Composite parent) {

            Composite composite = new Composite(parent, SWT.NONE);
            GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
            composite.setLayoutData(layoutData);
            composite.setLayout(new GridLayout(1, false));

            new Label(composite, SWT.None).setText("Expiration date:");

            final DateTime calendarControl = new DateTime(composite, SWT.CALENDAR);
            calendarControl.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    calendar.set(Calendar.YEAR, calendarControl.getYear());
                    calendar.set(Calendar.MONTH, calendarControl.getMonth());
                    calendar.set(Calendar.DAY_OF_MONTH, calendarControl.getDay());
                }
            });

            Composite timeComposite = new Composite(composite, SWT.NONE);
            timeComposite.setLayout(new GridLayout(2, false));

            new Label(timeComposite, SWT.None).setText("Expiration time: ");
            final DateTime timeControl = new DateTime(timeComposite, SWT.TIME);
            timeControl.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    calendar.set(Calendar.HOUR, timeControl.getHours());
                    calendar.set(Calendar.MINUTE, timeControl.getMinutes());
                    calendar.set(Calendar.SECOND, timeControl.getSeconds());
                }
            });

            Composite contentTypeComp = new Composite(composite, SWT.None);
            contentTypeComp.setLayoutData(layoutData);
            contentTypeComp.setLayout(new GridLayout(1, false));

            new Label(contentTypeComp, SWT.None).setText("Content-type override (optional): ");
            final Text contentTypeText = new Text(contentTypeComp, SWT.BORDER);
            contentTypeText.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(ModifyEvent e) {
                    contentType = contentTypeText.getText();
                }
            });

            contentTypeText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

            return composite;
        }

        protected DateSelectionDialog(Shell parentShell) {
            super(parentShell, "Generate Presigned URL", AwsToolkitCore.getDefault().getImageRegistry()
                    .get(AwsToolkitCore.IMAGE_AWS_ICON), "Expiration date:", MessageDialog.NONE,
                    new String[] { "Copy to clipboard" }, 0);
        }

    }
}
