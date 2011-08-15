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
package com.amazonaws.eclipse.explorer.s3.actions;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;

class CreateBucketWizardPage extends WizardPage {

    private static final BucketNameUtils bucketNameUtils = new BucketNameUtils();
    private static final Pattern bucketNamePattern = Pattern.compile("[a-z0-9][a-z0-9\\.\\_\\-]{2,254}");

    private IObservableValue bucketNameObservable = new WritableValue();
    private IObservableSet unavailableBucketNames = new WritableSet();
    private IObservableSet availableBucketNames = new WritableSet();
    private DataBindingContext bindingContext = new DataBindingContext();
    private AggregateValidationStatus validationStatus = new AggregateValidationStatus(bindingContext,
            AggregateValidationStatus.MAX_SEVERITY);
    private boolean complete = false;

    private ListBucketThread listBucketThread;

    protected CreateBucketWizardPage() {
        super("Create new bucket", "Create new bucket", AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor("aws-logo"));
    }

    public String getBucketName() {
        return (String) bucketNameObservable.getValue();
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.None);
        label.setText("Bucket name: ");

        Text text = new Text(composite, SWT.BORDER);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        text.setLayoutData(data);

        bindingContext.bindValue(bucketNameObservable, SWTObservables.observeText(text, SWT.Modify));

        MultiValidator validationStatusProvider = new MultiValidator() {

            @Override
            protected IStatus validate() {
                String bucketName = (String) bucketNameObservable.getValue();
                if ( bucketName == null || bucketName.length() == 0 ) {
                    return ValidationStatus.error("Please enter a bucket name");
                }

                try {
                    bucketNameUtils.validateBucketName(bucketName);
                } catch ( IllegalArgumentException e ) {
                    return ValidationStatus.error(e.getMessage());
                }

                if ( !bucketNamePattern.matcher(bucketName).matches() ) {
                    return ValidationStatus
                            .error("Bucket name does not match requirements.  See http://docs.amazonwebservices.com/AmazonS3/latest/dev/");
                }

                if ( availableBucketNames.contains(bucketName) ) {
                    return ValidationStatus.ok();
                }

                if ( unavailableBucketNames.contains(bucketName) ) {
                    return ValidationStatus.error("Bucket name in use");
                }

                listBucket(bucketName);

                return ValidationStatus.error("Validating bucket name");
            }
        };

        validationStatusProvider.observeValidatedSet(availableBucketNames);
        validationStatusProvider.observeValidatedSet(unavailableBucketNames);

        bindingContext.addValidationStatusProvider(validationStatusProvider);

        ControlDecoration decoration = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());

        validationStatus.addChangeListener(new IChangeListener() {

            public void handleChange(ChangeEvent event) {
                Object value = validationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                setMessage(status.getMessage(), status.getSeverity());

                setComplete(status.isOK());
            }

        });

        setControl(composite);
    }

    private void listBucket(String bucketName) {
        if ( listBucketThread != null ) {
            synchronized (listBucketThread) {
                listBucketThread.cancel();
            }
        }
        listBucketThread = new ListBucketThread(bucketName, availableBucketNames, unavailableBucketNames);
        listBucketThread.start();
    }

    @Override
    public boolean isPageComplete() {
        return complete;
    }

    private void setComplete(boolean complete) {
        this.complete = complete;
        if ( getWizard().getContainer() != null )
            getWizard().getContainer().updateButtons();
    }
}

final class DecorationChangeListener implements IValueChangeListener {

    private final ControlDecoration decoration;

    public DecorationChangeListener(ControlDecoration decoration, IObservableValue observableValue) {
        this.decoration = decoration;
        observableValue.addValueChangeListener(this);
        updateDecoration((IStatus) observableValue.getValue());
    }

    public void handleValueChange(ValueChangeEvent event) {
        IStatus status = (IStatus) event.getObservableValue().getValue();
        updateDecoration(status);
    }

    private void updateDecoration(IStatus status) {
        if ( status.isOK() ) {
            decoration.hide();
        } else {
            decoration.setDescriptionText(status.getMessage());
            decoration.show();
        }
    }
}

final class ListBucketThread extends Thread {

    private boolean isCanceled;
    private final String bucketName;
    private final IObservableSet availableBucketNames;
    private final IObservableSet unavailableBucketNames;

    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    public synchronized void cancel() {
        this.isCanceled = true;
    }

    public ListBucketThread(String bucketName, IObservableSet availableBucketNames,
            IObservableSet unavailableBucketNames) {
        super();
        this.bucketName = bucketName;
        this.availableBucketNames = availableBucketNames;
        this.unavailableBucketNames = unavailableBucketNames;
    }

    @Override
    public void run() {
        AmazonS3 client = AwsToolkitCore.getClientFactory().getS3Client();
        boolean available = false;
        try {
            client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withMaxKeys(0));
        } catch ( AmazonS3Exception e ) {
            if ( e.getErrorCode() != null && e.getErrorCode().equals("NoSuchBucket") ) {
                available = true;
            }
        }

        final boolean isAvailable = available;
        synchronized (this) {
            if ( !isCanceled ) {
                Display.getDefault().syncExec(new Runnable() {

                    public void run() {
                        if ( isAvailable ) {
                            availableBucketNames.add(bucketName);
                        } else {
                            unavailableBucketNames.add(bucketName);
                        }
                    }
                });
            }
        }
    }
}
