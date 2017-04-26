/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.git.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class DateUtils {
    private static final SimpleDateFormat DATE_STAMP_FORMAT      = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat DATE_TIME_STAMP_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    static {
        DATE_STAMP_FORMAT.setTimeZone(new SimpleTimeZone(0, "UTC"));
        DATE_TIME_STAMP_FORMAT.setTimeZone(new SimpleTimeZone(0, "UTC"));
    }

    public static synchronized String formatDateStamp(final Date date) {
        return DATE_STAMP_FORMAT.format(date);
    }

    public static synchronized String formatDateTimeStamp(final Date date) {
        return DATE_TIME_STAMP_FORMAT.format(date);
    }
    
}