/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor;

import org.eclipse.osgi.util.NLS;

public class Messages {
    private static final String BUNDLE_NAME = "com.amazonaws.eclipse.datatools.enablement.simpledb.editor.messages"; //$NON-NLS-1$
    public static String windowTitle;
    public static String pageTitle;
    public static String idWindowTitle;
    public static String idPageTitle;

    public static String dialogTitle;
    public static String dialogDescription;

    public static String labelEditAttributeValues;
    public static String newValue;

    public static String remove;
    public static String add;
    public static String edit;
    public static String valueToLong;
    public static String mainMessage;

    public static String idMainMessage;
    public static String idErrorDialogMessage;
    public static String idErrorDialogTitle;
    public static String idErrorStatusMessage;
    public static String incorrectDataType;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {

    }
}
