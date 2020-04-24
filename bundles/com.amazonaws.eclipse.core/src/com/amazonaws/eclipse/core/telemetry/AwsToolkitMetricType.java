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
package com.amazonaws.eclipse.core.telemetry;

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
    EXPLORER_CODEDEPLOY_REFRESH_DEPLOYMENT_GROUP_EDITOR("codedeploy_refreshGroup"),
    EXPLORER_CODEDEPLOY_OPEN_DEPLOYMENT_GROUP("codedeploy_openGroup"),
    /* Explorer CodeCommit Actions */
    EXPLORER_CODECOMMIT_REFRESH_REPO_EDITOR("codecommit_refreshEditor"),
    EXPLORER_CODECOMMIT_CREATE_REPO("codecommit_create"),
    EXPLORER_CODECOMMIT_CLONE_REPO("codecommit_clone"),
    EXPLORER_CODECOMMIT_DELETE_REPO("codecommit_delete"),
    EXPLORER_CODECOMMIT_OPEN_REPO_EDITOR("codecommit_openEditor"),
    /* Explorer Beanstalk Actions */
    EXPLORER_BEANSTALK_OPEN_ENVIRONMENT_EDITOR("beanstalk_openEditor"),
    EXPLORER_BEANSTALK_DELETE_APPLICATION("beanstalk_deleteApplication"),
    EXPLORER_BEANSTALK_TERMINATE_ENVIRONMENT("beanstalk_terminateEnvironment"),
    EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR("beanstalk_refreshEditor"),
    EXPLORER_BEANSTALK_IMPORT_TEMPLATE("beanstalk_importTemplate"),
    EXPLORER_BEANSTALK_EXPORT_TEMPLATE("beanstalk_exportTemplate"),
    /* Explorer S3 Actions */
    EXPLORER_S3_CREATE_BUCKET("s3_createBucket"),
    EXPLORER_S3_OPEN_BUCKET_EDITOR("s3_openEditor"),
    EXPLORER_S3_REFRESH_BUCKET_EDITOR("s3_refreshEditor"),
    EXPLORER_S3_DELETE_BUCKET("s3_deleteBucket"),
    EXPLORER_S3_DELETE_OBJECTS("s3_deleteObjects"),
    EXPLORER_S3_GENERATE_PRESIGNED_URL("s3_genereatePresignedUrl"),
    EXPLORER_S3_EDIT_OBJECT_TAGS("s3_editObjectTags"),
    EXPLORER_S3_EDIT_OBJECT_PERMISSIONS("s3_editObjectPermissions"),
    /* Explorer EC2 Actions */
    EXPLORER_EC2_OPEN_VIEW("ec2_openEditor"),
    EXPLORER_EC2_OPEN_AMIS_VIEW("ec2_openAmis"),
    EXPLORER_EC2_OPEN_INSTANCES_VIEW("ec2_openInstances"),
    EXPLORER_EC2_OPEN_EBS_VIEW("ec2_openEbs"),
    EXPLORER_EC2_OPEN_SECURITY_GROUPS_VIEW("ec2_openSecurityGroups"),
    EXPLORER_EC2_NEW_SECURITY_GROUP("ec2_createSecurityGroup"),
    EXPLORER_EC2_DELETE_SECURITY_GROUP("ec2_deleteSecurityGroup"),
    EXPLORER_EC2_ADD_PERMISSIONS_TO_SECURITY_GROUP("ec2_addPermissionToSecurityGroup"),
    EXPLORER_EC2_REMOVE_PERMISSIONS_FROM_SECURITY_GROUP("ec2_removePermissionFromSecurityGroup"),
    EXPLORER_EC2_REFRESH_SECURITY_GROUP("ec2_refreshSecurityGroup"),
    EXPLORER_EC2_OPEN_SHELL_ACTION("ec2_openShell"),
    EXPLORER_EC2_REBOOT_ACTION("ec2_reboot"),
    EXPLORER_EC2_TERMINATE_ACTION("ec2_terminate"),
    EXPLORER_EC2_CREATE_AMI_ACTION("ec2_createAmi"),
    EXPLORER_EC2_COPY_PUBLIC_DNS_NAME_ACTION("ec2_copyDnsName"),
    EXPLORER_EC2_ATTACH_NEW_VOLUME_ACTION("ec2_attachVolume"),
    EXPLORER_EC2_START_INSTANCES_ACTION("ec2_startInstance"),
    EXPLORER_EC2_STOP_INSTANCES_ACTION("ec2_stopInstance"),
    /* Aws level Events */
    AWS_NEW_JAVA_PROJECT_WIZARD("project_new"),
    /* Dynamodb Events */
    DYNAMODB_INSTALL_TEST_TOOL("dynamo_installTestTool"),
    DYNAMODB_UNINSTALL_TEST_TOOL("dynamo_uninstallTestTool"),
    /* Ec2 Events */
    EC2_LAUNCH_INSTANCES("Ec2-LaunchInstances"),
    /* Lambda Events */
    LAMBDA_NEW_LAMBDA_FUNCTION_WIZARD("project_new"),
    LAMBDA_NEW_LAMBDA_PROJECT_WIZARD("project_new"),
    LAMBDA_NEW_SERVERLESS_PROJECT_WIZARD("sam_init"),
    LAMBDA_UPLOAD_FUNCTION_WIZARD("lambda_deploy"),
    LAMBDA_INVOKE_FUNCTION_DIALOG("lambda_invokeRemote"),
    LAMBDA_DEPLOY_SERVERLESS_PROJECT_WIZARD("sam_deploy"),
    /* SAM Local Events */
    SAMLOCAL_GENERATE_EVENT("sam_generateEvent"),
    SAMLOCAL_LAUNCH("lambda_invokeLocal"),
    ;

    private final String metricName;

    private AwsToolkitMetricType(String metricName) {
        this.metricName = metricName;
    }

    public String getName() {
        return metricName;
    }
}
