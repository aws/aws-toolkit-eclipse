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
