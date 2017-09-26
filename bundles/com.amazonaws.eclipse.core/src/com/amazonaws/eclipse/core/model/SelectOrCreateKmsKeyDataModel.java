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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.model.SelectOrCreateKmsKeyDataModel.KmsKey;
import com.amazonaws.eclipse.core.util.KmsClientUtils;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.KeyListEntry;

public class SelectOrCreateKmsKeyDataModel extends SelectOrCreateDataModel<KmsKey, AwsResourceScopeParamBase> {
    private static final KmsKey LOADING = new KmsKey("Loading...");
    private static final KmsKey NOT_FOUND = new KmsKey("Not Found");
    private static final KmsKey ERROR = new KmsKey("Error");

    public static class KmsKey {
        public static final String KMS_KEY_ALIAS_PREFIX = "alias/";
        private final String presentationText;
        private KeyListEntry key;
        private AliasListEntry alias;

        private KmsKey(String presentationText) {
            this.presentationText = presentationText;
        }

        public KmsKey(KeyListEntry key, AliasListEntry alias) {
            this.presentationText = null;
            this.key = key;
            this.alias = alias;
        }

        public KeyListEntry getKey() {
            return key;
        }

        public AliasListEntry getAlias() {
            return alias;
        }

        public String getPresentationText() {
            if (presentationText != null) {
                return presentationText;
            }
            return alias == null ? key.getKeyId() : alias.getAliasName().substring(KMS_KEY_ALIAS_PREFIX.length());
        }
    }

    @Override
    public KmsKey getLoadingItem() {
        return LOADING;
    }

    @Override
    public KmsKey getNotFoundItem() {
        return NOT_FOUND;
    }

    @Override
    public KmsKey getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return "Kms Key";
    }

    @Override
    public String getDefaultResourceName() {
        return "lambda-function-kms-key";
    }

    @Override
    public List<KmsKey> loadAwsResources(AwsResourceScopeParamBase param) {
        AWSKMS kmsClient = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getKmsClientByRegion(param.getRegionId());
        List<KeyListEntry> keys = KmsClientUtils.listKeys(kmsClient);
        Map<String, AliasListEntry> aliasList = KmsClientUtils.listAliases(kmsClient);
        return keys.stream()
                    .map(key -> new KmsKey(key, aliasList.get(key.getKeyId())))
                    .collect(Collectors.toList());
    }

    @Override
    public String getResourceName(KmsKey resource) {
        return resource.getPresentationText();
    }
}
