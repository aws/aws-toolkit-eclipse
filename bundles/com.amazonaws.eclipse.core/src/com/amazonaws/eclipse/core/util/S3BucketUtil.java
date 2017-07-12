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
package com.amazonaws.eclipse.core.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class S3BucketUtil {

    public static List<Bucket> listBucketsInRegion(AmazonS3 s3, Region region) {
        List<Bucket> bucketsInAllRegions = s3.listBuckets();
        return findBucketsInRegion(s3, bucketsInAllRegions, region, 10);
    }

    private static List<Bucket> findBucketsInRegion(final AmazonS3 s3, List<Bucket> buckets,
            final Region region, int threads) {

        ExecutorService es = Executors.newFixedThreadPool(threads);

        final CopyOnWriteArrayList<Bucket> result = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(buckets.size());

        for (final Bucket bucket : buckets) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isBucketInRegion(s3, bucket, region)) {
                            result.add(bucket);
                        }
                    } catch (Exception e) {
                        AwsToolkitCore.getDefault().logInfo("Exception thrown when checking bucket " + bucket.getName() +
                            " with message: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result;
    }

    public static String createS3Path(String bucketName, String keyName) {
        return String.format("s3://%s/%s", bucketName, keyName);
    }

    private static boolean isBucketInRegion(AmazonS3 s3, Bucket bucket,
            Region eclipseRegion) {

        String s3RegionId = s3.getBucketLocation(bucket.getName());
        if (s3RegionId == null || s3RegionId.equals("US")) {
            s3RegionId = "us-east-1";
        }
        return eclipseRegion.getId().equals(s3RegionId);
    }
}