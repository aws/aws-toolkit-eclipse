/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.ui.dialogs;

import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CreateBucketRequest;

public class CreateS3BucketDialog extends AbstractInputDialog<Bucket> {

    private final AwsResourceScopeParamBase param;
    private Bucket createdBucket;

    public CreateS3BucketDialog(Shell parentShell, AwsResourceScopeParamBase param) {
        super(
                parentShell,
                "Create Bucket",
                "Create an S3 bucket in " + RegionUtils.getRegion(param.getRegionId()) + " region",
                "Creating the Bucket...",
                "Bucket Name:",
                "lambda-function-bucket-" + param.getRegionId() + "-" + System.currentTimeMillis());

        this.param = param;
    }

    public Bucket getCreatedBucket() {
        return getCreatedResource();
    }

    @Override
    protected void performFinish(String input) {
        AmazonS3 s3 = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getS3ClientByRegion(param.getRegionId());

        String regionId = param.getRegionId();
        String s3RegionName = regionId.equalsIgnoreCase("us-east-1") ? null : regionId;

        createdBucket = s3.createBucket(new CreateBucketRequest(input, s3RegionName));
    }

    @Override
    public Bucket getCreatedResource() {
        return createdBucket;
    }
}
