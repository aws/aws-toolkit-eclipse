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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsResponse;

/**
 * Service client for accessing ToolkitTelemetry asynchronously. This can be created using the static {@link #builder()}
 * method.
 *
 * null
 */
@Generated("software.amazon.awssdk:codegen")
public interface ToolkitTelemetryAsyncClient extends SdkClient {
    String SERVICE_NAME = "execute-api";

    /**
     * Create a {@link ToolkitTelemetryAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static ToolkitTelemetryAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link ToolkitTelemetryAsyncClient}.
     */
    static ToolkitTelemetryAsyncClientBuilder builder() {
        return new DefaultToolkitTelemetryAsyncClientBuilder();
    }

    /**
     * Invokes the PostErrorReport operation asynchronously.
     *
     * @param postErrorReportRequest
     * @return A Java Future containing the result of the PostErrorReport operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostErrorReport
     */
    default CompletableFuture<PostErrorReportResponse> postErrorReport(PostErrorReportRequest postErrorReportRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostErrorReport operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostErrorReportRequest.Builder} avoiding the need
     * to create one manually via {@link PostErrorReportRequest#builder()}
     * </p>
     *
     * @param postErrorReportRequest
     *        A {@link Consumer} that will call methods on {@link PostErrorReportRequest.Builder} to create a request.
     * @return A Java Future containing the result of the PostErrorReport operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostErrorReport
     */
    default CompletableFuture<PostErrorReportResponse> postErrorReport(
            Consumer<PostErrorReportRequest.Builder> postErrorReportRequest) {
        return postErrorReport(PostErrorReportRequest.builder().applyMutation(postErrorReportRequest).build());
    }

    /**
     * Invokes the PostFeedback operation asynchronously.
     *
     * @param postFeedbackRequest
     * @return A Java Future containing the result of the PostFeedback operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostFeedback
     */
    default CompletableFuture<PostFeedbackResponse> postFeedback(PostFeedbackRequest postFeedbackRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostFeedback operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostFeedbackRequest.Builder} avoiding the need to
     * create one manually via {@link PostFeedbackRequest#builder()}
     * </p>
     *
     * @param postFeedbackRequest
     *        A {@link Consumer} that will call methods on {@link PostFeedbackRequest.Builder} to create a request.
     * @return A Java Future containing the result of the PostFeedback operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostFeedback
     */
    default CompletableFuture<PostFeedbackResponse> postFeedback(Consumer<PostFeedbackRequest.Builder> postFeedbackRequest) {
        return postFeedback(PostFeedbackRequest.builder().applyMutation(postFeedbackRequest).build());
    }

    /**
     * Invokes the PostMetrics operation asynchronously.
     *
     * @param postMetricsRequest
     * @return A Java Future containing the result of the PostMetrics operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostMetrics
     */
    default CompletableFuture<PostMetricsResponse> postMetrics(PostMetricsRequest postMetricsRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PostMetrics operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PostMetricsRequest.Builder} avoiding the need to
     * create one manually via {@link PostMetricsRequest#builder()}
     * </p>
     *
     * @param postMetricsRequest
     *        A {@link Consumer} that will call methods on {@link PostMetricsRequest.Builder} to create a request.
     * @return A Java Future containing the result of the PostMetrics operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>ToolkitTelemetryException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample ToolkitTelemetryAsyncClient.PostMetrics
     */
    default CompletableFuture<PostMetricsResponse> postMetrics(Consumer<PostMetricsRequest.Builder> postMetricsRequest) {
        return postMetrics(PostMetricsRequest.builder().applyMutation(postMetricsRequest).build());
    }
}
