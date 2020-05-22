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
 * Interface for accessing ToolkitTelemetry.
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
public interface TelemetryClient {

    /**
     * @param postErrorReportRequest
     * @return Result of the PostErrorReport operation returned by the service.
     * @sample TelemetryClient.PostErrorReport
     */
    PostErrorReportResult postErrorReport(PostErrorReportRequest postErrorReportRequest);

    /**
     * @param postFeedbackRequest
     * @return Result of the PostFeedback operation returned by the service.
     * @sample TelemetryClient.PostFeedback
     */
    PostFeedbackResult postFeedback(PostFeedbackRequest postFeedbackRequest);

    /**
     * @param postMetricsRequest
     * @return Result of the PostMetrics operation returned by the service.
     * @sample TelemetryClient.PostMetrics
     */
    PostMetricsResult postMetrics(PostMetricsRequest postMetricsRequest);

    /**
     * @return Create new instance of builder with all defaults set.
     */
    public static TelemetryClientClientBuilder builder() {
        return new TelemetryClientClientBuilder();
    }

    /**
     * Shuts down this client object, releasing any resources that might be held open. This is an optional method, and
     * callers are not expected to call it, but can if they want to explicitly release any open resources. Once a client
     * has been shutdown, it should not be used to make any more requests.
     */
    void shutdown();

}
