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
package com.amazonaws.eclipse.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;

/**
 * Utility class for using AWS KMS client.
 */
public class KmsClientUtils {

    public static List<KeyListEntry> listKeys(AWSKMS kmsClient) {
        ListKeysRequest request = new ListKeysRequest();
        ListKeysResult result;
        List<KeyListEntry> keys = new ArrayList<>();

        do {
            result = kmsClient.listKeys(request);
            keys.addAll(result.getKeys());
            request.setMarker(result.getNextMarker());
        } while (result.getNextMarker() != null);

        return keys;
    }

    /**
     * Return a map of KMS key id to its alias. Notice, those don't have an alias are not
     * included in this map.
     */
    public static Map<String, AliasListEntry> listAliases(AWSKMS kmsClient) {
        ListAliasesRequest request = new ListAliasesRequest();
        ListAliasesResult result;
        Map<String, AliasListEntry> aliases = new HashMap<>();

        do {
            result = kmsClient.listAliases(request);
            result.getAliases().stream()
                    .filter(alias -> alias.getTargetKeyId() != null)
                    .forEach(alias -> aliases.put(alias.getTargetKeyId(), alias));
            request.setMarker(result.getNextMarker());
        } while (result.getNextMarker() != null);

        return aliases;
    }
}
