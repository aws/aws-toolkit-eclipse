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
package com.amazonaws.eclipse.lambda.upload.wizard.dialog;

import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;

public class CreateS3BucketDialog extends AbstractInputDialog {

    private final Region region;

    private String createdBucketName;

    public CreateS3BucketDialog(Shell parentShell, Region region) {
        super(
                parentShell,
                "Create Bucket",
                "Create an S3 bucket in " + region.getName() + " region",
                "Creating the Bucket...",
                "Bucket Name:",
                "lambda-function-bucket-" + region.getId() + "-" + System.currentTimeMillis());

        this.region = region;
    }

    public String getCreatedBucketName() {
        return createdBucketName;
    }

    @Override
    protected void performFinish(String input) {
        String regionEndpoint = region.getServiceEndpoint(ServiceAbbreviations.S3);
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientByEndpoint(regionEndpoint);

        String s3RegionName = null;
        String awsRegionId = region.getId();
        if (awsRegionId.equalsIgnoreCase("us-east-1")) {
            s3RegionName = null; // us_standard
        } else {
            s3RegionName = awsRegionId;
        }

        System.out.println(input);
        System.out.println(s3RegionName);
        s3.createBucket(new CreateBucketRequest(input, s3RegionName));

        createdBucketName = input;
    }

}
