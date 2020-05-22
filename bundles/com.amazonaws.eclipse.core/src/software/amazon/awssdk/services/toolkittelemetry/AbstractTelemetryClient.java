/*
 * Copyright 2015-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.services.toolkittelemetry;

import javax.annotation.Generated;
import software.amazon.awssdk.services.toolkittelemetry.model.*;

/**
 * Abstract implementation of {@code TelemetryClient}.
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class AbstractTelemetryClient implements TelemetryClient {

    protected AbstractTelemetryClient() {
    }

    @Override
    public PostErrorReportResult postErrorReport(PostErrorReportRequest request) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public PostFeedbackResult postFeedback(PostFeedbackRequest request) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public PostMetricsResult postMetrics(PostMetricsRequest request) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new java.lang.UnsupportedOperationException();
    }

}
