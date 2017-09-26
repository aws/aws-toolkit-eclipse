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
package com.amazonaws.eclipse.core.model;

import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.util.S3BucketUtil;
import com.amazonaws.services.s3.model.Bucket;

public class SelectOrCreateBucketDataModel extends SelectOrCreateDataModel<Bucket, AwsResourceScopeParamBase> {

    private static final String RESOURCE_TYPE = "Bucket";
    private static final Bucket LOADING = new Bucket("Loading...");
    private static final Bucket NONE_FOUND = new Bucket("None found");
    private static final Bucket ERROR = new Bucket("Error Loading Buckets");

    public String getBucketName() {
        return this.getExistingResource().getName();
    }

    @Override
    public Bucket getLoadingItem() {
        return LOADING;
    }

    @Override
    public Bucket getNotFoundItem() {
        return NONE_FOUND;
    }

    @Override
    public Bucket getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getDefaultResourceName() {
        return "lambda-function-bucket-" + System.currentTimeMillis();
    }

    @Override
    public List<Bucket> loadAwsResources(AwsResourceScopeParamBase param) {
        return S3BucketUtil.listBucketsInRegion(
                AwsToolkitCore.getClientFactory(param.getAccountId()).getS3ClientByRegion(param.getRegionId()),
                RegionUtils.getRegion(param.getRegionId()));
    }

    @Override
    public String getResourceName(Bucket resource) {
        return resource.getName();
    }
}