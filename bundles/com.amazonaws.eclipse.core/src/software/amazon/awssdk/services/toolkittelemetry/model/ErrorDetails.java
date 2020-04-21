/*
 * Copyright 2015-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import javax.annotation.Generated;
import com.amazonaws.protocol.StructuredPojo;
import com.amazonaws.protocol.ProtocolMarshaller;

@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class ErrorDetails implements Serializable, Cloneable, StructuredPojo {

    private String command;

    private Long epochTimestamp;

    private String type;

    private String message;

    private String stackTrace;

    /**
     * @param command
     */

    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return
     */

    public String getCommand() {
        return this.command;
    }

    /**
     * @param command
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ErrorDetails command(String command) {
        setCommand(command);
        return this;
    }

    /**
     * @param epochTimestamp
     */

    public void setEpochTimestamp(Long epochTimestamp) {
        this.epochTimestamp = epochTimestamp;
    }

    /**
     * @return
     */

    public Long getEpochTimestamp() {
        return this.epochTimestamp;
    }

    /**
     * @param epochTimestamp
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ErrorDetails epochTimestamp(Long epochTimestamp) {
        setEpochTimestamp(epochTimestamp);
        return this;
    }

    /**
     * @param type
     */

    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return
     */

    public String getType() {
        return this.type;
    }

    /**
     * @param type
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ErrorDetails type(String type) {
        setType(type);
        return this;
    }

    /**
     * @param message
     */

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return
     */

    public String getMessage() {
        return this.message;
    }

    /**
     * @param message
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ErrorDetails message(String message) {
        setMessage(message);
        return this;
    }

    /**
     * @param stackTrace
     */

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * @return
     */

    public String getStackTrace() {
        return this.stackTrace;
    }

    /**
     * @param stackTrace
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public ErrorDetails stackTrace(String stackTrace) {
        setStackTrace(stackTrace);
        return this;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getCommand() != null)
            sb.append("Command: ").append(getCommand()).append(",");
        if (getEpochTimestamp() != null)
            sb.append("EpochTimestamp: ").append(getEpochTimestamp()).append(",");
        if (getType() != null)
            sb.append("Type: ").append(getType()).append(",");
        if (getMessage() != null)
            sb.append("Message: ").append(getMessage()).append(",");
        if (getStackTrace() != null)
            sb.append("StackTrace: ").append(getStackTrace());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof ErrorDetails == false)
            return false;
        ErrorDetails other = (ErrorDetails) obj;
        if (other.getCommand() == null ^ this.getCommand() == null)
            return false;
        if (other.getCommand() != null && other.getCommand().equals(this.getCommand()) == false)
            return false;
        if (other.getEpochTimestamp() == null ^ this.getEpochTimestamp() == null)
            return false;
        if (other.getEpochTimestamp() != null && other.getEpochTimestamp().equals(this.getEpochTimestamp()) == false)
            return false;
        if (other.getType() == null ^ this.getType() == null)
            return false;
        if (other.getType() != null && other.getType().equals(this.getType()) == false)
            return false;
        if (other.getMessage() == null ^ this.getMessage() == null)
            return false;
        if (other.getMessage() != null && other.getMessage().equals(this.getMessage()) == false)
            return false;
        if (other.getStackTrace() == null ^ this.getStackTrace() == null)
            return false;
        if (other.getStackTrace() != null && other.getStackTrace().equals(this.getStackTrace()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getCommand() == null) ? 0 : getCommand().hashCode());
        hashCode = prime * hashCode + ((getEpochTimestamp() == null) ? 0 : getEpochTimestamp().hashCode());
        hashCode = prime * hashCode + ((getType() == null) ? 0 : getType().hashCode());
        hashCode = prime * hashCode + ((getMessage() == null) ? 0 : getMessage().hashCode());
        hashCode = prime * hashCode + ((getStackTrace() == null) ? 0 : getStackTrace().hashCode());
        return hashCode;
    }

    @Override
    public ErrorDetails clone() {
        try {
            return (ErrorDetails) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        software.amazon.awssdk.services.toolkittelemetry.model.transform.ErrorDetailsMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
