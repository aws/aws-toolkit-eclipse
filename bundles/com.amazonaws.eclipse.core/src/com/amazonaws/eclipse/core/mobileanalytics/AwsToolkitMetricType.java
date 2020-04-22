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
    OVERVIEW("aws_loadOverview"),
    /* AWS Explorer Events */
    EXPLORER_LOADING("aws_loadExplorer"),
    /* Explorer Dynamodb Actions */
    EXPLORER_DYNAMODB_OPEN_TABLE_EDITOR("dynamo_openEditor"),
    EXPLORER_DYNAMODB_RUN_SCAN("dynamo_runScan"),
    EXPLORER_DYNAMODB_SAVE("dynamo_save"),
    EXPLORER_DYNAMODB_EXPORT_AS_CSV("dynamo_exportCsv"),
    EXPLORER_DYNAMODB_ADD_NEW_ATTRIBUTE("dynamo_addAttribute"),
    /* Explorer CodeDeploy Actions */
    EXPLORER_CODEDEPLOY_REFRESH_DEPLOYMENT_GROUP_EDITOR("Explorer-CodeDeployRefreshDeploymentGroupEditor"),
    EXPLORER_CODEDEPLOY_OPEN_DEPLOYMENT_GROUP("Explorer-CodeDeployOpenDeploymentGroup"),
    /* Explorer CodeCommit Actions */
    EXPLORER_CODECOMMIT_REFRESH_REPO_EDITOR("Explorer-CodeCommitRefreshRepoEditor"),
    EXPLORER_CODECOMMIT_CREATE_REPO("Explorer-CodeCommitCreateRepo"),
    EXPLORER_CODECOMMIT_CLONE_REPO("Explorer-CodeCommitCloneRepo"),
    EXPLORER_CODECOMMIT_DELETE_REPO("Explorer-CodeCommitDeleteRepo"),
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
    /* Explorer EC2 Actions */
    EXPLORER_EC2_OPEN_VIEW("Explorer-Ec2OpenView"),
    EXPLORER_EC2_OPEN_AMIS_VIEW("Explorer-Ec2OpenAmisView"),
    EXPLORER_EC2_OPEN_INSTANCES_VIEW("Explorer-Ec2OpenInstancesView"),
    EXPLORER_EC2_OPEN_EBS_VIEW("Explorer-Ec2OpenEbsView"),
    EXPLORER_EC2_OPEN_SECURITY_GROUPS_VIEW("Explorer-Ec2OpenSecurityGroupsView"),
    EXPLORER_EC2_SELECT_SECURITY_GROUP("Explorer-Ec2SelectSecurityGroup"),
    EXPLORER_EC2_NEW_SECURITY_GROUP("Explorer-Ec2NewSecurityGroup"),
    EXPLORER_EC2_DELETE_SECURITY_GROUP("Explorer-Ec2DeleteSecurityGroup"),
    EXPLORER_EC2_ADD_PERMISSIONS_TO_SECURITY_GROUP("Explorer-Ec2AddPermissionsToSecurityGroup"),
    EXPLORER_EC2_REMOVE_PERMISSIONS_FROM_SECURITY_GROUP("Explorer-Ec2RemovePermissionsFromSecurityGroup"),
    EXPLORER_EC2_REFRESH_SECURITY_GROUP("Explorer-Ec2RefreshSecurityGroup"),
    EXPLORER_EC2_OPEN_SHELL_ACTION("Explorer-Ec2OpenShellAction"),
    EXPLORER_EC2_OPEN_SHELL_DIALOG_ACTION("Explorer-Ec2OpenShellDialogAction"),
    EXPLORER_EC2_REBOOT_ACTION("Explorer-Ec2RebootAction"),
    EXPLORER_EC2_TERMINATE_ACTION("Explorer-Ec2TerminateAction"),
    EXPLORER_EC2_CREATE_AMI_ACTION("Explorer-Ec2CreateAmiAction"),
    EXPLORER_EC2_COPY_PUBLIC_DNS_NAME_ACTION("Explorer-Ec2CopyPublicDnsNameAction"),
    EXPLORER_EC2_ATTACH_NEW_VOLUME_ACTION("Explorer-Ec2AttachNewVolumeAction"),
    EXPLORER_EC2_START_INSTANCES_ACTION("Explorer-Ec2StartInstancesAction"),
    EXPLORER_EC2_STOP_INSTANCES_ACTION("Explorer-Ec2StopInstancesAction"),
    /* Aws level Events */
    AWS_NEW_JAVA_PROJECT_WIZARD("Aws-NewJavaProjectWizard"),
    /* Dynamodb Events */
    DYNAMODB_INSTALL_TEST_TOOL("dynamo_installTestTool"),
    DYNAMODB_UNINSTALL_TEST_TOOL("dynamo_uninstallTestTool"),
    /* Ec2 Events */
    EC2_LAUNCH_INSTANCES("Ec2-LaunchInstances"),
    /* Lambda Events */
    LAMBDA_NEW_LAMBDA_FUNCTION_WIZARD("Lambda-NewLambdaFunctionWizard"),
    LAMBDA_NEW_LAMBDA_PROJECT_WIZARD("Lambda-NewLambdaProjectWizard"),
    LAMBDA_NEW_SERVERLESS_PROJECT_WIZARD("Lambda-NewServerlessProjectWizard"),
    LAMBDA_UPLOAD_FUNCTION_WIZARD("Lambda-UploadFunctionWizard"),
    LAMBDA_INVOKE_FUNCTION_DIALOG("Lambda-InvokeFunctionDialog"),
    LAMBDA_DEPLOY_SERVERLESS_PROJECT_WIZARD("Lambda-DeployServerlessProjectWizard"),
    /* SAM Local Events */
    SAMLOCAL_GENERATE_EVENT("SamLocal-GenerateEvent"),
    SAMLOCAL_LAUNCH("SamLocal-Launch"),
    ;

    private final String metricName;

    private AwsToolkitMetricType(String metricName) {
        this.metricName = metricName;
    }

    public String getName() {
        return metricName;
    }
}
