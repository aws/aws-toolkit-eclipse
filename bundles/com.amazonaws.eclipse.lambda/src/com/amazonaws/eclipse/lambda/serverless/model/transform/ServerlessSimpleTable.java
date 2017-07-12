/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package com.amazonaws.eclipse.lambda.serverless.model.transform;

import com.amazonaws.eclipse.lambda.serverless.model.Resource;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;

public class ServerlessSimpleTable extends Resource {
    private PrimaryKey primaryKey;
    private ProvisionedThroughput provisionedThroughput;

    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }
    public void setPrimaryKey(PrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }
    public void setProvisionedThroughput(ProvisionedThroughput provisionedThroughput) {
        this.provisionedThroughput = provisionedThroughput;
    }
    @Override
    public TypeProperties toTypeProperties() {
        // TODO Auto-generated method stub
        return null;
    }

}
