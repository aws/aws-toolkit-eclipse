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

import java.sql.Connection;

import org.eclipse.datatools.connectivity.sqm.core.definition.DatabaseDefinition;
import org.eclipse.datatools.connectivity.sqm.internal.core.definition.DatabaseDefinitionRegistryImpl;
import org.eclipse.datatools.connectivity.sqm.internal.core.definition.IDatabaseRecognizer;

public class SimpleDBDatabaseRecognizer implements IDatabaseRecognizer {
  public static final String PRODUCT = "SimpleDB"; //$NON-NLS-1$
  public static final String VERSION1 = "1.0"; //$NON-NLS-1$

  @Override
public DatabaseDefinition recognize(final Connection connection) {
    try {
      String product = connection.getMetaData().getDatabaseProductName();
      if (product.indexOf(PRODUCT) < 0) {
        return null;
      }

      //      String version = connection.getMetaData().getDatabaseProductVersion();
      //      if (version == null) {
      //        return null;
      //      }
      //
      //      Pattern p = Pattern.compile("[\\d]+[.][\\d]+[.][\\d]+"); //$NON-NLS-1$
      //      Matcher m = p.matcher(version);
      //      m.find();
      //      version = m.group();
      //      if (version.startsWith("1.")) { //$NON-NLS-1$
      return DatabaseDefinitionRegistryImpl.INSTANCE.getDefinition(PRODUCT, VERSION1);
      //      }
    } catch (Exception e) {
    }

    return null;
  }
}
