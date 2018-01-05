package com.amazonaws.eclipse.core.regions;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.model.ListDistributionsRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.s3.AmazonS3;

public class AwsClientFactoryTests {
    private static AWSClientFactory FACTORY;

    @BeforeClass
    public static void setUp() {
        // We are testing invalid signing region, it doesn't matter if the AWS Credentials
        // is not valid.
        FACTORY = new AWSClientFactory(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials("foo", "bar")));
    }

    /**
     * IAM and CloudFront are using global endpoints. The following two tests assure the clients
     * are created correctly with different regions.
     */
    @Test
    public void testIamClient() {
        testIamClientByRegion("us-west-2");
        testIamClientByRegion("cn-north-1");
        testIamClientByRegion("cn-northwest-1");
        testIamClientByRegion("us-gov-west-1");
    }

    @Test
    public void testCloudFrontClient() {
        testCloudFrontClientByRegion("us-west-2");
    }

    @Test
    public void testS3Client() {
        testS3ClientByRegion("us-west-2");
        testS3ClientByRegion("eu-central-1");
        testS3ClientByRegion("cn-north-1");
        testS3ClientByRegion("cn-northwest-1");
        testS3ClientByRegion("us-gov-west-1");
    }

    /**
     * For IAM client, if the provided signing region is not "us-east-1" for AWS partition,
     * the SignatureDoesNotMatch exception would be thrown. This test case assures the IAM
     * client is created correctly.
     */
    private void testIamClientByRegion(String regionId) {
        try {
            AmazonIdentityManagement iam = FACTORY.getIAMClientByRegion(regionId);
            iam.listAccessKeys();
        } catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode();
            Assert.assertNotEquals("SignatureDoesNotMatch", errorCode);
        }
    }

    /**
     * For CloudFront client, if the provided signing region is not "us-east-1" for AWS partition,
     * the SignatureDoesNotMatch exception would be thrown. This test case assures the CloudFront
     * client is created correctly.
     */
    private void testCloudFrontClientByRegion(String regionId) {
        try {
            AmazonCloudFront cloudFront = FACTORY.getCloudFrontClientByRegion(regionId);
            cloudFront.listDistributions(new ListDistributionsRequest());
        } catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode();
            Assert.assertNotEquals("SignatureDoesNotMatch", errorCode);
        }
    }

    /**
     * For S3 client, if the underlying endpoint is the global endpoint "https://s3.amazonaws.com",
     * but the signing region is not "us-east-1", the AuthorizationHeaderMalformed exception would
     * be thrown. This test assures the S3 client is created correctly with different regions.
     */
    private void testS3ClientByRegion(String regionId) {
        try {
            AmazonS3 s3 = FACTORY.getS3ClientByRegion(regionId);
            s3.listBuckets();
        } catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode();
            Assert.assertNotEquals("AuthorizationHeaderMalformed", errorCode);
        }
    }
}
