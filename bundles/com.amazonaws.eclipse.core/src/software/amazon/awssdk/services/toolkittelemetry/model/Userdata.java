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
public final class Userdata implements SdkPojo, Serializable, ToCopyableBuilder<Userdata.Builder, Userdata> {
    private static final SdkField<String> EMAIL_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(Userdata::email)).setter(setter(Builder::email))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Email").build()).build();

    private static final SdkField<String> COMMENT_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(Userdata::comment)).setter(setter(Builder::comment))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Comment").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(EMAIL_FIELD, COMMENT_FIELD));

    private static final long serialVersionUID = 1L;

    private final String email;

    private final String comment;

    private Userdata(BuilderImpl builder) {
        this.email = builder.email;
        this.comment = builder.comment;
    }

    /**
     * Returns the value of the Email property for this object.
     * 
     * @return The value of the Email property for this object.
     */
    public String email() {
        return email;
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
        hashCode = 31 * hashCode + Objects.hashCode(email());
        hashCode = 31 * hashCode + Objects.hashCode(comment());
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
        if (!(obj instanceof Userdata)) {
            return false;
        }
        Userdata other = (Userdata) obj;
        return Objects.equals(email(), other.email()) && Objects.equals(comment(), other.comment());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("Userdata").add("Email", email()).add("Comment", comment()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Email":
            return Optional.ofNullable(clazz.cast(email()));
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

    private static <T> Function<Object, T> getter(Function<Userdata, T> g) {
        return obj -> g.apply((Userdata) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, Userdata> {
        /**
         * Sets the value of the Email property for this object.
         *
         * @param email
         *        The new value for the Email property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder email(String email);

        /**
         * Sets the value of the Comment property for this object.
         *
         * @param comment
         *        The new value for the Comment property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder comment(String comment);
    }

    static final class BuilderImpl implements Builder {
        private String email;

        private String comment;

        private BuilderImpl() {
        }

        private BuilderImpl(Userdata model) {
            email(model.email);
            comment(model.comment);
        }

        public final String getEmail() {
            return email;
        }

        @Override
        public final Builder email(String email) {
            this.email = email;
            return this;
        }

        public final void setEmail(String email) {
            this.email = email;
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
        public Userdata build() {
            return new Userdata(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
