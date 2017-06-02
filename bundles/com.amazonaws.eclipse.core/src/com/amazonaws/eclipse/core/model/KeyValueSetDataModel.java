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

public class KeyValueSetDataModel {

    private final int maxPairs;
    private final List<Pair> pairSet;

    /**
     * @param maxPairs - Maximum amount allowed for the PairSet. -1 indicates unlimited!
     * @param pairSet - The actually set of key-value pairs.
     */
    public KeyValueSetDataModel(int maxPairs, List<Pair> pairSet) {
        this.maxPairs = maxPairs;
        this.pairSet = pairSet;
    }

    public int getMaxPairs() {
        return maxPairs;
    }

    public List<Pair> getPairSet() {
        return pairSet;
    }

    public boolean isUnlimitedPairs() {
        return maxPairs < 0;
    }

    public static class Pair {
        public static final String P_KEY = "key";
        public static final String P_VALUE = "value";

        private String key;
        private String value;

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
