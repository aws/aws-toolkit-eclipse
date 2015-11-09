/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda;

public class LambdaAnalytics {

    /*
     * Upload function wizard
     */
    public static final String EVENT_TYPE_UPLOAD_FUNCTION_WIZARD = "Lambda-UploadFunctionWizard";

    public static final String ATTR_NAME_OPENED_FROM = "OpenedFrom";
    public static final String ATTR_VALUE_PROJECT_CONTEXT_MENU = "ProjectContextMenu";
    public static final String ATTR_VALUE_FILE_EDITOR_CONTEXT_MENU = "FileEditorContextMenu";
    public static final String ATTR_VALUE_UPLOAD_BEFORE_INVOKE = "UploadBeforeInvoke";

    public static final String ATTR_NAME_CHANGE_SELECTION = "ChangeSelection";
    public static final String ATTR_VALUE_REGION_SELECTION_COMBO = "RegionSelectionCombo";
    public static final String ATTR_VALUE_FUNCTION_HANDLER_SELECTION_COMBO = "FunctionHandlerSelectionCombo";
    public static final String ATTR_VALUE_IAM_ROLE_SELECTION_COMBO = "IamRoleSelectionCombo";
    public static final String ATTR_VALUE_S3_BUCKET_SELECTION_COMBO = "S3BucketSelectionCombo";

    public static final String ATTR_NAME_CLICK = "Click";
    public static final String ATTR_VALUE_CREATE_IAM_ROLE_BUTTON = "CreateIamRoleButton";
    public static final String ATTR_VALUE_CREATE_S3_BUCKET_BUTTON = "CreateS3BucketButton";

    public static final String ATTR_NAME_END_RESULT = "EndResult";
    public static final String ATTR_VALUE_SUCCEEDED = "Succeeded";
    public static final String ATTR_VALUE_FAILED = "Failed";
    public static final String ATTR_VALUE_CANCELED = "Canceled";

    public static final String METRIC_NAME_IS_CREATING_NEW_FUNCTION = "IsCreatingNewFunction";
    public static final String METRIC_NAME_VALID_FUNCTION_HANDLER_CLASS_COUNT = "ValidFunctionHandlerClassCount";
    public static final String METRIC_NAME_LOAD_IAM_ROLE_TIME_DURATION_MS = "LoadIamRoleTimeDurationMs";
    public static final String METRIC_NAME_LOAD_S3_BUCKET_TIME_DURATION_MS = "LoadS3BucketTimeDurationMs";

    /*
     * Invoke function dialog
     */
    public static final String EVENT_TYPE_INVOKE_FUNCTION_DIALOG = "Lambda-InvokeFunctionDialog";

    // OpenedFrom -> ProjectContextMenu/FileEditorContextMenu

    // Change selection
    public static final String ATTR_VALUE_INVOKE_INPUT_FILE_SELECTION_COMBO = "InvokeInputFileSelectionCombo";

    // End result -> Succeeded/Failed/Canceled

    public static final String METRIC_NAME_IS_INVOKE_INPUT_MODIFIED = "IsInvokeInputModified";
    public static final String METRIC_NAME_IS_PROJECT_MODIFIED_AFTER_LAST_INVOKE = "IsProjectModifiedAfterLastInvoke";

    /*
     * New Lambda function wizard
     */
    public static final String EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD = "Lambda-NewLambdaProjectWizard";

    public static final String ATTR_NAME_FUNCTION_INPUT_TYPE = "FunctionInputType";
    public static final String ATTR_NAME_FUNCTION_OUTPUT_TYPE = "FunctionOutputType";

    // End result -> Succeeded/Failed/Canceled
}
