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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class MetadataCopier {
    static List<MetadataEntry> copy(Collection<MetadataEntry> metadataParam) {
        if (metadataParam == null || metadataParam instanceof SdkAutoConstructList) {
            return DefaultSdkAutoConstructList.getInstance();
        }
        List<MetadataEntry> metadataParamCopy = new ArrayList<>(metadataParam);
        return Collections.unmodifiableList(metadataParamCopy);
    }

    static List<MetadataEntry> copyFromBuilder(Collection<? extends MetadataEntry.Builder> metadataParam) {
        if (metadataParam == null) {
            return null;
        }
        return copy(metadataParam.stream().map(MetadataEntry.Builder::build).collect(toList()));
    }
}
