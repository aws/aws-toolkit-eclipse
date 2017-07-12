/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.diagnostic.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.AmazonServiceException;

public class ServiceExceptionParser {

    private static final String ACCESS_DENIED = "AccessDenied";
    private static final String OPERATION_NOT_ALLOWED_MSG_REGEX = "(.+) is not authorized to perform: (.+) on resource: (.+)";

    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String ACTION    = "ACTION";
    public static final String RESOURCE  = "RESOURCE";

    /**
     * Returns true if the given service exception indicates that the current
     * IAM user doesn't have sufficient permission to perform the operation.
     */
    public static boolean isOperationNotAllowedException(Exception e) {
        return e instanceof AmazonServiceException
                && parseOperationNotAllowedException((AmazonServiceException) e) != null;
    }

    /**
     * Returns a map of properties parsed from the given exception, or null if
     * the exception doesn't indicate a operation-not-allowed error.
     * <p>
     * The following properties are available in the returned map:
     * <ul>
     *  <li>PRINCIPAL</li>
     *  <li>ACTION</li>
     *  <li>RESOURCE</li>
     * </ul>
     */
    public static Map<String, String> parseOperationNotAllowedException(AmazonServiceException ase) {
        if (ase != null
                && ACCESS_DENIED.equalsIgnoreCase(ase.getErrorCode())
                && ase.getErrorMessage() != null) {

            Pattern p = Pattern.compile(OPERATION_NOT_ALLOWED_MSG_REGEX);
            Matcher m = p.matcher(ase.getErrorMessage());

            if (m.matches()) {
                Map<String, String> properties = new HashMap<>();
                properties.put(PRINCIPAL, m.group(1));
                properties.put(ACTION, m.group(2));
                properties.put(RESOURCE, m.group(3));

                return properties;
            }
        }

        return null;
    }

}
