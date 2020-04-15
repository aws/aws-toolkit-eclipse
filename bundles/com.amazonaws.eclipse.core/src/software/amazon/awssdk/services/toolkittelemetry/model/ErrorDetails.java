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
public final class ErrorDetails implements SdkPojo, Serializable, ToCopyableBuilder<ErrorDetails.Builder, ErrorDetails> {
    private static final SdkField<String> COMMAND_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(ErrorDetails::command)).setter(setter(Builder::command))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Command").build()).build();

    private static final SdkField<Long> EPOCH_TIMESTAMP_FIELD = SdkField.<Long> builder(MarshallingType.LONG)
            .getter(getter(ErrorDetails::epochTimestamp)).setter(setter(Builder::epochTimestamp))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("EpochTimestamp").build()).build();

    private static final SdkField<String> TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(ErrorDetails::type)).setter(setter(Builder::type))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Type").build()).build();

    private static final SdkField<String> MESSAGE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(ErrorDetails::message)).setter(setter(Builder::message))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Message").build()).build();

    private static final SdkField<String> STACK_TRACE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .getter(getter(ErrorDetails::stackTrace)).setter(setter(Builder::stackTrace))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StackTrace").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(COMMAND_FIELD,
            EPOCH_TIMESTAMP_FIELD, TYPE_FIELD, MESSAGE_FIELD, STACK_TRACE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String command;

    private final Long epochTimestamp;

    private final String type;

    private final String message;

    private final String stackTrace;

    private ErrorDetails(BuilderImpl builder) {
        this.command = builder.command;
        this.epochTimestamp = builder.epochTimestamp;
        this.type = builder.type;
        this.message = builder.message;
        this.stackTrace = builder.stackTrace;
    }

    /**
     * Returns the value of the Command property for this object.
     * 
     * @return The value of the Command property for this object.
     */
    public String command() {
        return command;
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
     * Returns the value of the Type property for this object.
     * 
     * @return The value of the Type property for this object.
     */
    public String type() {
        return type;
    }

    /**
     * Returns the value of the Message property for this object.
     * 
     * @return The value of the Message property for this object.
     */
    public String message() {
        return message;
    }

    /**
     * Returns the value of the StackTrace property for this object.
     * 
     * @return The value of the StackTrace property for this object.
     */
    public String stackTrace() {
        return stackTrace;
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
        hashCode = 31 * hashCode + Objects.hashCode(command());
        hashCode = 31 * hashCode + Objects.hashCode(epochTimestamp());
        hashCode = 31 * hashCode + Objects.hashCode(type());
        hashCode = 31 * hashCode + Objects.hashCode(message());
        hashCode = 31 * hashCode + Objects.hashCode(stackTrace());
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
        if (!(obj instanceof ErrorDetails)) {
            return false;
        }
        ErrorDetails other = (ErrorDetails) obj;
        return Objects.equals(command(), other.command()) && Objects.equals(epochTimestamp(), other.epochTimestamp())
                && Objects.equals(type(), other.type()) && Objects.equals(message(), other.message())
                && Objects.equals(stackTrace(), other.stackTrace());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("ErrorDetails").add("Command", command()).add("EpochTimestamp", epochTimestamp())
                .add("Type", type()).add("Message", message()).add("StackTrace", stackTrace()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Command":
            return Optional.ofNullable(clazz.cast(command()));
        case "EpochTimestamp":
            return Optional.ofNullable(clazz.cast(epochTimestamp()));
        case "Type":
            return Optional.ofNullable(clazz.cast(type()));
        case "Message":
            return Optional.ofNullable(clazz.cast(message()));
        case "StackTrace":
            return Optional.ofNullable(clazz.cast(stackTrace()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ErrorDetails, T> g) {
        return obj -> g.apply((ErrorDetails) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ErrorDetails> {
        /**
         * Sets the value of the Command property for this object.
         *
         * @param command
         *        The new value for the Command property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder command(String command);

        /**
         * Sets the value of the EpochTimestamp property for this object.
         *
         * @param epochTimestamp
         *        The new value for the EpochTimestamp property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder epochTimestamp(Long epochTimestamp);

        /**
         * Sets the value of the Type property for this object.
         *
         * @param type
         *        The new value for the Type property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder type(String type);

        /**
         * Sets the value of the Message property for this object.
         *
         * @param message
         *        The new value for the Message property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder message(String message);

        /**
         * Sets the value of the StackTrace property for this object.
         *
         * @param stackTrace
         *        The new value for the StackTrace property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stackTrace(String stackTrace);
    }

    static final class BuilderImpl implements Builder {
        private String command;

        private Long epochTimestamp;

        private String type;

        private String message;

        private String stackTrace;

        private BuilderImpl() {
        }

        private BuilderImpl(ErrorDetails model) {
            command(model.command);
            epochTimestamp(model.epochTimestamp);
            type(model.type);
            message(model.message);
            stackTrace(model.stackTrace);
        }

        public final String getCommand() {
            return command;
        }

        @Override
        public final Builder command(String command) {
            this.command = command;
            return this;
        }

        public final void setCommand(String command) {
            this.command = command;
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

        public final String getType() {
            return type;
        }

        @Override
        public final Builder type(String type) {
            this.type = type;
            return this;
        }

        public final void setType(String type) {
            this.type = type;
        }

        public final String getMessage() {
            return message;
        }

        @Override
        public final Builder message(String message) {
            this.message = message;
            return this;
        }

        public final void setMessage(String message) {
            this.message = message;
        }

        public final String getStackTrace() {
            return stackTrace;
        }

        @Override
        public final Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public final void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        @Override
        public ErrorDetails build() {
            return new ErrorDetails(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
