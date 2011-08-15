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

import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.s3.S3ContentProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;

class CreateBucketWizard extends Wizard {

    private CreateBucketWizardPage page;

    public CreateBucketWizard() {
        super();
        page = new CreateBucketWizardPage();
    }

    @Override
    public void addPages() {
        addPage(page);
    }

    @Override
    public int getPageCount() {
        return 1;
    }

    @Override
    public boolean needsPreviousAndNextButtons() {
        return false;
    }

    @Override
    public boolean performFinish() {
        AmazonS3 client = AwsToolkitCore.getClientFactory().getS3Client();
        client.createBucket(new CreateBucketRequest(page.getBucketName()));
        S3ContentProvider.getInstance().refresh();
        return true;
    }
}