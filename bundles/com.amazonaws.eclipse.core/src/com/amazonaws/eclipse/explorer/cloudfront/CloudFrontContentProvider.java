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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.model.DistributionList;
import com.amazonaws.services.cloudfront.model.DistributionSummary;
import com.amazonaws.services.cloudfront.model.ListDistributionsRequest;
import com.amazonaws.services.cloudfront.model.ListStreamingDistributionsRequest;
import com.amazonaws.services.cloudfront.model.StreamingDistributionList;
import com.amazonaws.services.cloudfront.model.StreamingDistributionSummary;

public class CloudFrontContentProvider extends AbstractContentProvider {

    public static class CloudFrontRootElement {
        public static final CloudFrontRootElement ROOT_ELEMENT = new CloudFrontRootElement();
    }

    public static class StreamingDistributionNode extends ExplorerNode {
        private final StreamingDistributionSummary distributionSummary;

        public StreamingDistributionNode(StreamingDistributionSummary distributionSummary) {
            super(distributionSummary.getDomainName(), 0,
                loadImage(AwsToolkitCore.IMAGE_STREAMING_DISTRIBUTION),
                new OpenStreamingDistributionEditorAction(distributionSummary));
            this.distributionSummary = distributionSummary;
        }

        public StreamingDistributionSummary getDistributionSummary() {
            return distributionSummary;
        }
    }

    public static class DistributionNode extends ExplorerNode {
        private final DistributionSummary distributionSummary;

        public DistributionNode(DistributionSummary distributionSummary) {
            super(distributionSummary.getDomainName(), 0,
                loadImage(AwsToolkitCore.IMAGE_DISTRIBUTION),
                new OpenDistributionEditorAction(distributionSummary));
            this.distributionSummary = distributionSummary;
        }

        public DistributionSummary getDistributionSummary() {
            return distributionSummary;
        }
    }


    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof CloudFrontRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { CloudFrontRootElement.ROOT_ELEMENT };
        }

        if (parentElement instanceof CloudFrontRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonCloudFront cf = AwsToolkitCore.getClientFactory().getCloudFrontClient();

                    List<ExplorerNode> distributionNodes = new ArrayList<>();

                    DistributionList distributionList = null;
                    do {
                        ListDistributionsRequest request = new ListDistributionsRequest();
                        if (distributionList != null) request.setMarker(distributionList.getNextMarker());

                        distributionList = cf.listDistributions(request).getDistributionList();
                        for (DistributionSummary distributionSummary : distributionList.getItems()) {
                            distributionNodes.add(new DistributionNode(distributionSummary));
                        }
                    } while (distributionList.isTruncated());


                    StreamingDistributionList streamingDistributionList = null;
                    do {
                        ListStreamingDistributionsRequest request = new ListStreamingDistributionsRequest();
                        if (streamingDistributionList != null) request.setMarker(streamingDistributionList.getNextMarker());

                        streamingDistributionList = cf.listStreamingDistributions(request).getStreamingDistributionList();
                        for (StreamingDistributionSummary distributionSummary : streamingDistributionList.getItems()) {
                            distributionNodes.add(new StreamingDistributionNode(distributionSummary));
                        }
                    } while (streamingDistributionList.isTruncated());

                    return distributionNodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.CLOUDFRONT;
    }

}
