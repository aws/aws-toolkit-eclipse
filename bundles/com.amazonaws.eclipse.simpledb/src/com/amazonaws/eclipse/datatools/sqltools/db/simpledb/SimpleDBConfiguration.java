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
package com.amazonaws.eclipse.datatools.sqltools.db.simpledb;

import org.eclipse.datatools.sqltools.core.DatabaseVendorDefinitionId;
import org.eclipse.datatools.sqltools.core.services.SQLService;
import org.eclipse.datatools.sqltools.db.generic.GenericDBConfiguration;


public class SimpleDBConfiguration extends GenericDBConfiguration {

    private static SimpleDBConfiguration _instance = null;
    public static final String[] SERVER_ALIASES = new String[] { Messages.SimpleDB, Messages.AmazonSimpleDB };

    // for eclipse to load this class, we must declare it as public
    public SimpleDBConfiguration() {

    }

    public static SimpleDBConfiguration getInstance() {
        return _instance;
    }

    @Override
    public SQLService getSQLService() {
        return new SimpleDBService();
    }

    @Override
    public boolean recognize(final String product, final String version) {
        DatabaseVendorDefinitionId targetid = new DatabaseVendorDefinitionId(product, version);

        for (int i = 0; i < SERVER_ALIASES.length; i++) {
            DatabaseVendorDefinitionId id = new DatabaseVendorDefinitionId(SERVER_ALIASES[i], getDatabaseVendorDefinitionId()
                    .getVersion());
            if (id.equals(targetid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getAssociatedConnectionProfileType() {
        return new String[] { "com.amazonaws.eclipse.datatools.enablement.simpledb.connectionProfile" }; //$NON-NLS-1$
    }

}
