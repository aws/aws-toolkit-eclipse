/*
 * Copyright 2011-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * A single content provider for the AWS Explorer view that notifies the user
 * of process-wide configuration issues like not having configured any
 * credentials to use for communicating with AWS. If there is such an issue,
 * this provider contributes a single child to the AWSResourcesRootElement
 * that describes the error and gives quick links to fix it. Otherwise, it
 * lurks silently in the background contributing nothing.
 */
public class AccountNotConfiguredContentProvider
    implements ITreeContentProvider {

    @Override
    public void inputChanged(final Viewer viewer,
                             final Object oldInput,
                             final Object newInput) {
    }

    /**
     * This content provider contributes a child to the root element if and
     * only if there are no valid credentials configured.
     */
    @Override
    public boolean hasChildren(final Object element) {
        return ((element instanceof AWSResourcesRootElement)
                    && !areCredentialsConfigured());
    }

    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement
                && !areCredentialsConfigured()) {

            return new Object[] { new AccountNotConfiguredNode() };
        }

        return new Object[0];
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object getParent(final Object element) {
        return null;
    }

    @Override
    public void dispose() {
    }

    private boolean areCredentialsConfigured() {
        return AwsToolkitCore.getDefault().getAccountInfo().isValid();
    }

    /**
     * An action that opens the account preferences tab so the user
     * can configure some credentials.
     */
    private static final class OpenAccountPreferencesAction extends Action {
        public OpenAccountPreferencesAction() {
            super.setText("Configure AWS Accounts");
            super.setImageDescriptor(AwsToolkitCore
                .getDefault()
                .getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_GEAR)
            );
        }

        @Override
        public void run() {
            String resource = AwsToolkitCore.ACCOUNT_PREFERENCE_PAGE_ID;

            PreferencesUtil.createPreferenceDialogOn(
                null,         // shell; null uses the active workbench window
                resource,
                new String[] { resource },
                null          // data; not used in this case
            ).open();
        }
    }

    /**
     * ExplorerNode alerting users that the current account is not fully
     * configured.
     */
    public static class AccountNotConfiguredNode extends ExplorerNode {
        public AccountNotConfiguredNode() {
            super("AWS Account not Configured",
                  0, // sort priority.
                  ExplorerNode.loadImage(AwsToolkitCore.IMAGE_GEARS),
                  new OpenAccountPreferencesAction());
        }
    }
}
