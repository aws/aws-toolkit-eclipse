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

/**
 * PostErrorReportResult JSON Unmarshaller
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class PostErrorReportResultJsonUnmarshaller implements Unmarshaller<PostErrorReportResult, JsonUnmarshallerContext> {

    public PostErrorReportResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        PostErrorReportResult postErrorReportResult = new PostErrorReportResult();

        return postErrorReportResult;
    }

    private static PostErrorReportResultJsonUnmarshaller instance;

    public static PostErrorReportResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new PostErrorReportResultJsonUnmarshaller();
        return instance;
    }
}
