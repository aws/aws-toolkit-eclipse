/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.model;

/**
 * Parameters for targeting a specific type of AWS resources.
 */
public abstract class AbstractAwsResourceScopeParam<T extends AbstractAwsResourceScopeParam<T>> {
    protected final String accountId;
    protected final String regionId;

    public AbstractAwsResourceScopeParam(String accountId, String regionId) {
        this.accountId = accountId;
        this.regionId = regionId;
    }

    public abstract T copy();

    public String getAccountId() {
        return accountId;
    }

    public String getRegionId() {
        return regionId;
    }

    public static class AwsResourceScopeParamBase extends AbstractAwsResourceScopeParam<AwsResourceScopeParamBase> {
        public AwsResourceScopeParamBase(String accountId, String regionId) {
            super(accountId, regionId);
        }

        @Override
        public AwsResourceScopeParamBase copy() {
            return new AwsResourceScopeParamBase(accountId, regionId);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((accountId == null) ? 0 : accountId.hashCode());
            result = prime * result + ((regionId == null) ? 0 : regionId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AwsResourceScopeParamBase other = (AwsResourceScopeParamBase) obj;
            if (accountId == null) {
                if (other.accountId != null)
                    return false;
            } else if (!accountId.equals(other.accountId))
                return false;
            if (regionId == null) {
                if (other.regionId != null)
                    return false;
            } else if (!regionId.equals(other.regionId))
                return false;
            return true;
        }
    }
}