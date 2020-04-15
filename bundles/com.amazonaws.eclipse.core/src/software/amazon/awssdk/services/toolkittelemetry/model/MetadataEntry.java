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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class MetadataEntry implements SdkPojo, Serializable, ToCopyableBuilder<MetadataEntry.Builder, MetadataEntry> {
    private static final SdkField<String> KEY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(MetadataEntry::key)).setter(setter(Builder::key))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Key").build()).build();

    private static final SdkField<String> VALUE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(MetadataEntry::value)).setter(setter(Builder::value))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Value").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(KEY_FIELD, VALUE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String key;

    private final String value;

    private MetadataEntry(BuilderImpl builder) {
        this.key = builder.key;
        this.value = builder.value;
    }

    /**
     * Returns the value of the Key property for this object.
     * 
     * @return The value of the Key property for this object.
     */
    public String key() {
        return key;
    }

    /**
     * Returns the value of the Value property for this object.
     * 
     * @return The value of the Value property for this object.
     */
    public String value() {
        return value;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(key());
        hashCode = 31 * hashCode + Objects.hashCode(value());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MetadataEntry)) {
            return false;
        }
        MetadataEntry other = (MetadataEntry) obj;
        return Objects.equals(key(), other.key()) && Objects.equals(value(), other.value());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("MetadataEntry").add("Key", key()).add("Value", value()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Key":
            return Optional.ofNullable(clazz.cast(key()));
        case "Value":
            return Optional.ofNullable(clazz.cast(value()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<MetadataEntry, T> g) {
        return obj -> g.apply((MetadataEntry) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, MetadataEntry> {
        /**
         * Sets the value of the Key property for this object.
         *
         * @param key
         *        The new value for the Key property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder key(String key);

        /**
         * Sets the value of the Value property for this object.
         *
         * @param value
         *        The new value for the Value property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder value(String value);
    }

    static final class BuilderImpl implements Builder {
        private String key;

        private String value;

        private BuilderImpl() {
        }

        private BuilderImpl(MetadataEntry model) {
            key(model.key);
            value(model.value);
        }

        public final String getKey() {
            return key;
        }

        @Override
        public final Builder key(String key) {
            this.key = key;
            return this;
        }

        public final void setKey(String key) {
            this.key = key;
        }

        public final String getValue() {
            return value;
        }

        @Override
        public final Builder value(String value) {
            this.value = value;
            return this;
        }

        public final void setValue(String value) {
            this.value = value;
        }

        @Override
        public MetadataEntry build() {
            return new MetadataEntry(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
