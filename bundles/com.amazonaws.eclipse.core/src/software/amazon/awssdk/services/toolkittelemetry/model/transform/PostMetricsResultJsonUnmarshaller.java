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
 * PostMetricsResult JSON Unmarshaller
 */
@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class PostMetricsResultJsonUnmarshaller implements Unmarshaller<PostMetricsResult, JsonUnmarshallerContext> {

    public PostMetricsResult unmarshall(JsonUnmarshallerContext context) throws Exception {
        PostMetricsResult postMetricsResult = new PostMetricsResult();

        return postMetricsResult;
    }

    private static PostMetricsResultJsonUnmarshaller instance;

    public static PostMetricsResultJsonUnmarshaller getInstance() {
        if (instance == null)
            instance = new PostMetricsResultJsonUnmarshaller();
        return instance;
    }
}
