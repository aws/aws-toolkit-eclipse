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
package com.amazonaws.eclipse.core.diagnostic.model;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.amazonaws.eclipse.core.diagnostic.utils.AwsPortalFeedbackFormUtils;

/**
 * The data model for all the information to be collected in an error report.
 */
public class ErrorReportDataModel {

    private String userEmail;
    private String userDescription;
    private Throwable bug;
    private String statusMessage;

    private PlatformEnvironmentDataModel platformEnv;

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }

    public Throwable getBug() {
        return bug;
    }

    public void setBug(Throwable bug) {
        this.bug = bug;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public PlatformEnvironmentDataModel getPlatformEnv() {
        return platformEnv;
    }

    public void setPlatformEnv(PlatformEnvironmentDataModel platformEnv) {
        this.platformEnv = platformEnv;
    }

    @Override
    public String toString() {
        StringWriter sb = new StringWriter();
        PrintWriter pw = new PrintWriter(sb);

        pw.println("============= User email =============");
        pw.println(getUserEmail());
        pw.println();

        pw.println("============= User description of the error =============");
        pw.println(getUserDescription());
        pw.println();

        pw.println("============= Error stack trace =============");
        pw.println(AwsPortalFeedbackFormUtils.getStackTraceFromThrowable(getBug()));
        pw.println();

        pw.println("============= Error status message =============");
        pw.println(getStatusMessage());
        pw.println();

        pw.println(getPlatformEnv().toString());

        return sb.toString();
    }
}
