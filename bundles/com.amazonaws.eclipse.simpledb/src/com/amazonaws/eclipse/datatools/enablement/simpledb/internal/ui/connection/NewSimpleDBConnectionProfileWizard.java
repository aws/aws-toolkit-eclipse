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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.connection;

import org.eclipse.datatools.connectivity.ui.wizards.ExtensibleNewConnectionProfileWizard;

public class NewSimpleDBConnectionProfileWizard extends ExtensibleNewConnectionProfileWizard {

  public NewSimpleDBConnectionProfileWizard() {
    super(new SimpleDBProfileDetailsWizardPage(
        "com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.connection.SimpleDBProfileDetailsWizardPage")); //$NON-NLS-1$
  }
}
