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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.model.ComboBoxItemData;

public class PredefinedHandlerInputType implements ComboBoxItemData {

    public static final PredefinedHandlerInputType S3_EVENT = new PredefinedHandlerInputType("S3Event", "com.amazonaws.services.lambda.runtime.events.S3Event", "S3 Event", true, "s3-event.put.json");
    public static final PredefinedHandlerInputType SNS_EVENT = new PredefinedHandlerInputType("SNSEvent", "com.amazonaws.services.lambda.runtime.events.SNSEvent", "SNS Event", true, "sns-event.json");
    public static final PredefinedHandlerInputType KINESIS_EVENT = new PredefinedHandlerInputType("KinesisEvent", "com.amazonaws.services.lambda.runtime.events.KinesisEvent", "Kinesis Event", true, "kinesis-event.json");
    public static final PredefinedHandlerInputType COGNITO_EVENT = new PredefinedHandlerInputType("CognitoEvent", "com.amazonaws.services.lambda.runtime.events.CognitoEvent", "Cognito Event", true, "cognito-sync-event.json");
    public static final PredefinedHandlerInputType DYNAMODB_EVENT = new PredefinedHandlerInputType("DynamodbEvent", "com.amazonaws.services.lambda.runtime.events.DynamodbEvent", "DynamoDB Event", true, "dynamodb-update-event.json");
    public static final PredefinedHandlerInputType CUSTOM = new PredefinedHandlerInputType(null, null, "Custom", false, null);

    private static final List<PredefinedHandlerInputType> LIST = new ArrayList<PredefinedHandlerInputType>();
    static {
        LIST.add(S3_EVENT);
        LIST.add(SNS_EVENT);
        LIST.add(KINESIS_EVENT);
        LIST.add(COGNITO_EVENT);
        LIST.add(DYNAMODB_EVENT);
        LIST.add(CUSTOM);
    }

    private final String className;
    private final String fqcn;
    private final String displayName;
    private final boolean requireSdkDependency;
    private final String sampleInputJsonFile;

    private PredefinedHandlerInputType(String className, String fqcn,
            String displayName, boolean requireSdkDependency,
            String sampleInputJsonFile) {

        this.className = className;
        this.fqcn = fqcn;
        this.displayName = displayName;
        this.requireSdkDependency = requireSdkDependency;
        this.sampleInputJsonFile = sampleInputJsonFile;
    }

    public static List<PredefinedHandlerInputType> list() {
        return LIST;
    }

    public String getClassName() {
        return className;
    }

    public String getFqcn() {
        return fqcn;
    }

    public String getName() {
        return displayName;
    }

    public boolean requireSdkDependency() {
        return requireSdkDependency;
    }

    public String getSampleInputJsonFile() {
        return sampleInputJsonFile;
    }

}
