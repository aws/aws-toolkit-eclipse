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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.AWSLogsException;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.OrderBy;
import com.amazonaws.services.logs.model.OutputLogEvent;
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

    public static List<LogStream> listLogStreams(AWSLogs client, String groupName) {
        List<LogStream> streams = new ArrayList<>();

        DescribeLogStreamsRequest request = new DescribeLogStreamsRequest()
                .withLogGroupName(groupName)
                .withOrderBy(OrderBy.LastEventTime)
                .withDescending(false);
        DescribeLogStreamsResult result = null;

        try {
            do {
                result = client.describeLogStreams(request);
                streams.addAll(result.getLogStreams());
                request.setNextToken(result.getNextToken());
            } while (result.getNextToken() != null);
        } catch (AWSLogsException e) {
            LambdaPlugin.getDefault().logError(e.getMessage(), e);
        } catch (Exception ee) {
            LambdaPlugin.getDefault().reportException(ee.getMessage(), ee);
        }

        return streams;
    }


    public static List<OutputLogEvent> listLogEvents(AWSLogs client, String groupName, List<LogStream> streamNames) {
        List<OutputLogEvent> events = new ArrayList<>();

        for (LogStream stream : streamNames) {
            GetLogEventsRequest getLogEventsRequest = new GetLogEventsRequest()
                    .withLogGroupName(groupName)
                    .withLogStreamName(stream.getLogStreamName());
            GetLogEventsResult getLogEventResult = null;
            do {
                getLogEventResult = client.getLogEvents(getLogEventsRequest);
                events.addAll(getLogEventResult.getEvents());
                getLogEventsRequest.setNextToken(getLogEventResult.getNextBackwardToken());
            } while (!getLogEventResult.getEvents().isEmpty());
        }

        return events;
    }

    public static String convertLogEventsToString(List<OutputLogEvent> events) {
        StringBuilder builder = new StringBuilder();

        for (OutputLogEvent event : events) {
            builder.append(String.format("%s\t%s\n",
                    longTimeToHumanReadible(event.getTimestamp()), event.getMessage()));
        }

        return builder.toString();
    }

    public static String longTimeToHumanReadible(long time) {
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date (time));
    }
}
