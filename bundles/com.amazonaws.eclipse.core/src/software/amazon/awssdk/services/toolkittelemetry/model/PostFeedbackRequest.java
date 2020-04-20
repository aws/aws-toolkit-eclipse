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
public class PostFeedbackRequest extends com.amazonaws.opensdk.BaseRequest implements Serializable, Cloneable, RequestSignerAware {

    private String aWSProduct;

    private String aWSProductVersion;

    private String oS;

    private String oSVersion;

    private String parentProduct;

    private String parentProductVersion;

    private java.util.List<MetadataEntry> metadata;

    private String sentiment;

    private String comment;

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

    public PostFeedbackRequest aWSProduct(String aWSProduct) {
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

    public PostFeedbackRequest aWSProduct(AWSProduct aWSProduct) {
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

    public PostFeedbackRequest aWSProductVersion(String aWSProductVersion) {
        setAWSProductVersion(aWSProductVersion);
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

    public PostFeedbackRequest oS(String oS) {
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

    public PostFeedbackRequest oSVersion(String oSVersion) {
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

    public PostFeedbackRequest parentProduct(String parentProduct) {
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

    public PostFeedbackRequest parentProductVersion(String parentProductVersion) {
        setParentProductVersion(parentProductVersion);
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

    public PostFeedbackRequest metadata(MetadataEntry... metadata) {
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

    public PostFeedbackRequest metadata(java.util.Collection<MetadataEntry> metadata) {
        setMetadata(metadata);
        return this;
    }

    /**
     * @param sentiment
     * @see Sentiment
     */

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    /**
     * @return
     * @see Sentiment
     */

    public String getSentiment() {
        return this.sentiment;
    }

    /**
     * @param sentiment
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Sentiment
     */

    public PostFeedbackRequest sentiment(String sentiment) {
        setSentiment(sentiment);
        return this;
    }

    /**
     * @param sentiment
     * @see Sentiment
     */

    public void setSentiment(Sentiment sentiment) {
        sentiment(sentiment);
    }

    /**
     * @param sentiment
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Sentiment
     */

    public PostFeedbackRequest sentiment(Sentiment sentiment) {
        this.sentiment = sentiment.toString();
        return this;
    }

    /**
     * @param comment
     */

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return
     */

    public String getComment() {
        return this.comment;
    }

    /**
     * @param comment
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public PostFeedbackRequest comment(String comment) {
        setComment(comment);
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
        if (getOS() != null)
            sb.append("OS: ").append(getOS()).append(",");
        if (getOSVersion() != null)
            sb.append("OSVersion: ").append(getOSVersion()).append(",");
        if (getParentProduct() != null)
            sb.append("ParentProduct: ").append(getParentProduct()).append(",");
        if (getParentProductVersion() != null)
            sb.append("ParentProductVersion: ").append(getParentProductVersion()).append(",");
        if (getMetadata() != null)
            sb.append("Metadata: ").append(getMetadata()).append(",");
        if (getSentiment() != null)
            sb.append("Sentiment: ").append(getSentiment()).append(",");
        if (getComment() != null)
            sb.append("Comment: ").append(getComment());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof PostFeedbackRequest == false)
            return false;
        PostFeedbackRequest other = (PostFeedbackRequest) obj;
        if (other.getAWSProduct() == null ^ this.getAWSProduct() == null)
            return false;
        if (other.getAWSProduct() != null && other.getAWSProduct().equals(this.getAWSProduct()) == false)
            return false;
        if (other.getAWSProductVersion() == null ^ this.getAWSProductVersion() == null)
            return false;
        if (other.getAWSProductVersion() != null && other.getAWSProductVersion().equals(this.getAWSProductVersion()) == false)
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
        if (other.getMetadata() == null ^ this.getMetadata() == null)
            return false;
        if (other.getMetadata() != null && other.getMetadata().equals(this.getMetadata()) == false)
            return false;
        if (other.getSentiment() == null ^ this.getSentiment() == null)
            return false;
        if (other.getSentiment() != null && other.getSentiment().equals(this.getSentiment()) == false)
            return false;
        if (other.getComment() == null ^ this.getComment() == null)
            return false;
        if (other.getComment() != null && other.getComment().equals(this.getComment()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getAWSProduct() == null) ? 0 : getAWSProduct().hashCode());
        hashCode = prime * hashCode + ((getAWSProductVersion() == null) ? 0 : getAWSProductVersion().hashCode());
        hashCode = prime * hashCode + ((getOS() == null) ? 0 : getOS().hashCode());
        hashCode = prime * hashCode + ((getOSVersion() == null) ? 0 : getOSVersion().hashCode());
        hashCode = prime * hashCode + ((getParentProduct() == null) ? 0 : getParentProduct().hashCode());
        hashCode = prime * hashCode + ((getParentProductVersion() == null) ? 0 : getParentProductVersion().hashCode());
        hashCode = prime * hashCode + ((getMetadata() == null) ? 0 : getMetadata().hashCode());
        hashCode = prime * hashCode + ((getSentiment() == null) ? 0 : getSentiment().hashCode());
        hashCode = prime * hashCode + ((getComment() == null) ? 0 : getComment().hashCode());
        return hashCode;
    }

    @Override
    public PostFeedbackRequest clone() {
        return (PostFeedbackRequest) super.clone();
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
    public PostFeedbackRequest sdkRequestConfig(com.amazonaws.opensdk.SdkRequestConfig sdkRequestConfig) {
        super.sdkRequestConfig(sdkRequestConfig);
        return this;
    }

}
