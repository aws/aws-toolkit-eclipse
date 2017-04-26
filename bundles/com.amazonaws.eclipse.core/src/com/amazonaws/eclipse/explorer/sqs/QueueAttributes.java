/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.sqs;

import com.amazonaws.services.sqs.model.QueueAttributeName;

public interface QueueAttributes {
    public static String ALL = "All";

    // Queue Attributes
    public static String RETENTION_PERIOD = "MessageRetentionPeriod";
    public static String MAX_MESSAGE_SIZE = "MaximumMessageSize";
    public static String CREATED = "CreatedTimestamp";
    public static String VISIBILITY_TIMEOUT = "VisibilityTimeout";
    public static String ARN = "QueueArn";
    public static String NUMBER_OF_MESSAGES = "ApproximateNumberOfMessages";
    public static String DELAY_SECONDS = QueueAttributeName.DelaySeconds.toString();

    // Message Attributes
    public static String FIRST_RECEIVED = "ApproximateFirstReceiveTimestamp";
    public static String RECEIVE_COUNT = "ApproximateReceiveCount";
    public static String SENT = "SentTimestamp";
    public static String SENDER_ID = "SenderId";
}
