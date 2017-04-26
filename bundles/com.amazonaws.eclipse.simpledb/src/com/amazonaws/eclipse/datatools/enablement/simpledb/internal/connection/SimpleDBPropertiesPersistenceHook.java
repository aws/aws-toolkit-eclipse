/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.connection;

import java.util.Properties;

import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.JDBCPasswordPropertyPersistenceHook;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.ISimpleDBConnectionProfileConstants;

public class SimpleDBPropertiesPersistenceHook extends JDBCPasswordPropertyPersistenceHook {

    @Override
    public String getConnectionPropertiesPageID() {
        return "com.amazonaws.eclipse.datatools.enablement.simpledb.profileProperties"; //$NON-NLS-1$
    }

    @Override
    public boolean arePropertiesComplete(final Properties props) {
        String uid = props.getProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID);
        String pwd = props.getProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID);
        String accountId = props.getProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID);

        /*
         * Legacy support: only set their user and pass if they hadn't
         * explicitly set it before.
         */
        if ( uid == null || uid.length() == 0 || pwd == null || pwd.length() == 0 ) {
            AccountInfo accountInfo = null;
            if ( accountId != null ) {
                accountInfo = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(accountId);
            } else {
                accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
            }
            return accountInfo.isValid();
        }

        return persistPassword(props) || props.getProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID, null) != null;
    }

}
