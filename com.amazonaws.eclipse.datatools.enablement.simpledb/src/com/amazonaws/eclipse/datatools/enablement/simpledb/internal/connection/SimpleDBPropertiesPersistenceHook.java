/*
 * Copyright 2008-2011 Amazon Technologies, Inc. 
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
    String useGlobal = props.getProperty(ISimpleDBConnectionProfileConstants.USE_GLOBAL_SETTINGS, null);

    if (useGlobal != null && Boolean.parseBoolean(useGlobal) == Boolean.TRUE) {
      AccountInfo info = AwsToolkitCore.getDefault().getAccountInfo();
      return info.getAccessKey() != null && info.getAccessKey().trim().length() > 0 && info.getSecretKey() != null
          && info.getSecretKey().trim().length() > 0;
    }

    return persistPassword(props) || props.getProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID, null) != null;
  }

}
