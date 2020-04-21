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
package software.amazon.awssdk.services.toolkittelemetry.model.transform;

import java.util.List;
import javax.annotation.Generated;

import com.amazonaws.SdkClientException;
import software.amazon.awssdk.services.toolkittelemetry.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * PostErrorReportRequestMarshaller
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
@SdkInternalApi
public class PostErrorReportRequestMarshaller {

    private static final MarshallingInfo<String> AWSPRODUCT_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("AWSProduct").build();
    private static final MarshallingInfo<String> AWSPRODUCTVERSION_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("AWSProductVersion").build();
    private static final MarshallingInfo<List> METADATA_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("Metadata").build();
    private static final MarshallingInfo<StructuredPojo> USERDATA_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("Userdata").build();
    private static final MarshallingInfo<StructuredPojo> ERRORDETAILS_BINDING = MarshallingInfo.builder(MarshallingType.STRUCTURED)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("ErrorDetails").build();

    private static final PostErrorReportRequestMarshaller instance = new PostErrorReportRequestMarshaller();

    public static PostErrorReportRequestMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(PostErrorReportRequest postErrorReportRequest, ProtocolMarshaller protocolMarshaller) {

        if (postErrorReportRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(postErrorReportRequest.getAWSProduct(), AWSPRODUCT_BINDING);
            protocolMarshaller.marshall(postErrorReportRequest.getAWSProductVersion(), AWSPRODUCTVERSION_BINDING);
            protocolMarshaller.marshall(postErrorReportRequest.getMetadata(), METADATA_BINDING);
            protocolMarshaller.marshall(postErrorReportRequest.getUserdata(), USERDATA_BINDING);
            protocolMarshaller.marshall(postErrorReportRequest.getErrorDetails(), ERRORDETAILS_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
