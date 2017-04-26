/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.lambda.upload.wizard.page;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

/**
 * To emulate onEnter event for JFace wizard page.
 * http://www.eclipse.org/forums/index.php/t/88927/
 */
abstract class WizardPageWithOnEnterHook extends WizardPage {

    protected WizardPageWithOnEnterHook(String pageName) {
        super(pageName);
    }

    protected abstract void onEnterPage();

    @Override
    public final boolean canFlipToNextPage() {
        return isPageComplete() && getNextPage(false) != null;
    }

    @Override
    public final IWizardPage getNextPage() {
        return getNextPage(true);
    }

    private IWizardPage getNextPage(boolean aboutToShow) {
        IWizardPage nextPage = super.getNextPage();

        if (aboutToShow && (nextPage instanceof WizardPageWithOnEnterHook)) {
            ((WizardPageWithOnEnterHook) nextPage).onEnterPage();
        }

        return nextPage;
    }
}
