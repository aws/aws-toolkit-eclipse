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
public final class PostFeedbackRequest extends ToolkitTelemetryRequest implements
        ToCopyableBuilder<PostFeedbackRequest.Builder, PostFeedbackRequest> {
    private static final SdkField<String> AWS_PRODUCT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::awsProductAsString)).setter(setter(Builder::awsProduct))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProduct").build()).build();

    private static final SdkField<String> AWS_PRODUCT_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::awsProductVersion)).setter(setter(Builder::awsProductVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AWSProductVersion").build()).build();

    private static final SdkField<String> OS_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::os)).setter(setter(Builder::os))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OS").build()).build();

    private static final SdkField<String> OS_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::osVersion)).setter(setter(Builder::osVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OSVersion").build()).build();

    private static final SdkField<String> PARENT_PRODUCT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::parentProduct)).setter(setter(Builder::parentProduct))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ParentProduct").build()).build();

    private static final SdkField<String> PARENT_PRODUCT_VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::parentProductVersion)).setter(setter(Builder::parentProductVersion))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ParentProductVersion").build())
            .build();

    private static final SdkField<List<MetadataEntry>> METADATA_FIELD = SdkField
            .<List<MetadataEntry>> builder(MarshallingType.LIST)
            .getter(getter(PostFeedbackRequest::metadata))
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

    private static final SdkField<String> SENTIMENT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::sentimentAsString)).setter(setter(Builder::sentiment))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Sentiment").build()).build();

    private static final SdkField<String> COMMENT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(PostFeedbackRequest::comment)).setter(setter(Builder::comment))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Comment").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(AWS_PRODUCT_FIELD,
            AWS_PRODUCT_VERSION_FIELD, OS_FIELD, OS_VERSION_FIELD, PARENT_PRODUCT_FIELD, PARENT_PRODUCT_VERSION_FIELD,
            METADATA_FIELD, SENTIMENT_FIELD, COMMENT_FIELD));

    private final String awsProduct;

    private final String awsProductVersion;

    private final String os;

    private final String osVersion;

    private final String parentProduct;

    private final String parentProductVersion;

    private final List<MetadataEntry> metadata;

    private final String sentiment;

    private final String comment;

    private PostFeedbackRequest(BuilderImpl builder) {
        super(builder);
        this.awsProduct = builder.awsProduct;
        this.awsProductVersion = builder.awsProductVersion;
        this.os = builder.os;
        this.osVersion = builder.osVersion;
        this.parentProduct = builder.parentProduct;
        this.parentProductVersion = builder.parentProductVersion;
        this.metadata = builder.metadata;
        this.sentiment = builder.sentiment;
        this.comment = builder.comment;
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
     * Returns the value of the Sentiment property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sentiment} will
     * return {@link Sentiment#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sentimentAsString}.
     * </p>
     * 
     * @return The value of the Sentiment property for this object.
     * @see Sentiment
     */
    public Sentiment sentiment() {
        return Sentiment.fromValue(sentiment);
    }

    /**
     * Returns the value of the Sentiment property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sentiment} will
     * return {@link Sentiment#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sentimentAsString}.
     * </p>
     * 
     * @return The value of the Sentiment property for this object.
     * @see Sentiment
     */
    public String sentimentAsString() {
        return sentiment;
    }

    /**
     * Returns the value of the Comment property for this object.
     * 
     * @return The value of the Comment property for this object.
     */
    public String comment() {
        return comment;
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
        hashCode = 31 * hashCode + Objects.hashCode(os());
        hashCode = 31 * hashCode + Objects.hashCode(osVersion());
        hashCode = 31 * hashCode + Objects.hashCode(parentProduct());
        hashCode = 31 * hashCode + Objects.hashCode(parentProductVersion());
        hashCode = 31 * hashCode + Objects.hashCode(metadata());
        hashCode = 31 * hashCode + Objects.hashCode(sentimentAsString());
        hashCode = 31 * hashCode + Objects.hashCode(comment());
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
        if (!(obj instanceof PostFeedbackRequest)) {
            return false;
        }
        PostFeedbackRequest other = (PostFeedbackRequest) obj;
        return Objects.equals(awsProductAsString(), other.awsProductAsString())
                && Objects.equals(awsProductVersion(), other.awsProductVersion()) && Objects.equals(os(), other.os())
                && Objects.equals(osVersion(), other.osVersion()) && Objects.equals(parentProduct(), other.parentProduct())
                && Objects.equals(parentProductVersion(), other.parentProductVersion())
                && Objects.equals(metadata(), other.metadata()) && Objects.equals(sentimentAsString(), other.sentimentAsString())
                && Objects.equals(comment(), other.comment());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("PostFeedbackRequest").add("AWSProduct", awsProductAsString())
                .add("AWSProductVersion", awsProductVersion()).add("OS", os()).add("OSVersion", osVersion())
                .add("ParentProduct", parentProduct()).add("ParentProductVersion", parentProductVersion())
                .add("Metadata", metadata()).add("Sentiment", sentimentAsString()).add("Comment", comment()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "AWSProduct":
            return Optional.ofNullable(clazz.cast(awsProductAsString()));
        case "AWSProductVersion":
            return Optional.ofNullable(clazz.cast(awsProductVersion()));
        case "OS":
            return Optional.ofNullable(clazz.cast(os()));
        case "OSVersion":
            return Optional.ofNullable(clazz.cast(osVersion()));
        case "ParentProduct":
            return Optional.ofNullable(clazz.cast(parentProduct()));
        case "ParentProductVersion":
            return Optional.ofNullable(clazz.cast(parentProductVersion()));
        case "Metadata":
            return Optional.ofNullable(clazz.cast(metadata()));
        case "Sentiment":
            return Optional.ofNullable(clazz.cast(sentimentAsString()));
        case "Comment":
            return Optional.ofNullable(clazz.cast(comment()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<PostFeedbackRequest, T> g) {
        return obj -> g.apply((PostFeedbackRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends ToolkitTelemetryRequest.Builder, SdkPojo, CopyableBuilder<Builder, PostFeedbackRequest> {
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
         * Sets the value of the Sentiment property for this object.
         *
         * @param sentiment
         *        The new value for the Sentiment property for this object.
         * @see Sentiment
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see Sentiment
         */
        Builder sentiment(String sentiment);

        /**
         * Sets the value of the Sentiment property for this object.
         *
         * @param sentiment
         *        The new value for the Sentiment property for this object.
         * @see Sentiment
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see Sentiment
         */
        Builder sentiment(Sentiment sentiment);

        /**
         * Sets the value of the Comment property for this object.
         *
         * @param comment
         *        The new value for the Comment property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder comment(String comment);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends ToolkitTelemetryRequest.BuilderImpl implements Builder {
        private String awsProduct;

        private String awsProductVersion;

        private String os;

        private String osVersion;

        private String parentProduct;

        private String parentProductVersion;

        private List<MetadataEntry> metadata = DefaultSdkAutoConstructList.getInstance();

        private String sentiment;

        private String comment;

        private BuilderImpl() {
        }

        private BuilderImpl(PostFeedbackRequest model) {
            super(model);
            awsProduct(model.awsProduct);
            awsProductVersion(model.awsProductVersion);
            os(model.os);
            osVersion(model.osVersion);
            parentProduct(model.parentProduct);
            parentProductVersion(model.parentProductVersion);
            metadata(model.metadata);
            sentiment(model.sentiment);
            comment(model.comment);
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

        public final String getSentimentAsString() {
            return sentiment;
        }

        @Override
        public final Builder sentiment(String sentiment) {
            this.sentiment = sentiment;
            return this;
        }

        @Override
        public final Builder sentiment(Sentiment sentiment) {
            this.sentiment(sentiment == null ? null : sentiment.toString());
            return this;
        }

        public final void setSentiment(String sentiment) {
            this.sentiment = sentiment;
        }

        public final String getComment() {
            return comment;
        }

        @Override
        public final Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public final void setComment(String comment) {
            this.comment = comment;
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
        public PostFeedbackRequest build() {
            return new PostFeedbackRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
