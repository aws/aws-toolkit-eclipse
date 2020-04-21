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

import javax.annotation.Generated;

import com.amazonaws.SdkClientException;
import software.amazon.awssdk.services.toolkittelemetry.model.*;

import com.amazonaws.protocol.*;
import com.amazonaws.annotation.SdkInternalApi;

/**
 * ErrorDetailsMarshaller
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
@SdkInternalApi
public class ErrorDetailsMarshaller {

    private static final MarshallingInfo<String> COMMAND_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("Command").build();
    private static final MarshallingInfo<Long> EPOCHTIMESTAMP_BINDING = MarshallingInfo.builder(MarshallingType.LONG)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("EpochTimestamp").build();
    private static final MarshallingInfo<String> TYPE_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("Type").build();
    private static final MarshallingInfo<String> MESSAGE_BINDING = MarshallingInfo.builder(MarshallingType.STRING).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("Message").build();
    private static final MarshallingInfo<String> STACKTRACE_BINDING = MarshallingInfo.builder(MarshallingType.STRING)
            .marshallLocation(MarshallLocation.PAYLOAD).marshallLocationName("StackTrace").build();

    private static final ErrorDetailsMarshaller instance = new ErrorDetailsMarshaller();

    public static ErrorDetailsMarshaller getInstance() {
        return instance;
    }

    /**
     * Marshall the given parameter object.
     */
    public void marshall(ErrorDetails errorDetails, ProtocolMarshaller protocolMarshaller) {

        if (errorDetails == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        try {
            protocolMarshaller.marshall(errorDetails.getCommand(), COMMAND_BINDING);
            protocolMarshaller.marshall(errorDetails.getEpochTimestamp(), EPOCHTIMESTAMP_BINDING);
            protocolMarshaller.marshall(errorDetails.getType(), TYPE_BINDING);
            protocolMarshaller.marshall(errorDetails.getMessage(), MESSAGE_BINDING);
            protocolMarshaller.marshall(errorDetails.getStackTrace(), STACKTRACE_BINDING);
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

}
