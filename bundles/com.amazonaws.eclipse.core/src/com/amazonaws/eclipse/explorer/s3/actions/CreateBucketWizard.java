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
package com.amazonaws.eclipse.explorer.s3.actions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.wizards.CompositeWizardPage;
import com.amazonaws.eclipse.core.ui.wizards.InputValidator;
import com.amazonaws.eclipse.core.ui.wizards.TextWizardPageInput;
import com.amazonaws.eclipse.core.ui.wizards.WizardPageInput;
import com.amazonaws.eclipse.explorer.s3.S3ContentProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;

class CreateBucketWizard extends Wizard {

    private final CompositeWizardPage page;

    /**
     * Constructor.
     */
    public CreateBucketWizard() {
        page = new CompositeWizardPage(
            "Create New Bucket",
            "Create New Bucket",
            AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor("aws-logo"));

        WizardPageInput bucketName = new TextWizardPageInput(
            "Bucket Name: ",
            null,       // no descriptive text.
            IsBucketNameValid.INSTANCE,
            IsBucketNameUnique.INSTANCE
        );

        page.addInput(BUCKET_NAME_INPUT, bucketName);
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
        String regionId = RegionUtils.getCurrentRegion().getId();
        String bucketName = (String) page.getInputValue(BUCKET_NAME_INPUT);

        AmazonS3 client = AwsToolkitCore.getClientFactory().getS3Client();

        CreateBucketRequest createBucketRequest =
            new CreateBucketRequest(bucketName);

        if ("us-east-1".equals(regionId)) {
            // us-east-1 is the default, no need to set a location
        } else if ("eu-west-1".equals(regionId)) {
            // eu-west-1 uses an older style location
            createBucketRequest.setRegion("EU");
        } else {
            createBucketRequest.setRegion(regionId);
        }

        client.createBucket(createBucketRequest);

        S3ContentProvider.getInstance().refresh();
        return true;
    }

    private static final String BUCKET_NAME_INPUT = "bucketName";

    /**
     * Synchronous validation; is this a syntactically-valid bucket name?
     */
    private static class IsBucketNameValid implements InputValidator {

        public static final IsBucketNameValid INSTANCE =
            new IsBucketNameValid();

        /**
         * Validate whether the input is a syntactically-valid bucket name.
         *
         * @param value the bucket name
         * @return the result of validation
         */
        @Override
        public IStatus validate(final Object value) {
            String bucketName = (String) value;

            if (bucketName == null || bucketName.length() == 0) {
                return ValidationStatus.error("Please enter a bucket name");
            }

            try {
                BucketNameUtils.validateBucketName(bucketName);
            } catch (IllegalArgumentException exception) {
                return ValidationStatus.error(exception.getMessage());
            }

            return ValidationStatus.ok();
        }

        /**
         * I'm stateless, use my singleton INSTANCE.
         */
        private IsBucketNameValid() {
        }
    }

    /**
     * Asynchronous validation; is the bucket name already in use by
     * someone else?
     */
    private static class IsBucketNameUnique implements InputValidator {

        public static final IsBucketNameUnique INSTANCE =
            new IsBucketNameUnique();

        /**
         * Validate that there is no existing bucket with the given name.
         *
         * @param value the bucket name
         * @return the result of validation
         */
        @Override
        public IStatus validate(final Object value) {
            String bucketName = (String) value;

            AmazonS3 client = AwsToolkitCore.getClientFactory().getS3Client();

            try {

                client.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withMaxKeys(0));

            } catch (AmazonServiceException exception) {

                if (VALID_ERROR_CODES.contains(exception.getErrorCode())) {
                    return ValidationStatus.ok();
                }

                // Not sure if listObjects will ever return this, but check
                // for it just in case...
                if ("InvalidBucketName".equals(exception.getErrorCode())) {
                    return ValidationStatus.error("Invalid bucket name");
                }

                if (!IN_USE_ERROR_CODES.contains(exception.getErrorCode())) {
                    // Unanticipated error code; log it for future analysis.
                    // Should we be erring on the side of leniency here and
                    // treating these as valid so we don't accidentally block
                    // a valid creation request?

                    AwsToolkitCore.getDefault().logError(
                        "Error checking whether bucket exists",
                        exception
                    );

                    return ValidationStatus.error("Error validating bucket name");
                }

            }

            return ValidationStatus.error("Bucket name in use");
        }

        /**
         * I'm stateless, use my singleton INSTANCE.
         */
        private IsBucketNameUnique() {
        }

        /**
         * Error Codes for ListObjects which we interpret to mean that the
         * bucket name is valid and does not yet exist.
         */
        private static final Set<String> VALID_ERROR_CODES;
        static {
            Set<String> set = new HashSet<>();

            set.add("NoSuchBucket");

            // err on the side of allowing the bucket creation in any
            // of these expected transient failure cases.
            set.add("RequestTimeout");
            set.add("ServiceUnavailable");
            set.add("SlowDown");

            VALID_ERROR_CODES = Collections.unmodifiableSet(set);
        }

        /**
         * Error Codes for ListObjects which imply that the bucket already
         * exists.
         */
        private static final Set<String> IN_USE_ERROR_CODES;
        static {
            Set<String> set = new HashSet<>();

            set.add("AccessDenied");
            set.add("InvalidBucketState");
            set.add("InvalidObjectState");
            set.add("PermanentRedirect");
            set.add("Redirect");
            set.add("TemporaryRedirect");

            IN_USE_ERROR_CODES = Collections.unmodifiableSet(set);
        }
    };
}
