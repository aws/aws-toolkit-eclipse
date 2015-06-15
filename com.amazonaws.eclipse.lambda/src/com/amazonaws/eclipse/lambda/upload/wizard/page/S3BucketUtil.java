package com.amazonaws.eclipse.lambda.upload.wizard.page;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        final CopyOnWriteArrayList<Bucket> result = new CopyOnWriteArrayList<Bucket>();
        final CountDownLatch latch = new CountDownLatch(buckets.size());

        for (final Bucket bucket : buckets) {
            es.submit(new Runnable() {
                public void run() {
                    if (isBucketInRegion(s3, bucket, region)) {
                        result.add(bucket);
                    }
                    latch.countDown();
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

    private static boolean isBucketInRegion(AmazonS3 s3, Bucket bucket,
            Region eclipseRegion) {

        String s3RegionId = s3.getBucketLocation(bucket.getName());
        com.amazonaws.services.s3.model.Region s3Region = com.amazonaws.services.s3.model.Region.fromValue(s3RegionId);
        com.amazonaws.regions.Region awsRegion = s3Region.toAWSRegion();

        return eclipseRegion.getId().equals(awsRegion.getName());
    }
}
