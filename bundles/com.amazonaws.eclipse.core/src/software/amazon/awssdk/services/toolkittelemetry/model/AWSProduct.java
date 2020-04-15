/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.toolkittelemetry.model;

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;

@Generated("software.amazon.awssdk:codegen")
public enum AWSProduct {
    CANARY("canary"),

    AWS_TOOLKIT_FOR_JET_BRAINS("AWS Toolkit For JetBrains"),

    AWS_TOOLKIT_FOR_ECLIPSE("AWS Toolkit For Eclipse"),

    AWS_TOOLKIT_FOR_VISUAL_STUDIO("AWS Toolkit For VisualStudio"),

    AWS_TOOLKIT_FOR_VS_CODE("AWS Toolkit For VS Code"),

    UNKNOWN_TO_SDK_VERSION(null);

    private final String value;

    private AWSProduct(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
     *
     * @param value
     *        real value
     * @return AWSProduct corresponding to the value
     */
    public static AWSProduct fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Stream.of(AWSProduct.values()).filter(e -> e.toString().equals(value)).findFirst().orElse(UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link AWSProduct}s
     */
    public static Set<AWSProduct> knownValues() {
        return Stream.of(values()).filter(v -> v != UNKNOWN_TO_SDK_VERSION).collect(toSet());
    }
}
