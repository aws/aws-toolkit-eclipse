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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostErrorReportResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostFeedbackResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsRequest;
import software.amazon.awssdk.services.toolkittelemetry.model.PostMetricsResponse;
import software.amazon.awssdk.services.toolkittelemetry.model.ToolkitTelemetryException;
import software.amazon.awssdk.services.toolkittelemetry.transform.PostErrorReportRequestMarshaller;
import software.amazon.awssdk.services.toolkittelemetry.transform.PostFeedbackRequestMarshaller;
import software.amazon.awssdk.services.toolkittelemetry.transform.PostMetricsRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link ToolkitTelemetryAsyncClient}.
 *
 * @see ToolkitTelemetryAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultToolkitTelemetryAsyncClient implements ToolkitTelemetryAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultToolkitTelemetryAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultToolkitTelemetryAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
    @Override
    public CompletableFuture<PostErrorReportResponse> postErrorReport(PostErrorReportRequest postErrorReportRequest) {
        try {
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<PostErrorReportResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, PostErrorReportResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<PostErrorReportResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PostErrorReportRequest, PostErrorReportResponse>()
                            .withOperationName("PostErrorReport")
                            .withMarshaller(new PostErrorReportRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withInput(postErrorReportRequest));
            return executeFuture;
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
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
    @Override
    public CompletableFuture<PostFeedbackResponse> postFeedback(PostFeedbackRequest postFeedbackRequest) {
        try {
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<PostFeedbackResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    PostFeedbackResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<PostFeedbackResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PostFeedbackRequest, PostFeedbackResponse>()
                            .withOperationName("PostFeedback").withMarshaller(new PostFeedbackRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withInput(postFeedbackRequest));
            return executeFuture;
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
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
    @Override
    public CompletableFuture<PostMetricsResponse> postMetrics(PostMetricsRequest postMetricsRequest) {
        try {
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<PostMetricsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    PostMetricsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<PostMetricsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PostMetricsRequest, PostMetricsResponse>()
                            .withOperationName("PostMetrics").withMarshaller(new PostMetricsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withInput(postMetricsRequest));
            return executeFuture;
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                .defaultServiceExceptionSupplier(ToolkitTelemetryException::builder).protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1");
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }
}
