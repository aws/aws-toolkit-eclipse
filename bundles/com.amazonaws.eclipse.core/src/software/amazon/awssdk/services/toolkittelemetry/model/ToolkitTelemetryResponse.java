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

package software.amazon.awssdk.services.toolkittelemetry.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;

@Generated("software.amazon.awssdk:codegen")
public abstract class ToolkitTelemetryResponse extends AwsResponse {
    private final ToolkitTelemetryResponseMetadata responseMetadata;

    protected ToolkitTelemetryResponse(Builder builder) {
        super(builder);
        this.responseMetadata = builder.responseMetadata();
    }

    @Override
    public ToolkitTelemetryResponseMetadata responseMetadata() {
        return responseMetadata;
    }

    public interface Builder extends AwsResponse.Builder {
        @Override
        ToolkitTelemetryResponse build();

        @Override
        ToolkitTelemetryResponseMetadata responseMetadata();

        @Override
        Builder responseMetadata(AwsResponseMetadata metadata);
    }

    protected abstract static class BuilderImpl extends AwsResponse.BuilderImpl implements Builder {
        private ToolkitTelemetryResponseMetadata responseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(ToolkitTelemetryResponse response) {
            super(response);
            this.responseMetadata = response.responseMetadata();
        }

        @Override
        public ToolkitTelemetryResponseMetadata responseMetadata() {
            return responseMetadata;
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
            this.responseMetadata = ToolkitTelemetryResponseMetadata.create(responseMetadata);
            return this;
        }
    }
}
