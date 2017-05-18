/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core.ui;

import static com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel.LOADING;
import static com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel.NONE_FOUND;
import static com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel.P_BUCKET;

import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.CoreAnalytics;
import com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.ui.dialogs.CreateS3BucketDialog;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.util.S3BucketUtil;
import com.amazonaws.eclipse.core.validator.SelectBucketValidator;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

/**
 * A basic composite that includes a combo box and a button which could be used to select
 * a bucket from the combo box, or create a new one by the button.
 */
public class SelectOrCreateBucketComposite extends Composite {

    private final DataBindingContext bindingContext;
    private final SelectOrCreateBucketDataModel dataModel;
    private final String labelValue = "S3 Bucket Name:";
    private final String buttonValue = "Create";
    private final SelectBucketValidator validator = new SelectBucketValidator();

    private Region currentRegion;
    private IObservableValue bucketNameLoadedObservable = new WritableValue();
    private LoadS3BucketsInFunctionRegionThread loadS3BucketsInFunctionRegionThread;

    private ComboViewerComplex<Bucket> selectCombo;
    private Button createButton;

    public SelectOrCreateBucketComposite(
            Composite parent,
            DataBindingContext bindingContext,
            SelectOrCreateBucketDataModel dataModel) {
        super(parent, SWT.NONE);
        this.bindingContext = bindingContext;
        this.dataModel = dataModel;
        this.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        this.setLayout(new GridLayout(3, false));
        createControls();
    }

    public void refreshBucketsInRegion(Region currentRegion, String defaultBucket) {
        this.currentRegion = currentRegion;
        validator.setRegion(currentRegion);
        bucketNameLoadedObservable.setValue(false);

        if (selectCombo != null) {
            selectCombo.getComboViewer().setInput(new Bucket[] { LOADING });
            selectCombo.getComboViewer().setSelection(new StructuredSelection(LOADING));
            selectCombo.getComboViewer().getCombo().setEnabled(false);
        }

        CancelableThread.cancelThread(loadS3BucketsInFunctionRegionThread);

        loadS3BucketsInFunctionRegionThread = new LoadS3BucketsInFunctionRegionThread(defaultBucket);
        loadS3BucketsInFunctionRegionThread.start();
    }

    private void createControls() {
        this.selectCombo = ComboViewerComplex.<Bucket>builder()
            .composite(this)
            .bindingContext(bindingContext)
            .labelValue(labelValue)
            .pojoObservableValue(PojoProperties.value(
                    SelectOrCreateBucketDataModel.class, P_BUCKET, Bucket.class)
                    .observe(dataModel))
            .labelProvider(new LabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof Bucket) {
                        Bucket bucket = (Bucket) element;
                        return bucket.getName();
                    }
                    return super.getText(element);
                }
            })
            .validator(validator)
            .build();
        this.selectCombo.getComboViewer().getCombo().setEnabled(false);

        this.createButton = WizardWidgetFactory.newPushButton(this, buttonValue);
        this.createButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onCreateButtonSelected();
            }
        });
    }

    private void onCreateButtonSelected() {
        CreateS3BucketDialog dialog = new CreateS3BucketDialog(
                Display.getCurrent().getActiveShell(), currentRegion);
        int returnCode = dialog.open();

        if (returnCode == 0) {
            Bucket bucket = dialog.getCreatedBucket();
            dataModel.setCreateNewBucket(true);

            if (bucketNameLoadedObservable.getValue().equals(Boolean.TRUE)) {
                selectCombo.getComboViewer().add(bucket);
                selectCombo.getComboViewer().setSelection(new StructuredSelection(bucket));
            } else {
                CancelableThread.cancelThread(loadS3BucketsInFunctionRegionThread);
                bucketNameLoadedObservable.setValue(true);

                selectCombo.getComboViewer().setInput(new Bucket[] {bucket});
                selectCombo.getComboViewer().setSelection(new StructuredSelection(bucket));
                selectCombo.getComboViewer().getCombo().setEnabled(true);
            }
        }
    }

    private final class LoadS3BucketsInFunctionRegionThread extends CancelableThread {

        private final String defaultBucket;

        /**
         * @param defaultBucket
         *            the bucket that should be selected by default after all
         *            buckets are loaded.
         */
        LoadS3BucketsInFunctionRegionThread(String defaultBucket) {
            this.defaultBucket = defaultBucket;
        }

        @Override
        public void run() {
            AmazonS3 s3 = AwsToolkitCore.getClientFactory()
                    .getS3ClientByRegion(currentRegion.getId());
            long startTime = System.currentTimeMillis();
            final List<Bucket> bucketsInFunctionRegion = S3BucketUtil.listBucketsInRegion(s3, currentRegion);
            CoreAnalytics.trackLoadBucketTimeDuration(System.currentTimeMillis() - startTime);

            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    try {
                        synchronized (LoadS3BucketsInFunctionRegionThread.this) {
                            if (!isCanceled()) {
                                if (bucketsInFunctionRegion.isEmpty()) {
                                    selectCombo.getComboViewer().setInput(new Bucket[] { NONE_FOUND });
                                    selectCombo.getComboViewer().setSelection(new StructuredSelection(NONE_FOUND));
                                    bucketNameLoadedObservable.setValue(false);
                                } else {
                                    Bucket defaultBucket = findDefaultBucket(bucketsInFunctionRegion);
                                    if (defaultBucket == null) {
                                        defaultBucket = bucketsInFunctionRegion.get(0);
                                    }
                                    selectCombo.getComboViewer().setInput(bucketsInFunctionRegion);
                                    selectCombo.getComboViewer().setSelection(new StructuredSelection(defaultBucket));
                                    selectCombo.getComboViewer().getCombo().setEnabled(true);
                                    bucketNameLoadedObservable.setValue(true);
                                }
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }

        private Bucket findDefaultBucket(List<Bucket> buckets) {
            if (defaultBucket == null) {
                return null;
            }
            for (Bucket bucket : buckets) {
                if (bucket.getName().equals(defaultBucket)) {
                    return bucket;
                }
            }
            return null;
        }
    }
}
