/*
 * Copyright 2008-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.diagnostic.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.cognito.AWSCognitoCredentialsProvider;
import com.amazonaws.eclipse.core.telemetry.cognito.identity.ToolkitCachedCognitoIdentityIdProvider;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.services.errorreport.ErrorReporter;
import com.amazonaws.services.errorreport.ErrorReporterClient;
import com.amazonaws.services.errorreport.model.ErrorReportDataModel;
import com.amazonaws.services.errorreport.model.PostReportRequest;
import com.amazonaws.services.errorreport.model.PostReportResult;

/**
 * A util class responsible for sending error report data to the AWS ErrorReport platform.
 */
public class AwsErrorReportUtils {
    private static final ErrorReporter ERROR_REPORTER_CLIENT = buildErrorReporterClient();

    /**
     * Send the error report data to the AWS ErrorReport platform.
     *
     * @throws AmazonClientException
     *             Thrown if the toolkit failed to retrieve a fresh
     *             authenticity_token, or if any client-side error occurred when
     *             sending the POST request and reading the POST response.
     * @throws AmazonServiceException
     *             Thrown if the POST request was rejected with non-2xx status
     *             code.
     */
    public static void reportBugToAws(final ErrorReportDataModel reportData)
            throws AmazonClientException, AmazonServiceException {
        PostReportResult result = ERROR_REPORTER_CLIENT.postReport(new PostReportRequest()
                .errorReportDataModel(reportData));
        if (!result.getErrorReportResult().isSuccess()) {
            throw new RuntimeException(result.getErrorReportResult().getMessage());
        }
    }

    public static String getStackTraceFromThrowable(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        t.printStackTrace(pw);
        pw.println();

        if (t.getCause() != null) {
            t.getCause().printStackTrace(pw);
        }

        return sw.toString();
    }

    private static ErrorReporter buildErrorReporterClient() {
        ToolkitCachedCognitoIdentityIdProvider identityIdProvider = new ToolkitCachedCognitoIdentityIdProvider(
                Constants.COGNITO_IDENTITY_POOL_ID_PROD, AwsToolkitCore.getDefault().getPreferenceStore());
        AWSCredentialsProvider credentialsProvider = new AWSCognitoCredentialsProvider(identityIdProvider);
        return ErrorReporterClient.builder()
                .iamCredentials(credentialsProvider)
                .iamRegion(Constants.COGNITO_IDENTITY_SERVICE_REGION.getName())
                .endpoint(Constants.ERROR_REPORT_SERVICE_ENDPOINT)
                .build();
    }
}