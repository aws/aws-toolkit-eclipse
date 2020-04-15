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
public final class PostErrorReportRequest extends ToolkitTelemetryRequest implements
        ToCopyableBuilder<PostErrorReportRequest.Builder, PostErrorReportRequest> {
    private static final SdkField<String> AWS_PRODUCT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostErrorReportRequest::awsProductAsString)).setter(setter(Builder::awsProduct))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProduct").build()).build();

    private static final SdkField<String> AWS_PRODUCT_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostErrorReportRequest::awsProductVersion)).setter(setter(Builder::awsProductVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProductVersion").build()).build();

    private static final SdkField<List<MetadataEntry>> METADATA_FIELD = SdkField
            .<List<MetadataEntry>> builder(MarshallingType.LIST)
            .getter(getter(PostErrorReportRequest::metadata))
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

    private static final SdkField<Userdata> USERDATA_FIELD = SdkField.<Userdata> builder(MarshallingType.SDK_POJO)
            .getter(getter(PostErrorReportRequest::userdata)).setter(setter(Builder::userdata)).constructor(Userdata::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Userdata").build()).build();

    private static final SdkField<ErrorDetails> ERROR_DETAILS_FIELD = SdkField.<ErrorDetails> builder(MarshallingType.SDK_POJO)
            .getter(getter(PostErrorReportRequest::errorDetails)).setter(setter(Builder::errorDetails))
            .constructor(ErrorDetails::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ErrorDetails").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(AWS_PRODUCT_FIELD,
            AWS_PRODUCT_VERSION_FIELD, METADATA_FIELD, USERDATA_FIELD, ERROR_DETAILS_FIELD));

    private final String awsProduct;

    private final String awsProductVersion;

    private final List<MetadataEntry> metadata;

    private final Userdata userdata;

    private final ErrorDetails errorDetails;

    private PostErrorReportRequest(BuilderImpl builder) {
        super(builder);
        this.awsProduct = builder.awsProduct;
        this.awsProductVersion = builder.awsProductVersion;
        this.metadata = builder.metadata;
        this.userdata = builder.userdata;
        this.errorDetails = builder.errorDetails;
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

    /**
     * Returns the value of the Userdata property for this object.
     * 
     * @return The value of the Userdata property for this object.
     */
    public Userdata userdata() {
        return userdata;
    }

    /**
     * Returns the value of the ErrorDetails property for this object.
     * 
     * @return The value of the ErrorDetails property for this object.
     */
    public ErrorDetails errorDetails() {
        return errorDetails;
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
        hashCode = 31 * hashCode + Objects.hashCode(metadata());
        hashCode = 31 * hashCode + Objects.hashCode(userdata());
        hashCode = 31 * hashCode + Objects.hashCode(errorDetails());
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
        if (!(obj instanceof PostErrorReportRequest)) {
            return false;
        }
        PostErrorReportRequest other = (PostErrorReportRequest) obj;
        return Objects.equals(awsProductAsString(), other.awsProductAsString())
                && Objects.equals(awsProductVersion(), other.awsProductVersion()) && Objects.equals(metadata(), other.metadata())
                && Objects.equals(userdata(), other.userdata()) && Objects.equals(errorDetails(), other.errorDetails());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("PostErrorReportRequest").add("AWSProduct", awsProductAsString())
                .add("AWSProductVersion", awsProductVersion()).add("Metadata", metadata()).add("Userdata", userdata())
                .add("ErrorDetails", errorDetails()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "AWSProduct":
            return Optional.ofNullable(clazz.cast(awsProductAsString()));
        case "AWSProductVersion":
            return Optional.ofNullable(clazz.cast(awsProductVersion()));
        case "Metadata":
            return Optional.ofNullable(clazz.cast(metadata()));
        case "Userdata":
            return Optional.ofNullable(clazz.cast(userdata()));
        case "ErrorDetails":
            return Optional.ofNullable(clazz.cast(errorDetails()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<PostErrorReportRequest, T> g) {
        return obj -> g.apply((PostErrorReportRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends ToolkitTelemetryRequest.Builder, SdkPojo, CopyableBuilder<Builder, PostErrorReportRequest> {
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

        /**
         * Sets the value of the Userdata property for this object.
         *
         * @param userdata
         *        The new value for the Userdata property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userdata(Userdata userdata);

        /**
         * Sets the value of the Userdata property for this object.
         *
         * This is a convenience that creates an instance of the {@link Userdata.Builder} avoiding the need to create
         * one manually via {@link Userdata#builder()}.
         *
         * When the {@link Consumer} completes, {@link Userdata.Builder#build()} is called immediately and its result is
         * passed to {@link #userdata(Userdata)}.
         * 
         * @param userdata
         *        a consumer that will call methods on {@link Userdata.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #userdata(Userdata)
         */
        default Builder userdata(Consumer<Userdata.Builder> userdata) {
            return userdata(Userdata.builder().applyMutation(userdata).build());
        }

        /**
         * Sets the value of the ErrorDetails property for this object.
         *
         * @param errorDetails
         *        The new value for the ErrorDetails property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder errorDetails(ErrorDetails errorDetails);

        /**
         * Sets the value of the ErrorDetails property for this object.
         *
         * This is a convenience that creates an instance of the {@link ErrorDetails.Builder} avoiding the need to
         * create one manually via {@link ErrorDetails#builder()}.
         *
         * When the {@link Consumer} completes, {@link ErrorDetails.Builder#build()} is called immediately and its
         * result is passed to {@link #errorDetails(ErrorDetails)}.
         * 
         * @param errorDetails
         *        a consumer that will call methods on {@link ErrorDetails.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #errorDetails(ErrorDetails)
         */
        default Builder errorDetails(Consumer<ErrorDetails.Builder> errorDetails) {
            return errorDetails(ErrorDetails.builder().applyMutation(errorDetails).build());
        }

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends ToolkitTelemetryRequest.BuilderImpl implements Builder {
        private String awsProduct;

        private String awsProductVersion;

        private List<MetadataEntry> metadata = DefaultSdkAutoConstructList.getInstance();

        private Userdata userdata;

        private ErrorDetails errorDetails;

        private BuilderImpl() {
        }

        private BuilderImpl(PostErrorReportRequest model) {
            super(model);
            awsProduct(model.awsProduct);
            awsProductVersion(model.awsProductVersion);
            metadata(model.metadata);
            userdata(model.userdata);
            errorDetails(model.errorDetails);
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

        public final Userdata.Builder getUserdata() {
            return userdata != null ? userdata.toBuilder() : null;
        }

        @Override
        public final Builder userdata(Userdata userdata) {
            this.userdata = userdata;
            return this;
        }

        public final void setUserdata(Userdata.BuilderImpl userdata) {
            this.userdata = userdata != null ? userdata.build() : null;
        }

        public final ErrorDetails.Builder getErrorDetails() {
            return errorDetails != null ? errorDetails.toBuilder() : null;
        }

        @Override
        public final Builder errorDetails(ErrorDetails errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public final void setErrorDetails(ErrorDetails.BuilderImpl errorDetails) {
            this.errorDetails = errorDetails != null ? errorDetails.build() : null;
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
        public PostErrorReportRequest build() {
            return new PostErrorReportRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
