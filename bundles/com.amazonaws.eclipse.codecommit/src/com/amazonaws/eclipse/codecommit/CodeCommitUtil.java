/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codecommit;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CodeCommitUtil {
    public static String codeCommitTimeToHumanReadible(String time) {
        String[] token = time.split(" ");
        if (token.length != 2) return null;
        long epochTime = Long.parseLong(token[0]);
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date (epochTime*1000));
    }

    /**
     * Return value if it is not null, otherwise, return empty string.
     */
    public static String nonNullString(String value) {
        return value == null ? "" : value;
    }
}
