/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.rds;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.rds.RDSExplorerActionProvider.ConfigureConnectionProfileAction;
import com.amazonaws.services.rds.model.DBInstance;

public class RDSExplorerNodes {
    public static final class RdsRootElement {
        public static final RdsRootElement RDS_ROOT_NODE = new RdsRootElement();
    }

    public static class DatabaseNode extends ExplorerNode {
        private final DBInstance dbInstance;

        public DatabaseNode(DBInstance dbInstance) {
            super(dbInstance.getDBInstanceIdentifier(), 0, loadImage(AwsToolkitCore.IMAGE_DATABASE), new ConfigureConnectionProfileAction(dbInstance));
            this.dbInstance = dbInstance;
        }

        public DBInstance getDBInstance() {
            return dbInstance;
        }
    }
}