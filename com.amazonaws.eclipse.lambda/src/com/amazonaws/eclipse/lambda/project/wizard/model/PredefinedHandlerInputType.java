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

public enum PredefinedHandlerInputType {

    S3_EVENT("S3Event", "com.amazonaws.services.lambda.runtime.events.S3Event", "S3 Event", true, "s3-event.put.json"),
    SNS_EVENT("SNSEvent", "com.amazonaws.services.lambda.runtime.events.SNSEvent", "SNS Event", true, "sns-event.json"),
    KINESIS_EVENT("KinesisEvent", "com.amazonaws.services.lambda.runtime.events.KinesisEvent", "Kinesis Event", true, "kinesis-event.json"),
    COGNITO_EVENT("CognitoEvent", "com.amazonaws.services.lambda.runtime.events.CognitoEvent", "Cognito Event", true, "cognito-sync-event.json"),
    DYNAMODB_EVENT("DynamodbEvent", "com.amazonaws.services.lambda.runtime.events.DynamodbEvent", "DynamoDB Event", true, "dynamodb-update-event.json"),
    ;

    private final String className;
    private final String fqcn;
    private final String displayName;
    private final boolean requireSdkDependency;
    private final String sampleInputJsonFile;

    PredefinedHandlerInputType(String className, String fqcn,
            String displayName, boolean requireSdkDependency,
            String sampleInputJsonFile) {

        this.className = className;
        this.fqcn = fqcn;
        this.displayName = displayName;
        this.requireSdkDependency = requireSdkDependency;
        this.sampleInputJsonFile = sampleInputJsonFile;
    }

    public String getClassName() {
        return className;
    }

    public String getFqcn() {
        return fqcn;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requireSdkDependency() {
        return requireSdkDependency;
    }

    public String getSampleInputJsonFile() {
        return sampleInputJsonFile;
    }

}
