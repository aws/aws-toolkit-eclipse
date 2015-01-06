package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

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
