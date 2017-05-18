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
package com.amazonaws.eclipse.core;

import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;

public class CoreAnalytics {

    /*
     * Upload function wizard.
     */
    private static final String EVENT_TYPE_UPLOAD_FUNCTION_WIZARD = "Lambda-UploadFunctionWizard";
    private static final String METRIC_NAME_LOAD_S3_BUCKET_TIME_DURATION_MS = "LoadS3BucketTimeDurationMs";

    private static final ToolkitAnalyticsManager ANALYTICS = AwsToolkitCore.getDefault().getAnalyticsManager();

    public static void trackLoadBucketTimeDuration(long duration) {
        ANALYTICS.publishEvent(ANALYTICS.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addMetric(METRIC_NAME_LOAD_S3_BUCKET_TIME_DURATION_MS, (double)duration)
                .build());
    }
}
