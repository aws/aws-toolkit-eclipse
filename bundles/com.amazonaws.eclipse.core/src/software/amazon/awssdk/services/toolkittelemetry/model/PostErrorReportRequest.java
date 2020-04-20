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
public class PostErrorReportRequest extends com.amazonaws.opensdk.BaseRequest implements Serializable, Cloneable, RequestSignerAware {

    private String aWSProduct;

    private String aWSProductVersion;

    private java.util.List<MetadataEntry> metadata;

    private Userdata userdata;

    private ErrorDetails errorDetails;

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

    public PostErrorReportRequest aWSProduct(String aWSProduct) {
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

    public PostErrorReportRequest aWSProduct(AWSProduct aWSProduct) {
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

    public PostErrorReportRequest aWSProductVersion(String aWSProductVersion) {
        setAWSProductVersion(aWSProductVersion);
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

    public PostErrorReportRequest metadata(MetadataEntry... metadata) {
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

    public PostErrorReportRequest metadata(java.util.Collection<MetadataEntry> metadata) {
        setMetadata(metadata);
        return this;
    }

    /**
     * @param userdata
     */

    public void setUserdata(Userdata userdata) {
        this.userdata = userdata;
    }

    /**
     * @return
     */

    public Userdata getUserdata() {
        return this.userdata;
    }

    /**
     * @param userdata
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostErrorReportRequest userdata(Userdata userdata) {
        setUserdata(userdata);
        return this;
    }

    /**
     * @param errorDetails
     */

    public void setErrorDetails(ErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    /**
     * @return
     */

    public ErrorDetails getErrorDetails() {
        return this.errorDetails;
    }

    /**
     * @param errorDetails
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostErrorReportRequest errorDetails(ErrorDetails errorDetails) {
        setErrorDetails(errorDetails);
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
        if (getMetadata() != null)
            sb.append("Metadata: ").append(getMetadata()).append(",");
        if (getUserdata() != null)
            sb.append("Userdata: ").append(getUserdata()).append(",");
        if (getErrorDetails() != null)
            sb.append("ErrorDetails: ").append(getErrorDetails());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof PostErrorReportRequest == false)
            return false;
        PostErrorReportRequest other = (PostErrorReportRequest) obj;
        if (other.getAWSProduct() == null ^ this.getAWSProduct() == null)
            return false;
        if (other.getAWSProduct() != null && other.getAWSProduct().equals(this.getAWSProduct()) == false)
            return false;
        if (other.getAWSProductVersion() == null ^ this.getAWSProductVersion() == null)
            return false;
        if (other.getAWSProductVersion() != null && other.getAWSProductVersion().equals(this.getAWSProductVersion()) == false)
            return false;
        if (other.getMetadata() == null ^ this.getMetadata() == null)
            return false;
        if (other.getMetadata() != null && other.getMetadata().equals(this.getMetadata()) == false)
            return false;
        if (other.getUserdata() == null ^ this.getUserdata() == null)
            return false;
        if (other.getUserdata() != null && other.getUserdata().equals(this.getUserdata()) == false)
            return false;
        if (other.getErrorDetails() == null ^ this.getErrorDetails() == null)
            return false;
        if (other.getErrorDetails() != null && other.getErrorDetails().equals(this.getErrorDetails()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getAWSProduct() == null) ? 0 : getAWSProduct().hashCode());
        hashCode = prime * hashCode + ((getAWSProductVersion() == null) ? 0 : getAWSProductVersion().hashCode());
        hashCode = prime * hashCode + ((getMetadata() == null) ? 0 : getMetadata().hashCode());
        hashCode = prime * hashCode + ((getUserdata() == null) ? 0 : getUserdata().hashCode());
        hashCode = prime * hashCode + ((getErrorDetails() == null) ? 0 : getErrorDetails().hashCode());
        return hashCode;
    }

    @Override
    public PostErrorReportRequest clone() {
        return (PostErrorReportRequest) super.clone();
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
    public PostErrorReportRequest sdkRequestConfig(com.amazonaws.opensdk.SdkRequestConfig sdkRequestConfig) {
        super.sdkRequestConfig(sdkRequestConfig);
        return this;
    }

}
