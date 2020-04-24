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
package com.amazonaws.eclipse.explorer.s3;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.acls.EditBucketPermissionsDialog;
import com.amazonaws.eclipse.explorer.s3.acls.EditPermissionsDialog;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class BucketEditor extends EditorPart {

    public final static String ID = "com.amazonaws.eclipse.explorer.s3.bucketEditor";

    private BucketEditorInput bucketEditorInput;

    private S3ObjectSummaryTable objectSummaryTable;

    public S3ObjectSummaryTable getObjectSummaryTable() {
        return objectSummaryTable;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        bucketEditorInput = (BucketEditorInput) input;
        setPartName(input.getName());
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(bucketEditorInput.getBucketName());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_BUCKET));
        form.getBody().setLayout(new GridLayout(1, false));

        createBucketSummary(form, toolkit, bucketEditorInput.getBucketName());
        createBucketObjectList(form, toolkit, bucketEditorInput.getBucketName());

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().update(true);
    }

    private class RefreshAction extends AwsAction {
        public RefreshAction() {
            super(AwsToolkitMetricType.EXPLORER_S3_REFRESH_BUCKET_EDITOR);
            this.setText("Refresh");
            this.setToolTipText("Refresh bucket contents");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        protected void doRun() {
            objectSummaryTable.refresh(null);
            actionFinished();
        }
    }

    /**
     * Creates a table of buckets
     */
    private void createBucketObjectList(final ScrolledForm form, final FormToolkit toolkit, final String bucketName) {
        objectSummaryTable = new S3ObjectSummaryTable(bucketEditorInput.getAccountId(),
                bucketEditorInput.getBucketName(), bucketEditorInput.getRegionEndpoint(), form.getBody(), toolkit,
                SWT.None);
        objectSummaryTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    /**
     * Creates a summary of a bucket
     */
    private void createBucketSummary(final ScrolledForm form, final FormToolkit toolkit, final String bucketName) {

        final Composite parent = toolkit.createComposite(form.getBody(), SWT.None);
        parent.setLayout(new GridLayout(2, false));

        toolkit.createLabel(parent, "Bucket info loading");
        toolkit.createLabel(parent, "");

        new Thread() {
            @Override
            public void run() {
                Bucket bucket = null;
                // TODO: We don't need to list all the buckets just to get one
                for ( Bucket b : AwsToolkitCore.getClientFactory().getS3Client().listBuckets() ) {
                    if ( b.getName().equals(bucketEditorInput.getName()) ) {
                        bucket = b;
                        break;
                    }
                }

                if ( bucket == null )
                    return;

                updateComposite(form, toolkit, bucket);
            }

            protected void updateComposite(final ScrolledForm form, final FormToolkit toolkit, final Bucket b) {
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        for ( Control c : parent.getChildren() ) {
                            c.dispose();
                        }

                        toolkit.createLabel(parent, "Owner: ");
                        toolkit.createLabel(parent, b.getOwner().getDisplayName());
                        toolkit.createLabel(parent, "Creation Date: ");
                        toolkit.createLabel(parent, b.getCreationDate().toString());
                        Button editBucketAclButton = toolkit.createButton(parent, "Edit Bucket ACL", SWT.PUSH);
                        editBucketAclButton.addSelectionListener(new SelectionListener() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                final EditPermissionsDialog editPermissionsDialog = new EditBucketPermissionsDialog(b);
                                if (editPermissionsDialog.open() == 0) {
                                    Region region = RegionUtils.getRegionByEndpoint(bucketEditorInput.getRegionEndpoint());
                                    final AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientByRegion(region.getId());
                                    new Job("Updating bucket ACL") {
                                        @Override
                                        protected IStatus run(IProgressMonitor monitor) {
                                            try {
                                                s3.setBucketAcl(b.getName(), editPermissionsDialog.getAccessControlList());
                                            } catch (AmazonClientException ace) {
                                                AwsToolkitCore.getDefault().reportException("Unable to update bucket ACL", ace);
                                            }

                                            return Status.OK_STATUS;
                                        }
                                    }.schedule();
                                }
                            }

                            @Override
                            public void widgetDefaultSelected(SelectionEvent e) {}
                        });

                        form.reflow(true);
                    }
                });
            }

        }.start();
    }

    @Override
    public void setFocus() {
    }
}
