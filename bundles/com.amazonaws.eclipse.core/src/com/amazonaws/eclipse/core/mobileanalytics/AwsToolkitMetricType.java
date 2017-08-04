/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.mobileanalytics;

/**
 * These are predefined metric types.
 */
public enum AwsToolkitMetricType {
    /* AWS Overview Events */
    OVERVIEW("Overview"),
    /* AWS Explorer Events */
    EXPLORER_LOADING("Explorer-Loading"),
    /* Explorer Dynamodb Actions */
    EXPLORER_DYNAMODB_OPEN_TABLE_EDITOR("Explorer-DynamodbOpenTableEditor"),
    EXPLORER_DYNAMODB_RUN_SCAN("Explorer-DynamodbRunScan"),
    EXPLORER_DYNAMODB_SAVE("Explorer-DynamodbSave"),
    EXPLORER_DYNAMODB_NEXT_PAGE("Explorer-DynamodbNextPage"),
    EXPLORER_DYNAMODB_EXPORT_AS_CSV("Explorer-DynamodbExportAsCsv"),
    EXPLORER_DYNAMODB_ADD_NEW_ATTRIBUTE("Explorer-DynamodbAddNewAttribute"),
    /* Explorer CodeDeploy Actions */
    EXPLORER_CODEDEPLOY_REFRESH_DEPLOYMENT_GROUP_EDITOR("Explorer-CodeDeployRefreshDeploymentGroupEditor"),
    EXPLORER_CODEDEPLOY_OPEN_DEPLOYMENT_GROUP("Explorer-CodeDeployOpenDeploymentGroup"),
    /* Explorer CodeCommit Actions */
    EXPLORER_CODECOMMIT_REFRESH_REPO_EDITOR("Explorer-CodeCommitRefreshRepoEditor"),
    EXPLORER_CODECOMMIT_CREATE_REPO("Explorer-CodeCommitCreateRepo"),
    EXPLORER_CODECOMMIT_CLONE_REPO("Explorer-CodeCommitCloneRepo"),
    EXPLORER_CODECOMMIT_DELETE_REPO("Explorer_CodeCommitDeleteRepo"),
    EXPLORER_CODECOMMIT_OPEN_REPO_EDITOR("Explorer-CodeCommitOpenRepoEditor"),
    /* Explorer Beanstalk Actions */
    EXPLORER_BEANSTALK_OPEN_ENVIRONMENT_EDITOR("Explorer-BeanstalkOpenEnvironmentEditor"),
    EXPLORER_BEANSTALK_DELETE_APPLICATION("Explorer-BeanstalkDeleteApplication"),
    EXPLORER_BEANSTALK_TERMINATE_ENVIRONMENT("Explorer-BeanstalkTerminateEnvironment"),
    EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR("Explorer-BeanstalkRefreshEnvironmentEditor"),
    EXPLORER_BEANSTALK_IMPORT_TEMPLATE("Explorer-BeanstalkImportTemplate"),
    EXPLORER_BEANSTALK_EXPORT_TEMPLATE("Explorer-BeanstalkExportTemplate"),
    /* Explorer S3 Actions */
    EXPLORER_S3_CREATE_BUCKET("Explorer-S3CreateBucket"),
    EXPLORER_S3_OPEN_BUCKET_EDITOR("Explorer-S3OpenBucketEditor"),
    EXPLORER_S3_REFRESH_BUCKET_EDITOR("Explorer-S3RefreshBucketEditor"),
    EXPLORER_S3_DELETE_BUCKET("Explorer-S3DeleteBucket"),
    EXPLORER_S3_DELETE_OBJECT("Explorer-S3DeleteObject"),
    EXPLORER_S3_GENERATE_PRESIGNED_URL("Explorer-S3GeneratePresignedUrl"),
    EXPLORER_S3_EDIT_OBJECT_TAGS("Explorer-S3EditObjectTags"),
    EXPLORER_S3_EDIT_OBJECT_PERMISSIONS("Explorer-S3EditObjectPermissions"),
    /* Dynamodb Events */
    DYNAMODB_INSTALL_TEST_TOOL("Dynamodb-InstallTestTool"),
    DYNAMODB_UNINSTALL_TEST_TOOL("Dynamodb-UninstallTestTool"),
    /* Lambda Events */
    LAMBDA_NEW_LAMBDA_FUNCTION_WIZARD("Lambda-NewLambdaFunctionWizard"),
    LAMBDA_NEW_LAMBDA_PROJECT_WIZARD("Lambda-NewLambdaProjectWizard"),
    LAMBDA_NEW_SERVERLESS_PROJECT_WIZARD("Lambda-NewServerlessProjectWizard"),
    LAMBDA_UPLOAD_FUNCTION_WIZARD("Lambda-UploadFunctionWizard"),
    LAMBDA_INVOKE_FUNCTION_DIALOG("Lambda-InvokeFunctionDialog"),
    LAMBDA_DEPLOY_SERVERLESS_PROJECT_WIZARD("Lambda-DeployServerlessProjectWizard"),
    ;

    private final String metricName;

    private AwsToolkitMetricType(String metricName) {
        this.metricName = metricName;
    }

    public String getName() {
        return metricName;
    }
}
