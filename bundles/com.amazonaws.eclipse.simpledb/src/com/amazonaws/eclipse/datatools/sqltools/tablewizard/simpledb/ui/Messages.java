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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui;

import org.eclipse.osgi.util.NLS;

public class Messages {
    private static final String BUNDLE_NAME = "com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.messages"; //$NON-NLS-1$

    public static String domainDeleteMenu;

    public static String EditorsToBeClosed;

    public static String EditorsToBeClosedMessage;

    public static String ConfirmAttributeDeletion;
    public static String ConfirmAttributeDeletionMessage;

    public static String ConfirmDomainDeletion;

    public static String ConfirmDomainDeletionDescription;

    public static String CreateNewDomain;

    public static String NewDomainName;

    public static String EmptyDomainName;

    public static String InvalidDomainName;

    public static String InvalidDomainNameDescription;

    public static String CreateNewAttribute;

    public static String NewAttributeName;

    public static String EmptyAttributeName;

    public static String attributeDeleteMenu;

    public static String GENERATE_DDL_MENU_TEXT;

    public static String loadDataMenu;

    public static String attributeNewMenu;

    public static String domainNewMenu;

    private Messages() {
    }

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
