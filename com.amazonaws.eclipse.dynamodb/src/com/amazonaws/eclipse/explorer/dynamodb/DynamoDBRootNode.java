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
package com.amazonaws.eclipse.explorer.dynamodb;

import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.explorer.ExplorerNode;


/**
 * Root node for DynamoDB resources in the AWS explorer
 */
public class DynamoDBRootNode extends ExplorerNode {

    public static final DynamoDBRootNode NODE = new DynamoDBRootNode();

    private DynamoDBRootNode() {
        super("Amazon DynamoDB", 1, DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_DYNAMODB_SERVICE));
    }

}
