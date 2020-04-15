/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.ToolkitTelemetryException;

/**
 * Service client for accessing ToolkitTelemetry. This can be created using the static {@link #builder()} method.
 *
 * null
 */
@Generated("software.amazon.awssdk:codegen")
public interface ToolkitTelemetryClient extends SdkClient {
    String SERVICE_NAME = "execute-api";

    /**
     * Create a {@link ToolkitTelemetryClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static ToolkitTelemetryClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link ToolkitTelemetryClient}.
     */
    static ToolkitTelemetryClientBuilder builder() {
        return new DefaultToolkitTelemetryClientBuilder();
    }

    /**
     * Invokes the PostErrorReport operation.
     *
     * @param postErrorReportRequest
     * @return Result of the PostErrorReport operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostErrorReport
     */
    default PostErrorReportResponse postErrorReport(PostErrorReportRequest postErrorReportRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostErrorReport operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostErrorReportRequest.Builder} avoiding the need
     * to create one manually via {@link PostErrorReportRequest#builder()}
     * </p>
     *
     * @param postErrorReportRequest
     *        A {@link Consumer} that will call methods on {@link PostErrorReportRequest.Builder} to create a request.
     * @return Result of the PostErrorReport operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostErrorReport
     */
    default PostErrorReportResponse postErrorReport(Consumer<PostErrorReportRequest.Builder> postErrorReportRequest)
            throws AwsServiceException, SdkClientException, ToolkitTelemetryException {
        return postErrorReport(PostErrorReportRequest.builder().applyMutation(postErrorReportRequest).build());
    }

    /**
     * Invokes the PostFeedback operation.
     *
     * @param postFeedbackRequest
     * @return Result of the PostFeedback operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostFeedback
     */
    default PostFeedbackResponse postFeedback(PostFeedbackRequest postFeedbackRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostFeedback operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostFeedbackRequest.Builder} avoiding the need to
     * create one manually via {@link PostFeedbackRequest#builder()}
     * </p>
     *
     * @param postFeedbackRequest
     *        A {@link Consumer} that will call methods on {@link PostFeedbackRequest.Builder} to create a request.
     * @return Result of the PostFeedback operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostFeedback
     */
    default PostFeedbackResponse postFeedback(Consumer<PostFeedbackRequest.Builder> postFeedbackRequest)
            throws AwsServiceException, SdkClientException, ToolkitTelemetryException {
        return postFeedback(PostFeedbackRequest.builder().applyMutation(postFeedbackRequest).build());
    }

    /**
     * Invokes the PostMetrics operation.
     *
     * @param postMetricsRequest
     * @return Result of the PostMetrics operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostMetrics
     */
    default PostMetricsResponse postMetrics(PostMetricsRequest postMetricsRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostMetrics operation.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostMetricsRequest.Builder} avoiding the need to
     * create one manually via {@link PostMetricsRequest#builder()}
     * </p>
     *
     * @param postMetricsRequest
     *        A {@link Consumer} that will call methods on {@link PostMetricsRequest.Builder} to create a request.
     * @return Result of the PostMetrics operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws ToolkitTelemetryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample ToolkitTelemetryClient.PostMetrics
     */
    default PostMetricsResponse postMetrics(Consumer<PostMetricsRequest.Builder> postMetricsRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        return postMetrics(PostMetricsRequest.builder().applyMutation(postMetricsRequest).build());
    }

    static ServiceMetadata serviceMetadata() {
        return ServiceMetadata.of("ToolkitTelemetry");
    }
}
