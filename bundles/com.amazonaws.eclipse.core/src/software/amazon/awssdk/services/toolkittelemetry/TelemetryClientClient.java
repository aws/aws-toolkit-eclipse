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

import java.util.*;

import javax.annotation.Generated;

import com.amazonaws.http.*;
import com.amazonaws.protocol.json.*;

import com.amazonaws.annotation.ThreadSafe;
import com.amazonaws.client.AwsSyncClientParams;

import com.amazonaws.client.ClientHandler;
import com.amazonaws.client.ClientHandlerParams;
import com.amazonaws.client.ClientExecutionParams;
import com.amazonaws.opensdk.protect.client.SdkClientHandler;
import com.amazonaws.SdkBaseException;

import software.amazon.awssdk.services.toolkittelemetry.model.*;
import software.amazon.awssdk.services.toolkittelemetry.model.transform.*;

/**
 * Client for accessing ToolkitTelemetry. All service calls made using this client are blocking, and will not return
 * until the service call completes.
 * <p>
 * 
 */
@ThreadSafe
@Generated("com.amazonaws:aws-java-sdk-code-generator")
class TelemetryClientClient implements TelemetryClient {

    private final ClientHandler clientHandler;

    private static final com.amazonaws.opensdk.protect.protocol.ApiGatewayProtocolFactoryImpl protocolFactory = new com.amazonaws.opensdk.protect.protocol.ApiGatewayProtocolFactoryImpl(
            new JsonClientMetadata().withProtocolVersion("1.1").withSupportsCbor(false).withSupportsIon(false).withContentTypeOverride("application/json")
                    .withBaseServiceExceptionClass(software.amazon.awssdk.services.toolkittelemetry.model.TelemetryClientException.class));

    /**
     * Constructs a new client to invoke service methods on ToolkitTelemetry using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    TelemetryClientClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams));
    }

    /**
     * @param postErrorReportRequest
     * @return Result of the PostErrorReport operation returned by the service.
     * @sample TelemetryClient.PostErrorReport
     */
    @Override
    public PostErrorReportResult postErrorReport(PostErrorReportRequest postErrorReportRequest) {
        HttpResponseHandler<PostErrorReportResult> responseHandler = protocolFactory.createResponseHandler(new JsonOperationMetadata().withPayloadJson(true)
                .withHasStreamingSuccessResponse(false), new PostErrorReportResultJsonUnmarshaller());

        HttpResponseHandler<SdkBaseException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<PostErrorReportRequest, PostErrorReportResult>()
                .withMarshaller(new PostErrorReportRequestProtocolMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postErrorReportRequest));
    }

    /**
     * @param postFeedbackRequest
     * @return Result of the PostFeedback operation returned by the service.
     * @sample TelemetryClient.PostFeedback
     */
    @Override
    public PostFeedbackResult postFeedback(PostFeedbackRequest postFeedbackRequest) {
        HttpResponseHandler<PostFeedbackResult> responseHandler = protocolFactory.createResponseHandler(new JsonOperationMetadata().withPayloadJson(true)
                .withHasStreamingSuccessResponse(false), new PostFeedbackResultJsonUnmarshaller());

        HttpResponseHandler<SdkBaseException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<PostFeedbackRequest, PostFeedbackResult>()
                .withMarshaller(new PostFeedbackRequestProtocolMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postFeedbackRequest));
    }

    /**
     * @param postMetricsRequest
     * @return Result of the PostMetrics operation returned by the service.
     * @sample TelemetryClient.PostMetrics
     */
    @Override
    public PostMetricsResult postMetrics(PostMetricsRequest postMetricsRequest) {
        HttpResponseHandler<PostMetricsResult> responseHandler = protocolFactory.createResponseHandler(new JsonOperationMetadata().withPayloadJson(true)
                .withHasStreamingSuccessResponse(false), new PostMetricsResultJsonUnmarshaller());

        HttpResponseHandler<SdkBaseException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<PostMetricsRequest, PostMetricsResult>()
                .withMarshaller(new PostMetricsRequestProtocolMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler).withInput(postMetricsRequest));
    }

    /**
     * Create the error response handler for the operation.
     * 
     * @param errorShapeMetadata
     *        Error metadata for the given operation
     * @return Configured error response handler to pass to HTTP layer
     */
    private HttpResponseHandler<SdkBaseException> createErrorResponseHandler(JsonErrorShapeMetadata... errorShapeMetadata) {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata().withErrorShapes(Arrays.asList(errorShapeMetadata)));
    }

    @Override
    public void shutdown() {
        clientHandler.shutdown();
    }

}
