/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.lambda.invoke.logs;

import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.util.Base64;
import com.amazonaws.util.CodecUtils;
import com.amazonaws.util.StringUtils;

public class CloudWatchLogsUtils {

    public static final int MAX_LAMBDA_LOG_RESULT_LENGTH = 4 * 1024;// 4Kb

    public static String fetchLogsForLambdaFunction(InvokeResult invokeResult) {
        if (invokeResult != null && !StringUtils.isNullOrEmpty(invokeResult.getLogResult())) {
            return CodecUtils.toStringDirect(Base64.decode((invokeResult.getLogResult())));
        }
        return null;
    }
}
