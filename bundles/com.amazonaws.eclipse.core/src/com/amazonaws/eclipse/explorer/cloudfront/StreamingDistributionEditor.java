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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontActions.DisableStreamingDistributionAction;
import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontActions.EnableStreamingDistributionAction;
import com.amazonaws.services.cloudfront.model.GetStreamingDistributionRequest;
import com.amazonaws.services.cloudfront.model.StreamingDistribution;
import com.amazonaws.services.cloudfront.model.StreamingDistributionConfig;

public class StreamingDistributionEditor extends AbstractDistributionEditor {

    private StreamingDistributionEditorInput editorInput;
    private EnableStreamingDistributionAction enableDistributionAction;
    private DisableStreamingDistributionAction disableDistributionAction;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        editorInput = (StreamingDistributionEditorInput)input;
    }

    @Override
    public void refreshData() {
        new LoadDistributionInfoThread().start();
    }

    @Override
    protected boolean supportsDefaultRootObjects() {
        return false;
    }

    @Override
    protected String getResourceTitle() {
        return "Streaming Distribution";
    }

    @Override
    protected void contributeActions(IToolBarManager toolbarManager) {
        enableDistributionAction = new EnableStreamingDistributionAction(editorInput.getDistributionId(), editorInput.getAccountId(), this);
        disableDistributionAction = new DisableStreamingDistributionAction(editorInput.getDistributionId(), editorInput.getAccountId(), this);

        toolbarManager.add(enableDistributionAction);
        toolbarManager.add(disableDistributionAction);
    }

    private class LoadDistributionInfoThread extends ResourceEditorDataLoaderThread {
        @Override
        public void loadData() {
            final StreamingDistribution distribution = getClient().getStreamingDistribution(new GetStreamingDistributionRequest(editorInput.getDistributionId())).getStreamingDistribution();

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    setText(domainNameText, distribution.getDomainName());
                    setText(distributionIdText, distribution.getId());
                    setText(lastModifiedText, distribution.getLastModifiedTime());
                    setText(statusText, distribution.getStatus());

                    StreamingDistributionConfig distributionConfig = distribution.getStreamingDistributionConfig();
                    setText(commentText, distributionConfig.getComment());
                    setText(originText, distributionConfig.getS3Origin().getDomainName());
                    setText(enabledText, distributionConfig.getEnabled());

                    cnamesList.removeAll();
                    for (String cname : distributionConfig.getAliases().getItems()) {
                        cnamesList.add(cname);
                    }

                    if (distributionConfig.getLogging() != null) {
                        setText(loggingEnabledText, "Yes");
                        setText(loggingBucketText, distributionConfig.getLogging().getBucket());
                        setText(loggingPrefixText, distributionConfig.getLogging().getPrefix());
                    } else {
                        setText(loggingEnabledText, "No");
                        loggingBucketText.setText("N/A");
                        loggingPrefixText.setText("N/A");
                    }

                    enableDistributionAction.setEnabled(distributionConfig.isEnabled() == false);
                    disableDistributionAction.setEnabled(distributionConfig.isEnabled() == true);
                    updateToolbar();
                }
            });
        }
    }
}
