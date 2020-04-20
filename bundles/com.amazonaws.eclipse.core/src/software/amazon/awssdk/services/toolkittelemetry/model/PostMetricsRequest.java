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

import com.amazonaws.auth.RequestSigner;
import com.amazonaws.opensdk.protect.auth.RequestSignerAware;

@Generated("com.amazonaws:aws-java-sdk-code-generator")
public class PostMetricsRequest extends com.amazonaws.opensdk.BaseRequest implements Serializable, Cloneable, RequestSignerAware {

    private String aWSProduct;

    private String aWSProductVersion;

    private String clientID;

    private String oS;

    private String oSVersion;

    private String parentProduct;

    private String parentProductVersion;

    private java.util.List<MetricDatum> metricData;

    /**
     * @param aWSProduct
     * @see AWSProduct
     */

    public void setAWSProduct(String aWSProduct) {
        this.aWSProduct = aWSProduct;
    }

    /**
     * @return
     * @see AWSProduct
     */

    public String getAWSProduct() {
        return this.aWSProduct;
    }

    /**
     * @param aWSProduct
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see AWSProduct
     */

    public PostMetricsRequest aWSProduct(String aWSProduct) {
        setAWSProduct(aWSProduct);
        return this;
    }

    /**
     * @param aWSProduct
     * @see AWSProduct
     */

    public void setAWSProduct(AWSProduct aWSProduct) {
        aWSProduct(aWSProduct);
    }

    /**
     * @param aWSProduct
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see AWSProduct
     */

    public PostMetricsRequest aWSProduct(AWSProduct aWSProduct) {
        this.aWSProduct = aWSProduct.toString();
        return this;
    }

    /**
     * @param aWSProductVersion
     */

    public void setAWSProductVersion(String aWSProductVersion) {
        this.aWSProductVersion = aWSProductVersion;
    }

    /**
     * @return
     */

    public String getAWSProductVersion() {
        return this.aWSProductVersion;
    }

    /**
     * @param aWSProductVersion
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest aWSProductVersion(String aWSProductVersion) {
        setAWSProductVersion(aWSProductVersion);
        return this;
    }

    /**
     * @param clientID
     */

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    /**
     * @return
     */

    public String getClientID() {
        return this.clientID;
    }

    /**
     * @param clientID
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest clientID(String clientID) {
        setClientID(clientID);
        return this;
    }

    /**
     * @param oS
     */

    public void setOS(String oS) {
        this.oS = oS;
    }

    /**
     * @return
     */

    public String getOS() {
        return this.oS;
    }

    /**
     * @param oS
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest oS(String oS) {
        setOS(oS);
        return this;
    }

    /**
     * @param oSVersion
     */

    public void setOSVersion(String oSVersion) {
        this.oSVersion = oSVersion;
    }

    /**
     * @return
     */

    public String getOSVersion() {
        return this.oSVersion;
    }

    /**
     * @param oSVersion
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest oSVersion(String oSVersion) {
        setOSVersion(oSVersion);
        return this;
    }

    /**
     * @param parentProduct
     */

    public void setParentProduct(String parentProduct) {
        this.parentProduct = parentProduct;
    }

    /**
     * @return
     */

    public String getParentProduct() {
        return this.parentProduct;
    }

    /**
     * @param parentProduct
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest parentProduct(String parentProduct) {
        setParentProduct(parentProduct);
        return this;
    }

    /**
     * @param parentProductVersion
     */

    public void setParentProductVersion(String parentProductVersion) {
        this.parentProductVersion = parentProductVersion;
    }

    /**
     * @return
     */

    public String getParentProductVersion() {
        return this.parentProductVersion;
    }

    /**
     * @param parentProductVersion
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest parentProductVersion(String parentProductVersion) {
        setParentProductVersion(parentProductVersion);
        return this;
    }

    /**
     * @return
     */

    public java.util.List<MetricDatum> getMetricData() {
        return metricData;
    }

    /**
     * @param metricData
     */

    public void setMetricData(java.util.Collection<MetricDatum> metricData) {
        if (metricData == null) {
            this.metricData = null;
            return;
        }

        this.metricData = new java.util.ArrayList<MetricDatum>(metricData);
    }

    /**
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setMetricData(java.util.Collection)} or {@link #withMetricData(java.util.Collection)} if you want to
     * override the existing values.
     * </p>
     * 
     * @param metricData
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest metricData(MetricDatum... metricData) {
        if (this.metricData == null) {
            setMetricData(new java.util.ArrayList<MetricDatum>(metricData.length));
        }
        for (MetricDatum ele : metricData) {
            this.metricData.add(ele);
        }
        return this;
    }

    /**
     * @param metricData
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostMetricsRequest metricData(java.util.Collection<MetricDatum> metricData) {
        setMetricData(metricData);
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
        if (getAWSProduct() != null)
            sb.append("AWSProduct: ").append(getAWSProduct()).append(",");
        if (getAWSProductVersion() != null)
            sb.append("AWSProductVersion: ").append(getAWSProductVersion()).append(",");
        if (getClientID() != null)
            sb.append("ClientID: ").append(getClientID()).append(",");
        if (getOS() != null)
            sb.append("OS: ").append(getOS()).append(",");
        if (getOSVersion() != null)
            sb.append("OSVersion: ").append(getOSVersion()).append(",");
        if (getParentProduct() != null)
            sb.append("ParentProduct: ").append(getParentProduct()).append(",");
        if (getParentProductVersion() != null)
            sb.append("ParentProductVersion: ").append(getParentProductVersion()).append(",");
        if (getMetricData() != null)
            sb.append("MetricData: ").append(getMetricData());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof PostMetricsRequest == false)
            return false;
        PostMetricsRequest other = (PostMetricsRequest) obj;
        if (other.getAWSProduct() == null ^ this.getAWSProduct() == null)
            return false;
        if (other.getAWSProduct() != null && other.getAWSProduct().equals(this.getAWSProduct()) == false)
            return false;
        if (other.getAWSProductVersion() == null ^ this.getAWSProductVersion() == null)
            return false;
        if (other.getAWSProductVersion() != null && other.getAWSProductVersion().equals(this.getAWSProductVersion()) == false)
            return false;
        if (other.getClientID() == null ^ this.getClientID() == null)
            return false;
        if (other.getClientID() != null && other.getClientID().equals(this.getClientID()) == false)
            return false;
        if (other.getOS() == null ^ this.getOS() == null)
            return false;
        if (other.getOS() != null && other.getOS().equals(this.getOS()) == false)
            return false;
        if (other.getOSVersion() == null ^ this.getOSVersion() == null)
            return false;
        if (other.getOSVersion() != null && other.getOSVersion().equals(this.getOSVersion()) == false)
            return false;
        if (other.getParentProduct() == null ^ this.getParentProduct() == null)
            return false;
        if (other.getParentProduct() != null && other.getParentProduct().equals(this.getParentProduct()) == false)
            return false;
        if (other.getParentProductVersion() == null ^ this.getParentProductVersion() == null)
            return false;
        if (other.getParentProductVersion() != null && other.getParentProductVersion().equals(this.getParentProductVersion()) == false)
            return false;
        if (other.getMetricData() == null ^ this.getMetricData() == null)
            return false;
        if (other.getMetricData() != null && other.getMetricData().equals(this.getMetricData()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getAWSProduct() == null) ? 0 : getAWSProduct().hashCode());
        hashCode = prime * hashCode + ((getAWSProductVersion() == null) ? 0 : getAWSProductVersion().hashCode());
        hashCode = prime * hashCode + ((getClientID() == null) ? 0 : getClientID().hashCode());
        hashCode = prime * hashCode + ((getOS() == null) ? 0 : getOS().hashCode());
        hashCode = prime * hashCode + ((getOSVersion() == null) ? 0 : getOSVersion().hashCode());
        hashCode = prime * hashCode + ((getParentProduct() == null) ? 0 : getParentProduct().hashCode());
        hashCode = prime * hashCode + ((getParentProductVersion() == null) ? 0 : getParentProductVersion().hashCode());
        hashCode = prime * hashCode + ((getMetricData() == null) ? 0 : getMetricData().hashCode());
        return hashCode;
    }

    @Override
    public PostMetricsRequest clone() {
        return (PostMetricsRequest) super.clone();
    }

    @Override
    public Class<? extends RequestSigner> signerType() {
        return com.amazonaws.opensdk.protect.auth.IamRequestSigner.class;
    }

    /**
     * Set the configuration for this request.
     *
     * @param sdkRequestConfig
     *        Request configuration.
     * @return This object for method chaining.
     */
    public PostMetricsRequest sdkRequestConfig(com.amazonaws.opensdk.SdkRequestConfig sdkRequestConfig) {
        super.sdkRequestConfig(sdkRequestConfig);
        return this;
    }

}
