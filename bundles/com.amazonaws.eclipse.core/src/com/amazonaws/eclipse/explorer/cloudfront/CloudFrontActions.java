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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.model.DistributionConfig;
import com.amazonaws.services.cloudfront.model.GetDistributionConfigRequest;
import com.amazonaws.services.cloudfront.model.GetDistributionConfigResult;
import com.amazonaws.services.cloudfront.model.GetStreamingDistributionConfigRequest;
import com.amazonaws.services.cloudfront.model.GetStreamingDistributionConfigResult;
import com.amazonaws.services.cloudfront.model.StreamingDistributionConfig;
import com.amazonaws.services.cloudfront.model.UpdateDistributionRequest;
import com.amazonaws.services.cloudfront.model.UpdateStreamingDistributionRequest;

public class CloudFrontActions {

    private static abstract class AbstractDistributionStateChangingAction extends Action {
        protected final String accountId;
        protected final String distributionId;
        private final IRefreshable refreshable;

        public AbstractDistributionStateChangingAction(String distributionId, String accountId, IRefreshable refreshable) {
            this.distributionId = distributionId;
            this.accountId = accountId;
            this.refreshable = refreshable;
            this.setDisabledImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_GREY_CIRCLE));
        }

        protected AmazonCloudFront getClient() {
            return AwsToolkitCore.getDefault().getClientFactory(accountId).getCloudFrontClient();
        }

        public abstract boolean isEnablingDistribution();

        public abstract void updateDistributionConfig();

        @Override
        public void run() {
            new Job("Updating distribution status") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        updateDistributionConfig();
                        if (refreshable != null) refreshable.refreshData();
                    } catch (Exception e) {
                        return new Status(Status.ERROR, AwsToolkitCore.getDefault().getPluginId(), "Unable to update distribution status", e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    private static abstract class StreamingDistributionStateChangingAction extends AbstractDistributionStateChangingAction {

        public StreamingDistributionStateChangingAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
        }

        @Override
        public void updateDistributionConfig() {
            AmazonCloudFront cf = getClient();
            GetStreamingDistributionConfigResult distributionConfigResult =
                cf.getStreamingDistributionConfig(new GetStreamingDistributionConfigRequest(distributionId));
            StreamingDistributionConfig distributionConfig = distributionConfigResult.getStreamingDistributionConfig();
            distributionConfig.setEnabled(isEnablingDistribution());

            cf.updateStreamingDistribution(new UpdateStreamingDistributionRequest()
                    .withIfMatch(distributionConfigResult.getETag())
                    .withStreamingDistributionConfig(distributionConfig)
                    .withId(distributionId));
        }
    }

    private static abstract class DistributionStateChangingAction extends AbstractDistributionStateChangingAction {
        public DistributionStateChangingAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
        }

        @Override
        public void updateDistributionConfig() {
            AmazonCloudFront cf = getClient();
            GetDistributionConfigResult distributionConfigResult =
                cf.getDistributionConfig(new GetDistributionConfigRequest(distributionId));
            DistributionConfig distributionConfig = distributionConfigResult.getDistributionConfig();
            distributionConfig.setEnabled(isEnablingDistribution());

            cf.updateDistribution(new UpdateDistributionRequest()
                    .withIfMatch(distributionConfigResult.getETag())
                    .withDistributionConfig(distributionConfig)
                    .withId(distributionId));
        }
    }

    public static class EnableStreamingDistributionAction extends StreamingDistributionStateChangingAction {
        public EnableStreamingDistributionAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
            this.setText("Enable");
            this.setToolTipText("Enable this streaming distribution");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_GREEN_CIRCLE));
        }

        @Override
        public boolean isEnablingDistribution() {
            return true;
        }
    }

    public static class DisableStreamingDistributionAction extends StreamingDistributionStateChangingAction {
        public DisableStreamingDistributionAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
            this.setText("Disable");
            this.setToolTipText("Disable this streaming distribution");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_RED_CIRCLE));
        }

        @Override
        public boolean isEnablingDistribution() {
            return false;
        }
    }

    public static class EnableDistributionAction extends DistributionStateChangingAction {
        public EnableDistributionAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
            this.setText("Enable");
            this.setToolTipText("Enable this distribution");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_GREEN_CIRCLE));
        }

        @Override
        public boolean isEnablingDistribution() {
            return true;
        }
    }

    public static class DisableDistributionAction extends DistributionStateChangingAction {
        public DisableDistributionAction(String distributionId, String accountId, IRefreshable refreshable) {
            super(distributionId, accountId, refreshable);
            this.setText("Disable");
            this.setToolTipText("Disable this distribution");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_RED_CIRCLE));
        }

        @Override
        public boolean isEnablingDistribution() {
            return false;
        }
    }
}
