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
package com.amazonaws.eclipse.core.maven;

import java.util.Comparator;

public class MavenArtifactVersionComparator implements Comparator<String> {

    @Override
    public int compare(String left, String right) {
        int[] leftVersion = parseVersion(left);
        int[] rightVersion = parseVersion(right);

        int min = Math.min(leftVersion.length, rightVersion.length);
        for (int i = 0; i < min; i++) {
            if (leftVersion[i] < rightVersion[i]) return 1;
            if (leftVersion[i] > rightVersion[i]) return -1;
        }

        return 0;
    }

    private int[] parseVersion(String version) {
        if (version == null) return new int[0];

        String[] components = version.split("\\.");
        int[] ints = new int[components.length];

        int counter = 0;
        for (String component : components) {
            int versionNumber = Integer.MIN_VALUE;
            try {
                versionNumber = Integer.parseInt(component);
            } catch (NumberFormatException e) {
                // In case the version is not a number, we use the minimum integer to lower the priority.
            }
            ints[counter++] = versionNumber;
        }

        return ints;
    }
}
