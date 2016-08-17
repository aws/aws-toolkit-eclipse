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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.osgi.framework.Bundle;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.diagnostic.model.ErrorReportDataModel;
import com.amazonaws.eclipse.core.diagnostic.model.PlatformEnvironmentDataModel;
import com.amazonaws.util.SdkHttpUtils;

/**
 * A util class responsible for sending error report data to the Amazon
 * HTMLForms system.
 */
public class AwsPortalFeedbackFormUtils {

    private static final String BUG_REPORT_FORM_NAME = "aws-eclipse-error-report";
    private static final String FORM_URL = String.format("https://aws.amazon.com/forms/%s", BUG_REPORT_FORM_NAME);

    /** Used for extracting authenticity_token from the form HTML page */
    private static final String AWS_FORM_INPUT_SELECTOR = "form.aws-form input";
    private static final String AUTHENTICITY_TOKEN = "authenticity_token";

    /**
     * Each form field will be rendered as an HTML form input element with a
     * name of "HTMLFormsForm-{fieldName}". We need to use the same field name
     * for the POST request.
     */
    private static final String FORM_FIELD_PREFIX = "HTMLFormsForm-";

    /** All the fields supported by the form */
    private static final String EMAIL                 = "email";
    private static final String USER_DESCRIPTION      = "user-description";
    private static final String ERROR_STACKTRACE      = "error-stacktrace";
    private static final String ERROR_STATUS_MESSAGE  = "error-status-message";
    private static final String ECLIPSE_PLATFORM_ENV  = "eclipse-platform-env";
    private static final String INSTALLED_PLUGINS     = "installed-plugins";

    /**
     * Send the error report data to the Amazon HTMLForms system.
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

        /*
         * We need to reuse the same http-client instance for both GET and POST
         * operations. By doing this, we can rely on the http-client library to
         * automatically handle session cookies that are required by the Brew system
         * (e.g. '_awshome-brew_session' is returned via Set-Cookie header in the
         * GET response, and we have to include the same cookie value in the next
         * POST request.)
         */
        final DefaultHttpClient formHttpClient = new DefaultHttpClient();

        String token = getFreshAuthenticityToken(formHttpClient);
        String payload = generateFormPostContent(reportData, token);

        sendFormPostRequest(formHttpClient, payload);
    }

    /**
     * Returns the value of 'authenticity_token' by requesting the form page
     * from 'aws.amazon.com/forms'.
     *
     * @param httpClient
     *            The http-client instance to use when sending the GET request.
     */
    private static String getFreshAuthenticityToken(final HttpClient httpClient) {
        Document freshForm;
        try {
            HttpGet getForm = new HttpGet(FORM_URL);

            HttpResponse response = httpClient.execute(getForm);
            String formPageContent = IOUtils.toString(response.getEntity().getContent());

            freshForm = Jsoup.parse(formPageContent);
        } catch (IOException ioe) {
            throw new AmazonClientException("Cannot get the form page from "
                    + FORM_URL, ioe);
        }

        for (Element formInput : freshForm.select(AWS_FORM_INPUT_SELECTOR)) {
            if (formInput.attr("name").equals(AUTHENTICITY_TOKEN)) {
                return formInput.attr("value");
            }
        }

        throw new AmazonClientException("Failed to extract " + AUTHENTICITY_TOKEN
                + " from " + FORM_URL);
    }

    /**
     * Serialize the specified error report data into "x-www-form-urlencoded"
     * format. This method also appends additional parameters that are
     * implicitly required by the Amazon HTMLForms system (including
     * authenticity_token).
     */
    private static String generateFormPostContent(
            final ErrorReportDataModel reportData,
            final String authentityToken) {
        StringBuilder content = new StringBuilder();

        // These are the additional fields required by the POST API
        content.append(AUTHENTICITY_TOKEN).append("=")
               .append(SdkHttpUtils.urlEncode(authentityToken, false));
        content.append("&_method=put");


        // These are the "real" data fields

        /* ============= User email ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(EMAIL).append("=");
        content.append(SdkHttpUtils.urlEncode(reportData.getUserEmail(), false));

        /* ============= User description of the error ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(USER_DESCRIPTION).append("=");
        content.append(SdkHttpUtils.urlEncode(reportData.getUserDescription(), false));

        /* ============= Error stack trace ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(ERROR_STACKTRACE).append("=");
        content.append(SdkHttpUtils.urlEncode(
                getStackTraceFromThrowable(reportData.getBug()), false));

        /* ============= Error status message ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(ERROR_STATUS_MESSAGE).append("=");
        content.append(SdkHttpUtils.urlEncode(reportData.getStatusMessage(), false));

        PlatformEnvironmentDataModel env = reportData.getPlatformEnv();

        /* ============= Platform environment ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(ECLIPSE_PLATFORM_ENV).append("=");

        if (env != null) {
            StringWriter eclipsePlatformEnv = new StringWriter();
            PrintWriter pw = new PrintWriter(eclipsePlatformEnv);
            pw.print("Eclipse platform version : ");
            pw.println(env.getEclipsePlatformVersion());

            pw.print("OS name : ");
            pw.println(env.getOsName());
            pw.print("OS version : ");
            pw.println(env.getOsVersion());
            pw.print("OS architecture : ");
            pw.println(env.getOsArch());
            pw.print("JVM name : ");
            pw.println(env.getJavaVmName());
            pw.print("JVM version : ");
            pw.println(env.getJavaVmVersion());
            pw.print("Java lang version : ");
            pw.println(env.getJavaVersion());
            pw.println();
            pw.println();

            content.append(SdkHttpUtils.urlEncode(eclipsePlatformEnv.toString(), false));
        }

        /* ============= Installed Plug-ins ============= */
        content.append("&");
        content.append(FORM_FIELD_PREFIX).append(INSTALLED_PLUGINS).append("=");

        if (env != null) {
            StringWriter installedPlugins = new StringWriter();
            PrintWriter pw = new PrintWriter(installedPlugins);

            for (Bundle bundle : env.getInstalledBundles()) {
                pw.println(bundle.toString());
            }

            content.append(SdkHttpUtils.urlEncode(installedPlugins.toString(), false));
        }

        return content.toString();
    }

    /**
     * Send the specified content by a POST request.
     *
     * @param httpClient
     *            The http-client instance to use when sending the POST request.
     * @param payload
     *            The payload of the POST request.
     */
    private static void sendFormPostRequest(final HttpClient httpClient,
            final String payload) throws AmazonClientException, AmazonServiceException {
        HttpPost request = new HttpPost(FORM_URL);

        request.addHeader("Content-Type", "application/x-www-form-urlencoded");

        try {
            request.setEntity(new StringEntity(payload, "UTF-8"));
        } catch (Exception e) {
            throw new AmazonClientException("Unable to send POST request to " + FORM_URL, e);
        }

        HttpResponse response;
        try {
            response = httpClient.execute(request);
        } catch (ClientProtocolException e) {
            throw new AmazonClientException("Unable to send POST request to "
                    + FORM_URL, e);
        } catch (IOException e) {
            throw new AmazonClientException("Unable to send POST request to "
                    + FORM_URL, e);
        }

        if (response.getStatusLine().getStatusCode() / 100 != HttpStatus.SC_OK / 100) {
            String responseContent;
            try {
                responseContent = IOUtils.toString(response.getEntity().getContent());
            } catch (IllegalStateException e) {
                throw new AmazonClientException(
                        "Unable to read POST response content.", e);
            } catch (IOException e) {
                throw new AmazonClientException(
                        "Unable to read POST response content.", e);
            }

            AmazonServiceException ase = new AmazonServiceException(
                    "Unable to send POST request to " + FORM_URL + " : "
                            + responseContent);
            ase.setStatusCode(response.getStatusLine().getStatusCode());

            throw ase;
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

}
