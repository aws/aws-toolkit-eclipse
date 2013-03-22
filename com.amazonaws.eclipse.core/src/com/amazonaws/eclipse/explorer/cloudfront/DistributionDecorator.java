/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.cloudfront;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontContentProvider.DistributionNode;
import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontContentProvider.StreamingDistributionNode;
import com.amazonaws.services.cloudfront_2012_03_15.model.CustomOrigin;
import com.amazonaws.services.cloudfront_2012_03_15.model.S3Origin;

public class DistributionDecorator implements ILightweightLabelDecorator {

    public void addListener(ILabelProviderListener listener) {}
    public void removeListener(ILabelProviderListener listener) {}
    public void dispose() {}

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof DistributionNode) {
            DistributionNode distributionNode = (DistributionNode)element;

            String origin = createOriginString(
                distributionNode.getDistributionSummary().getS3Origin(),
                distributionNode.getDistributionSummary().getCustomOrigin());
            
            decoration.addSuffix(" " + origin);
        } else if (element instanceof StreamingDistributionNode) {
            StreamingDistributionNode distributionNode = (StreamingDistributionNode)element;

            String origin = createOriginString(
                distributionNode.getDistributionSummary().getS3Origin(),
                null);
            
            decoration.addSuffix(" " + origin);
        }
    }
    
    private String createOriginString(S3Origin s3Origin, CustomOrigin customOrigin) {
        if (s3Origin != null) {
            return s3Origin.getDNSName();
        } else if (customOrigin != null) {
            return customOrigin.getDNSName();
        }
        
        return null;
    }
    
}
