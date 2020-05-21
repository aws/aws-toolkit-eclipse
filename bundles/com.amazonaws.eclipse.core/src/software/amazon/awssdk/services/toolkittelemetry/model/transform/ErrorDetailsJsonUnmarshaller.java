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

import software.amazon.awssdk.services.toolkittelemetry.model.*;
import com.amazonaws.transform.*;

import com.fasterxml.jackson.core.JsonToken;
import static com.fasterxml.jackson.core.JsonToken.*;

/**
 * ErrorDetails JSON Unmarshaller
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class ErrorDetailsJsonUnmarshaller implements Unmarshaller<ErrorDetails, JsonUnmarshallerContext> {

    public ErrorDetails unmarshall(JsonUnmarshallerContext context) throws Exception {
        ErrorDetails errorDetails = new ErrorDetails();

        int originalDepth = context.getCurrentDepth();
        String currentParentElement = context.getCurrentParentElement();
        int targetDepth = originalDepth + 1;

        JsonToken token = context.getCurrentToken();
        if (token == null)
            token = context.nextToken();
        if (token == VALUE_NULL) {
            return null;
        }

        while (true) {
            if (token == null)
                break;

            if (token == FIELD_NAME || token == START_OBJECT) {
                if (context.testExpression("Command", targetDepth)) {
                    context.nextToken();
                    errorDetails.setCommand(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("EpochTimestamp", targetDepth)) {
                    context.nextToken();
                    errorDetails.setEpochTimestamp(context.getUnmarshaller(Long.class).unmarshall(context));
                }
                if (context.testExpression("Type", targetDepth)) {
                    context.nextToken();
                    errorDetails.setType(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("Message", targetDepth)) {
                    context.nextToken();
                    errorDetails.setMessage(context.getUnmarshaller(String.class).unmarshall(context));
                }
                if (context.testExpression("StackTrace", targetDepth)) {
                    context.nextToken();
                    errorDetails.setStackTrace(context.getUnmarshaller(String.class).unmarshall(context));
                }
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getLastParsedParentElement() == null || context.getLastParsedParentElement().equals(currentParentElement)) {
                    if (context.getCurrentDepth() <= originalDepth)
                        break;
                }
            }
            token = context.nextToken();
        }

        return errorDetails;
    }

    private static ErrorDetailsJsonUnmarshaller instance;

    public static ErrorDetailsJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new ErrorDetailsJsonUnmarshaller();
        return instance;
    }
}
