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

import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontActions.DisableDistributionAction;
import com.amazonaws.eclipse.explorer.cloudfront.CloudFrontActions.EnableDistributionAction;
import com.amazonaws.services.cloudfront.model.Distribution;
import com.amazonaws.services.cloudfront.model.DistributionConfig;
import com.amazonaws.services.cloudfront.model.GetDistributionRequest;

public class DistributionEditor extends AbstractDistributionEditor {

    private DistributionEditorInput editorInput;
    private EnableDistributionAction enableDistributionAction;
    private DisableDistributionAction disableDistributionAction;


    @Override
    public void refreshData() {
        new LoadDistributionInfoThread().start();
    }

    @Override
    protected String getResourceTitle() {
        return "Distribution";
    }

    @Override
    protected void contributeActions(IToolBarManager toolbarManager) {
        enableDistributionAction = new EnableDistributionAction(editorInput.getDistributionId(), editorInput.getAccountId(), this);
        disableDistributionAction = new DisableDistributionAction(editorInput.getDistributionId(), editorInput.getAccountId(), this);

        toolbarManager.add(enableDistributionAction);
        toolbarManager.add(disableDistributionAction);
    }


    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        this.editorInput = (DistributionEditorInput)input;
    }

    private class LoadDistributionInfoThread extends ResourceEditorDataLoaderThread {
        @Override
        public void loadData() {
            final Distribution distribution = getClient().getDistribution(new GetDistributionRequest(editorInput.getDistributionId())).getDistribution();

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    setText(domainNameText, distribution.getDomainName());
                    setText(distributionIdText, distribution.getId());
                    setText(lastModifiedText, distribution.getLastModifiedTime());
                    setText(statusText, distribution.getStatus());

                    DistributionConfig distributionConfig = distribution.getDistributionConfig();
                    setText(enabledText, distributionConfig.getEnabled());
                    setText(commentText, distributionConfig.getComment());
                    setText(defaultRootObjectLabel, distributionConfig.getDefaultRootObject());

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

                    if (!distributionConfig.getOrigins().getItems().isEmpty()) {
                        setText(originText, distributionConfig.getOrigins().getItems().get(0).getDomainName());
                    } else {
                        originText.setText("N/A");
                    }

                    enableDistributionAction.setEnabled(distributionConfig.isEnabled() == false);
                    disableDistributionAction.setEnabled(distributionConfig.isEnabled() == true);
                    updateToolbar();
               }
            });
        }
    }
}
