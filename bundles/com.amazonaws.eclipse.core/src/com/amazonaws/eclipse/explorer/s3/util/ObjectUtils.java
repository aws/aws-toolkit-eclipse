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
package com.amazonaws.eclipse.explorer.s3.util;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;

/**
 * Utilities for common Amazon S3 object operations.
 */
public class ObjectUtils {

    /**
     * Deletes an object along with all object versions, if any exist.
     */
    public void deleteObjectAndAllVersions(String bucketName, String key) {
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucketName);
        VersionListing versionListing = null;
        do {
            if (versionListing == null) {
                versionListing = s3.listVersions(bucketName, key);
            } else {
                versionListing = s3.listNextBatchOfVersions(versionListing);
            }

            for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                s3.deleteVersion(bucketName, key, versionSummary.getVersionId());
            }
        } while (versionListing.isTruncated());
    }


    /**
     * Deletes a bucket along with all contained objects and any object versions if they exist.
     */
    public void deleteBucketAndAllVersions(String bucketName) {
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucketName);
        VersionListing versionListing = null;
        do {
            if (versionListing == null) {
                versionListing = s3.listVersions(bucketName, null);
            } else {
                versionListing = s3.listNextBatchOfVersions(versionListing);
            }

            for (S3VersionSummary versionSummary : versionListing.getVersionSummaries()) {
                s3.deleteVersion(bucketName, versionSummary.getKey(), versionSummary.getVersionId());
            }
        } while (versionListing.isTruncated());

        s3.deleteBucket(bucketName);
    }
}
