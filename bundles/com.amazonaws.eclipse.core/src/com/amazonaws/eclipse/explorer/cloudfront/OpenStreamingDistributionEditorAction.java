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

import org.eclipse.ui.IEditorInput;

import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.cloudfront.model.StreamingDistributionSummary;

public class OpenStreamingDistributionEditorAction extends AbstractOpenAwsEditorAction {
    private StreamingDistributionSummary streamingDistributionSummary;

    public OpenStreamingDistributionEditorAction(StreamingDistributionSummary distributionSummary) {
        super("CloudFront Streaming Distribution Editor",
            "com.amazonaws.eclipse.explorer.cloudfront.streamingDistributionEditor",
            ServiceAbbreviations.CLOUDFRONT);
        this.streamingDistributionSummary = distributionSummary;
    }

    @Override
    public IEditorInput createEditorInput(String endpoint, String accountId) {
        return new StreamingDistributionEditorInput(streamingDistributionSummary, endpoint, accountId);
    }
}
