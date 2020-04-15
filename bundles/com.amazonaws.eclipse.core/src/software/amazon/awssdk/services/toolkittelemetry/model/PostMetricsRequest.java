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
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
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
public final class PostMetricsRequest extends ToolkitTelemetryRequest implements
        ToCopyableBuilder<PostMetricsRequest.Builder, PostMetricsRequest> {
    private static final SdkField<String> AWS_PRODUCT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::awsProductAsString)).setter(setter(Builder::awsProduct))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProduct").build()).build();

    private static final SdkField<String> AWS_PRODUCT_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::awsProductVersion)).setter(setter(Builder::awsProductVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProductVersion").build()).build();

    private static final SdkField<String> CLIENT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::clientID)).setter(setter(Builder::clientID))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ClientID").build()).build();

    private static final SdkField<String> OS_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::os)).setter(setter(Builder::os))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OS").build()).build();

    private static final SdkField<String> OS_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::osVersion)).setter(setter(Builder::osVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OSVersion").build()).build();

    private static final SdkField<String> PARENT_PRODUCT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::parentProduct)).setter(setter(Builder::parentProduct))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ParentProduct").build()).build();

    private static final SdkField<String> PARENT_PRODUCT_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostMetricsRequest::parentProductVersion)).setter(setter(Builder::parentProductVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ParentProductVersion").build())
            .build();

    private static final SdkField<List<MetricDatum>> METRIC_DATA_FIELD = SdkField
            .<List<MetricDatum>> builder(MarshallingType.LIST)
            .getter(getter(PostMetricsRequest::metricData))
            .setter(setter(Builder::metricData))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MetricData").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<MetricDatum> builder(MarshallingType.SDK_POJO)
                                            .constructor(MetricDatum::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(AWS_PRODUCT_FIELD,
            AWS_PRODUCT_VERSION_FIELD, CLIENT_ID_FIELD, OS_FIELD, OS_VERSION_FIELD, PARENT_PRODUCT_FIELD,
            PARENT_PRODUCT_VERSION_FIELD, METRIC_DATA_FIELD));

    private final String awsProduct;

    private final String awsProductVersion;

    private final String clientID;

    private final String os;

    private final String osVersion;

    private final String parentProduct;

    private final String parentProductVersion;

    private final List<MetricDatum> metricData;

    private PostMetricsRequest(BuilderImpl builder) {
        super(builder);
        this.awsProduct = builder.awsProduct;
        this.awsProductVersion = builder.awsProductVersion;
        this.clientID = builder.clientID;
        this.os = builder.os;
        this.osVersion = builder.osVersion;
        this.parentProduct = builder.parentProduct;
        this.parentProductVersion = builder.parentProductVersion;
        this.metricData = builder.metricData;
    }

    /**
     * Returns the value of the AWSProduct property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #awsProduct} will
     * return {@link AWSProduct#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #awsProductAsString}.
     * </p>
     * 
     * @return The value of the AWSProduct property for this object.
     * @see AWSProduct
     */
    public AWSProduct awsProduct() {
        return AWSProduct.fromValue(awsProduct);
    }

    /**
     * Returns the value of the AWSProduct property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #awsProduct} will
     * return {@link AWSProduct#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #awsProductAsString}.
     * </p>
     * 
     * @return The value of the AWSProduct property for this object.
     * @see AWSProduct
     */
    public String awsProductAsString() {
        return awsProduct;
    }

    /**
     * Returns the value of the AWSProductVersion property for this object.
     * 
     * @return The value of the AWSProductVersion property for this object.
     */
    public String awsProductVersion() {
        return awsProductVersion;
    }

    /**
     * Returns the value of the ClientID property for this object.
     * 
     * @return The value of the ClientID property for this object.
     */
    public String clientID() {
        return clientID;
    }

    /**
     * Returns the value of the OS property for this object.
     * 
     * @return The value of the OS property for this object.
     */
    public String os() {
        return os;
    }

    /**
     * Returns the value of the OSVersion property for this object.
     * 
     * @return The value of the OSVersion property for this object.
     */
    public String osVersion() {
        return osVersion;
    }

    /**
     * Returns the value of the ParentProduct property for this object.
     * 
     * @return The value of the ParentProduct property for this object.
     */
    public String parentProduct() {
        return parentProduct;
    }

    /**
     * Returns the value of the ParentProductVersion property for this object.
     * 
     * @return The value of the ParentProductVersion property for this object.
     */
    public String parentProductVersion() {
        return parentProductVersion;
    }

    /**
     * Returns true if the MetricData property was specified by the sender (it may be empty), or false if the sender did
     * not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public boolean hasMetricData() {
        return metricData != null && !(metricData instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the MetricData property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasMetricData()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the MetricData property for this object.
     */
    public List<MetricDatum> metricData() {
        return metricData;
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
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(awsProductAsString());
        hashCode = 31 * hashCode + Objects.hashCode(awsProductVersion());
        hashCode = 31 * hashCode + Objects.hashCode(clientID());
        hashCode = 31 * hashCode + Objects.hashCode(os());
        hashCode = 31 * hashCode + Objects.hashCode(osVersion());
        hashCode = 31 * hashCode + Objects.hashCode(parentProduct());
        hashCode = 31 * hashCode + Objects.hashCode(parentProductVersion());
        hashCode = 31 * hashCode + Objects.hashCode(metricData());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PostMetricsRequest)) {
            return false;
        }
        PostMetricsRequest other = (PostMetricsRequest) obj;
        return Objects.equals(awsProductAsString(), other.awsProductAsString())
                && Objects.equals(awsProductVersion(), other.awsProductVersion()) && Objects.equals(clientID(), other.clientID())
                && Objects.equals(os(), other.os()) && Objects.equals(osVersion(), other.osVersion())
                && Objects.equals(parentProduct(), other.parentProduct())
                && Objects.equals(parentProductVersion(), other.parentProductVersion())
                && Objects.equals(metricData(), other.metricData());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("PostMetricsRequest").add("AWSProduct", awsProductAsString())
                .add("AWSProductVersion", awsProductVersion()).add("ClientID", clientID()).add("OS", os())
                .add("OSVersion", osVersion()).add("ParentProduct", parentProduct())
                .add("ParentProductVersion", parentProductVersion()).add("MetricData", metricData()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "AWSProduct":
            return Optional.ofNullable(clazz.cast(awsProductAsString()));
        case "AWSProductVersion":
            return Optional.ofNullable(clazz.cast(awsProductVersion()));
        case "ClientID":
            return Optional.ofNullable(clazz.cast(clientID()));
        case "OS":
            return Optional.ofNullable(clazz.cast(os()));
        case "OSVersion":
            return Optional.ofNullable(clazz.cast(osVersion()));
        case "ParentProduct":
            return Optional.ofNullable(clazz.cast(parentProduct()));
        case "ParentProductVersion":
            return Optional.ofNullable(clazz.cast(parentProductVersion()));
        case "MetricData":
            return Optional.ofNullable(clazz.cast(metricData()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<PostMetricsRequest, T> g) {
        return obj -> g.apply((PostMetricsRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends ToolkitTelemetryRequest.Builder, SdkPojo, CopyableBuilder<Builder, PostMetricsRequest> {
        /**
         * Sets the value of the AWSProduct property for this object.
         *
         * @param awsProduct
         *        The new value for the AWSProduct property for this object.
         * @see AWSProduct
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see AWSProduct
         */
        Builder awsProduct(String awsProduct);

        /**
         * Sets the value of the AWSProduct property for this object.
         *
         * @param awsProduct
         *        The new value for the AWSProduct property for this object.
         * @see AWSProduct
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see AWSProduct
         */
        Builder awsProduct(AWSProduct awsProduct);

        /**
         * Sets the value of the AWSProductVersion property for this object.
         *
         * @param awsProductVersion
         *        The new value for the AWSProductVersion property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder awsProductVersion(String awsProductVersion);

        /**
         * Sets the value of the ClientID property for this object.
         *
         * @param clientID
         *        The new value for the ClientID property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder clientID(String clientID);

        /**
         * Sets the value of the OS property for this object.
         *
         * @param os
         *        The new value for the OS property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder os(String os);

        /**
         * Sets the value of the OSVersion property for this object.
         *
         * @param osVersion
         *        The new value for the OSVersion property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder osVersion(String osVersion);

        /**
         * Sets the value of the ParentProduct property for this object.
         *
         * @param parentProduct
         *        The new value for the ParentProduct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder parentProduct(String parentProduct);

        /**
         * Sets the value of the ParentProductVersion property for this object.
         *
         * @param parentProductVersion
         *        The new value for the ParentProductVersion property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder parentProductVersion(String parentProductVersion);

        /**
         * Sets the value of the MetricData property for this object.
         *
         * @param metricData
         *        The new value for the MetricData property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder metricData(Collection<MetricDatum> metricData);

        /**
         * Sets the value of the MetricData property for this object.
         *
         * @param metricData
         *        The new value for the MetricData property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder metricData(MetricDatum... metricData);

        /**
         * Sets the value of the MetricData property for this object.
         *
         * This is a convenience that creates an instance of the {@link List<MetricDatum>.Builder} avoiding the need to
         * create one manually via {@link List<MetricDatum>#builder()}.
         *
         * When the {@link Consumer} completes, {@link List<MetricDatum>.Builder#build()} is called immediately and its
         * result is passed to {@link #metricData(List<MetricDatum>)}.
         * 
         * @param metricData
         *        a consumer that will call methods on {@link List<MetricDatum>.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #metricData(List<MetricDatum>)
         */
        Builder metricData(Consumer<MetricDatum.Builder>... metricData);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends ToolkitTelemetryRequest.BuilderImpl implements Builder {
        private String awsProduct;

        private String awsProductVersion;

        private String clientID;

        private String os;

        private String osVersion;

        private String parentProduct;

        private String parentProductVersion;

        private List<MetricDatum> metricData = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(PostMetricsRequest model) {
            super(model);
            awsProduct(model.awsProduct);
            awsProductVersion(model.awsProductVersion);
            clientID(model.clientID);
            os(model.os);
            osVersion(model.osVersion);
            parentProduct(model.parentProduct);
            parentProductVersion(model.parentProductVersion);
            metricData(model.metricData);
        }

        public final String getAwsProductAsString() {
            return awsProduct;
        }

        @Override
        public final Builder awsProduct(String awsProduct) {
            this.awsProduct = awsProduct;
            return this;
        }

        @Override
        public final Builder awsProduct(AWSProduct awsProduct) {
            this.awsProduct(awsProduct == null ? null : awsProduct.toString());
            return this;
        }

        public final void setAwsProduct(String awsProduct) {
            this.awsProduct = awsProduct;
        }

        public final String getAwsProductVersion() {
            return awsProductVersion;
        }

        @Override
        public final Builder awsProductVersion(String awsProductVersion) {
            this.awsProductVersion = awsProductVersion;
            return this;
        }

        public final void setAwsProductVersion(String awsProductVersion) {
            this.awsProductVersion = awsProductVersion;
        }

        public final String getClientID() {
            return clientID;
        }

        @Override
        public final Builder clientID(String clientID) {
            this.clientID = clientID;
            return this;
        }

        public final void setClientID(String clientID) {
            this.clientID = clientID;
        }

        public final String getOs() {
            return os;
        }

        @Override
        public final Builder os(String os) {
            this.os = os;
            return this;
        }

        public final void setOs(String os) {
            this.os = os;
        }

        public final String getOsVersion() {
            return osVersion;
        }

        @Override
        public final Builder osVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }

        public final void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public final String getParentProduct() {
            return parentProduct;
        }

        @Override
        public final Builder parentProduct(String parentProduct) {
            this.parentProduct = parentProduct;
            return this;
        }

        public final void setParentProduct(String parentProduct) {
            this.parentProduct = parentProduct;
        }

        public final String getParentProductVersion() {
            return parentProductVersion;
        }

        @Override
        public final Builder parentProductVersion(String parentProductVersion) {
            this.parentProductVersion = parentProductVersion;
            return this;
        }

        public final void setParentProductVersion(String parentProductVersion) {
            this.parentProductVersion = parentProductVersion;
        }

        public final Collection<MetricDatum.Builder> getMetricData() {
            return metricData != null ? metricData.stream().map(MetricDatum::toBuilder).collect(Collectors.toList()) : null;
        }

        @Override
        public final Builder metricData(Collection<MetricDatum> metricData) {
            this.metricData = MetricDataCopier.copy(metricData);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder metricData(MetricDatum... metricData) {
            metricData(Arrays.asList(metricData));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder metricData(Consumer<MetricDatum.Builder>... metricData) {
            metricData(Stream.of(metricData).map(c -> MetricDatum.builder().applyMutation(c).build())
                    .collect(Collectors.toList()));
            return this;
        }

        public final void setMetricData(Collection<MetricDatum.BuilderImpl> metricData) {
            this.metricData = MetricDataCopier.copyFromBuilder(metricData);
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public PostMetricsRequest build() {
            return new PostMetricsRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
