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

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
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

/**
 * Internal implementation of {@link ToolkitTelemetryClient}.
 *
 * @see ToolkitTelemetryClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultToolkitTelemetryClient implements ToolkitTelemetryClient {
    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultToolkitTelemetryClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
    @Override
    public PostErrorReportResponse postErrorReport(PostErrorReportRequest postErrorReportRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<PostErrorReportResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                PostErrorReportResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);

        return clientHandler.execute(new ClientExecutionParams<PostErrorReportRequest, PostErrorReportResponse>()
                .withOperationName("PostErrorReport").withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postErrorReportRequest)
                .withMarshaller(new PostErrorReportRequestMarshaller(protocolFactory)));
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
    @Override
    public PostFeedbackResponse postFeedback(PostFeedbackRequest postFeedbackRequest) throws AwsServiceException,
            SdkClientException, ToolkitTelemetryException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<PostFeedbackResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                PostFeedbackResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);

        return clientHandler.execute(new ClientExecutionParams<PostFeedbackRequest, PostFeedbackResponse>()
                .withOperationName("PostFeedback").withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postFeedbackRequest)
                .withMarshaller(new PostFeedbackRequestMarshaller(protocolFactory)));
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
    @Override
    public PostMetricsResponse postMetrics(PostMetricsRequest postMetricsRequest) throws AwsServiceException, SdkClientException,
            ToolkitTelemetryException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                .isPayloadJson(true).build();

        HttpResponseHandler<PostMetricsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                PostMetricsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                operationMetadata);

        return clientHandler.execute(new ClientExecutionParams<PostMetricsRequest, PostMetricsResponse>()
                .withOperationName("PostMetrics").withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postMetricsRequest)
                .withMarshaller(new PostMetricsRequestMarshaller(protocolFactory)));
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                .defaultServiceExceptionSupplier(ToolkitTelemetryException::builder).protocol(AwsJsonProtocol.REST_JSON)
                .protocolVersion("1.1");
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
