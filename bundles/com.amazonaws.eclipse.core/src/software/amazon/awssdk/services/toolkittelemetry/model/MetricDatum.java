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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class MetricDatum implements SdkPojo, Serializable, ToCopyableBuilder<MetricDatum.Builder, MetricDatum> {
    private static final SdkField<String> METRIC_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(MetricDatum::metricName)).setter(setter(Builder::metricName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MetricName").build()).build();

    private static final SdkField<Long> EPOCH_TIMESTAMP_FIELD = SdkField.<Long> builder(MarshallingType.LONG)
            .getter(getter(MetricDatum::epochTimestamp)).setter(setter(Builder::epochTimestamp))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("EpochTimestamp").build()).build();

    private static final SdkField<String> UNIT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(MetricDatum::unitAsString)).setter(setter(Builder::unit))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Unit").build()).build();

    private static final SdkField<Double> VALUE_FIELD = SdkField.<Double> builder(MarshallingType.DOUBLE)
            .getter(getter(MetricDatum::value)).setter(setter(Builder::value))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Value").build()).build();

    private static final SdkField<List<MetadataEntry>> METADATA_FIELD = SdkField
            .<List<MetadataEntry>> builder(MarshallingType.LIST)
            .getter(getter(MetricDatum::metadata))
            .setter(setter(Builder::metadata))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Metadata").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<MetadataEntry> builder(MarshallingType.SDK_POJO)
                                            .constructor(MetadataEntry::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(METRIC_NAME_FIELD,
            EPOCH_TIMESTAMP_FIELD, UNIT_FIELD, VALUE_FIELD, METADATA_FIELD));

    private static final long serialVersionUID = 1L;

    private final String metricName;

    private final Long epochTimestamp;

    private final String unit;

    private final Double value;

    private final List<MetadataEntry> metadata;

    private MetricDatum(BuilderImpl builder) {
        this.metricName = builder.metricName;
        this.epochTimestamp = builder.epochTimestamp;
        this.unit = builder.unit;
        this.value = builder.value;
        this.metadata = builder.metadata;
    }

    /**
     * Returns the value of the MetricName property for this object.
     * 
     * @return The value of the MetricName property for this object.
     */
    public String metricName() {
        return metricName;
    }

    /**
     * Returns the value of the EpochTimestamp property for this object.
     * 
     * @return The value of the EpochTimestamp property for this object.
     */
    public Long epochTimestamp() {
        return epochTimestamp;
    }

    /**
     * Returns the value of the Unit property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #unit} will return
     * {@link Unit#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #unitAsString}.
     * </p>
     * 
     * @return The value of the Unit property for this object.
     * @see Unit
     */
    public Unit unit() {
        return Unit.fromValue(unit);
    }

    /**
     * Returns the value of the Unit property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #unit} will return
     * {@link Unit#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #unitAsString}.
     * </p>
     * 
     * @return The value of the Unit property for this object.
     * @see Unit
     */
    public String unitAsString() {
        return unit;
    }

    /**
     * Returns the value of the Value property for this object.
     * 
     * @return The value of the Value property for this object.
     */
    public Double value() {
        return value;
    }

    /**
     * Returns true if the Metadata property was specified by the sender (it may be empty), or false if the sender did
     * not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public boolean hasMetadata() {
        return metadata != null && !(metadata instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the Metadata property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasMetadata()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the Metadata property for this object.
     */
    public List<MetadataEntry> metadata() {
        return metadata;
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
        hashCode = 31 * hashCode + Objects.hashCode(metricName());
        hashCode = 31 * hashCode + Objects.hashCode(epochTimestamp());
        hashCode = 31 * hashCode + Objects.hashCode(unitAsString());
        hashCode = 31 * hashCode + Objects.hashCode(value());
        hashCode = 31 * hashCode + Objects.hashCode(metadata());
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
        if (!(obj instanceof MetricDatum)) {
            return false;
        }
        MetricDatum other = (MetricDatum) obj;
        return Objects.equals(metricName(), other.metricName()) && Objects.equals(epochTimestamp(), other.epochTimestamp())
                && Objects.equals(unitAsString(), other.unitAsString()) && Objects.equals(value(), other.value())
                && Objects.equals(metadata(), other.metadata());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("MetricDatum").add("MetricName", metricName()).add("EpochTimestamp", epochTimestamp())
                .add("Unit", unitAsString()).add("Value", value()).add("Metadata", metadata()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "MetricName":
            return Optional.ofNullable(clazz.cast(metricName()));
        case "EpochTimestamp":
            return Optional.ofNullable(clazz.cast(epochTimestamp()));
        case "Unit":
            return Optional.ofNullable(clazz.cast(unitAsString()));
        case "Value":
            return Optional.ofNullable(clazz.cast(value()));
        case "Metadata":
            return Optional.ofNullable(clazz.cast(metadata()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<MetricDatum, T> g) {
        return obj -> g.apply((MetricDatum) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, MetricDatum> {
        /**
         * Sets the value of the MetricName property for this object.
         *
         * @param metricName
         *        The new value for the MetricName property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder metricName(String metricName);

        /**
         * Sets the value of the EpochTimestamp property for this object.
         *
         * @param epochTimestamp
         *        The new value for the EpochTimestamp property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder epochTimestamp(Long epochTimestamp);

        /**
         * Sets the value of the Unit property for this object.
         *
         * @param unit
         *        The new value for the Unit property for this object.
         * @see Unit
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see Unit
         */
        Builder unit(String unit);

        /**
         * Sets the value of the Unit property for this object.
         *
         * @param unit
         *        The new value for the Unit property for this object.
         * @see Unit
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see Unit
         */
        Builder unit(Unit unit);

        /**
         * Sets the value of the Value property for this object.
         *
         * @param value
         *        The new value for the Value property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder value(Double value);

        /**
         * Sets the value of the Metadata property for this object.
         *
         * @param metadata
         *        The new value for the Metadata property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder metadata(Collection<MetadataEntry> metadata);

        /**
         * Sets the value of the Metadata property for this object.
         *
         * @param metadata
         *        The new value for the Metadata property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder metadata(MetadataEntry... metadata);

        /**
         * Sets the value of the Metadata property for this object.
         *
         * This is a convenience that creates an instance of the {@link List<MetadataEntry>.Builder} avoiding the need
         * to create one manually via {@link List<MetadataEntry>#builder()}.
         *
         * When the {@link Consumer} completes, {@link List<MetadataEntry>.Builder#build()} is called immediately and
         * its result is passed to {@link #metadata(List<MetadataEntry>)}.
         * 
         * @param metadata
         *        a consumer that will call methods on {@link List<MetadataEntry>.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #metadata(List<MetadataEntry>)
         */
        Builder metadata(Consumer<MetadataEntry.Builder>... metadata);
    }

    static final class BuilderImpl implements Builder {
        private String metricName;

        private Long epochTimestamp;

        private String unit;

        private Double value;

        private List<MetadataEntry> metadata = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(MetricDatum model) {
            metricName(model.metricName);
            epochTimestamp(model.epochTimestamp);
            unit(model.unit);
            value(model.value);
            metadata(model.metadata);
        }

        public final String getMetricName() {
            return metricName;
        }

        @Override
        public final Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public final void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public final Long getEpochTimestamp() {
            return epochTimestamp;
        }

        @Override
        public final Builder epochTimestamp(Long epochTimestamp) {
            this.epochTimestamp = epochTimestamp;
            return this;
        }

        public final void setEpochTimestamp(Long epochTimestamp) {
            this.epochTimestamp = epochTimestamp;
        }

        public final String getUnitAsString() {
            return unit;
        }

        @Override
        public final Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public final Builder unit(Unit unit) {
            this.unit(unit == null ? null : unit.toString());
            return this;
        }

        public final void setUnit(String unit) {
            this.unit = unit;
        }

        public final Double getValue() {
            return value;
        }

        @Override
        public final Builder value(Double value) {
            this.value = value;
            return this;
        }

        public final void setValue(Double value) {
            this.value = value;
        }

        public final Collection<MetadataEntry.Builder> getMetadata() {
            return metadata != null ? metadata.stream().map(MetadataEntry::toBuilder).collect(Collectors.toList()) : null;
        }

        @Override
        public final Builder metadata(Collection<MetadataEntry> metadata) {
            this.metadata = MetadataCopier.copy(metadata);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder metadata(MetadataEntry... metadata) {
            metadata(Arrays.asList(metadata));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder metadata(Consumer<MetadataEntry.Builder>... metadata) {
            metadata(Stream.of(metadata).map(c -> MetadataEntry.builder().applyMutation(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final void setMetadata(Collection<MetadataEntry.BuilderImpl> metadata) {
            this.metadata = MetadataCopier.copyFromBuilder(metadata);
        }

        @Override
        public MetricDatum build() {
            return new MetricDatum(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
