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

import java.util.logging.Logger;

import org.eclipse.swt.program.Program;

import com.amazonaws.util.SdkHttpUtils;

/**
 * A utility class responsible for opening an email message.
 */
public class EmailMessageLauncher {

    public static final String AWS_ECLIPSE_FEEDBACK_AT_AMZN = "aws-eclipse-feedback@amazon.com";
    public static final String ECLIPSE_FEEDBACK_SUBJECT = "AWS Eclipse Toolkit General Feedback";

    public static final String AWS_ECLIPSE_ERRORS_AT_AMZN = "aws-eclipse-errors@amazon.com";
    public static final String ECLIPSE_ERROR_REPORT_SUBJECT = "AWS Eclipse Toolkit Error Report";

    private final String recipient;
    private final String subject;
    private final String body;

    private EmailMessageLauncher(String recipient, String subject, String body) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    /**
     * Use "mailto:" link to open an email message via the system preferred
     * email client.
     */
    public void open() {
        try {

            StringBuilder mailto = new StringBuilder()
                .append("mailto:").append(SdkHttpUtils.urlEncode(recipient, false))
                .append("?subject=").append(SdkHttpUtils.urlEncode(subject, false))
                .append("&body=").append(SdkHttpUtils.urlEncode(body, false));

            Program.launch(mailto.toString());

        } catch (Exception e) {
            Logger logger = Logger.getLogger(EmailMessageLauncher.class.getName());

            logger.warning("Unable to open email message to '" + recipient + "': " + e.getMessage());
        }
    }

    /**
     * Create an email launcher that builds a message sent to
     * "aws-eclipse-feedback@amazon.com"
     */
    public static EmailMessageLauncher createEmptyFeedbackEmail() {
        return new EmailMessageLauncher(
                AWS_ECLIPSE_FEEDBACK_AT_AMZN, ECLIPSE_FEEDBACK_SUBJECT, "");
    }

    /**
     * Create an email launcher that builds a message sent to
     * "aws-eclipse-errors@amazon.com"
     */
    public static EmailMessageLauncher createEmptyErrorReportEmail() {
        return new EmailMessageLauncher(
                AWS_ECLIPSE_ERRORS_AT_AMZN, ECLIPSE_ERROR_REPORT_SUBJECT, "");
    }

}
