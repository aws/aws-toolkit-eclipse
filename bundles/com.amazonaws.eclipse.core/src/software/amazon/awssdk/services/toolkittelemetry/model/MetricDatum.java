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
public class MetricDatum implements Serializable, Cloneable, StructuredPojo {

    private String metricName;

    private Long epochTimestamp;

    private String unit;

    private Double value;

    private java.util.List<MetadataEntry> metadata;

    /**
     * @param metricName
     */

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * @return
     */

    public String getMetricName() {
        return this.metricName;
    }

    /**
     * @param metricName
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MetricDatum metricName(String metricName) {
        setMetricName(metricName);
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

    public MetricDatum epochTimestamp(Long epochTimestamp) {
        setEpochTimestamp(epochTimestamp);
        return this;
    }

    /**
     * @param unit
     * @see Unit
     */

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return
     * @see Unit
     */

    public String getUnit() {
        return this.unit;
    }

    /**
     * @param unit
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Unit
     */

    public MetricDatum unit(String unit) {
        setUnit(unit);
        return this;
    }

    /**
     * @param unit
     * @see Unit
     */

    public void setUnit(Unit unit) {
        unit(unit);
    }

    /**
     * @param unit
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Unit
     */

    public MetricDatum unit(Unit unit) {
        this.unit = unit.toString();
        return this;
    }

    /**
     * @param value
     */

    public void setValue(Double value) {
        this.value = value;
    }

    /**
     * @return
     */

    public Double getValue() {
        return this.value;
    }

    /**
     * @param value
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MetricDatum value(Double value) {
        setValue(value);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<MetadataEntry> getMetadata() {
        return metadata;
    }

    /**
     * @param metadata
     */

    public void setMetadata(java.util.Collection<MetadataEntry> metadata) {
        if (metadata == null) {
            this.metadata = null;
            return;
        }

        this.metadata = new java.util.ArrayList<MetadataEntry>(metadata);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setMetadata(java.util.Collection)} or {@link #withMetadata(java.util.Collection)} if you want to override
     * the existing values.
     * </p>
     * 
     * @param metadata
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MetricDatum metadata(MetadataEntry... metadata) {
        if (this.metadata == null) {
            setMetadata(new java.util.ArrayList<MetadataEntry>(metadata.length));
        }
        for (MetadataEntry ele : metadata) {
            this.metadata.add(ele);
        }
        return this;
    }

    /**
     * @param metadata
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public MetricDatum metadata(java.util.Collection<MetadataEntry> metadata) {
        setMetadata(metadata);
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
        if (getMetricName() != null)
            sb.append("MetricName: ").append(getMetricName()).append(",");
        if (getEpochTimestamp() != null)
            sb.append("EpochTimestamp: ").append(getEpochTimestamp()).append(",");
        if (getUnit() != null)
            sb.append("Unit: ").append(getUnit()).append(",");
        if (getValue() != null)
            sb.append("Value: ").append(getValue()).append(",");
        if (getMetadata() != null)
            sb.append("Metadata: ").append(getMetadata());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof MetricDatum == false)
            return false;
        MetricDatum other = (MetricDatum) obj;
        if (other.getMetricName() == null ^ this.getMetricName() == null)
            return false;
        if (other.getMetricName() != null && other.getMetricName().equals(this.getMetricName()) == false)
            return false;
        if (other.getEpochTimestamp() == null ^ this.getEpochTimestamp() == null)
            return false;
        if (other.getEpochTimestamp() != null && other.getEpochTimestamp().equals(this.getEpochTimestamp()) == false)
            return false;
        if (other.getUnit() == null ^ this.getUnit() == null)
            return false;
        if (other.getUnit() != null && other.getUnit().equals(this.getUnit()) == false)
            return false;
        if (other.getValue() == null ^ this.getValue() == null)
            return false;
        if (other.getValue() != null && other.getValue().equals(this.getValue()) == false)
            return false;
        if (other.getMetadata() == null ^ this.getMetadata() == null)
            return false;
        if (other.getMetadata() != null && other.getMetadata().equals(this.getMetadata()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getMetricName() == null) ? 0 : getMetricName().hashCode());
        hashCode = prime * hashCode + ((getEpochTimestamp() == null) ? 0 : getEpochTimestamp().hashCode());
        hashCode = prime * hashCode + ((getUnit() == null) ? 0 : getUnit().hashCode());
        hashCode = prime * hashCode + ((getValue() == null) ? 0 : getValue().hashCode());
        hashCode = prime * hashCode + ((getMetadata() == null) ? 0 : getMetadata().hashCode());
        return hashCode;
    }

    @Override
    public MetricDatum clone() {
        try {
            return (MetricDatum) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    @com.amazonaws.annotation.SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        software.amazon.awssdk.services.toolkittelemetry.model.transform.MetricDatumMarshaller.getInstance().marshall(this, protocolMarshaller);
    }
}
