/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codestar.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.codecommit.credentials.GitCredentialsManager;
import com.amazonaws.eclipse.codestar.CodeStarAnalytics;
import com.amazonaws.eclipse.codestar.CodeStarPlugin;
import com.amazonaws.eclipse.codestar.UIConstants;
import com.amazonaws.eclipse.codestar.CodeStarAnalytics.EventResult;
import com.amazonaws.eclipse.codestar.model.CodeStarProjectCheckoutWizardDataModel;
import com.amazonaws.eclipse.codestar.page.CodeStarProjectCheckoutPage;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.egit.GitRepositoryInfo;
import com.amazonaws.eclipse.core.egit.RepositorySelection;
import com.amazonaws.eclipse.core.egit.UIText;
import com.amazonaws.eclipse.core.egit.jobs.CloneGitRepositoryJob;
import com.amazonaws.eclipse.core.egit.jobs.ImportProjectJob;
import com.amazonaws.eclipse.core.egit.ui.CloneDestinationPage;
import com.amazonaws.eclipse.core.egit.ui.SourceBranchPage;
import com.amazonaws.eclipse.core.util.WorkbenchUtils;

/**
 * Wizard for importing an existing CodeStar project.
 */
@SuppressWarnings({ "restriction" })
public class CodeStarProjectCheckoutWizard extends Wizard implements IImportWizard {

    protected IWorkbench workbench;
    protected CodeStarProjectCheckoutWizardDataModel dataModel;

    // a page for CodeStar project selection
    protected CodeStarProjectCheckoutPage checkoutPage;
    // a page for repository branch selection
    protected SourceBranchPage sourceBranchPage;
    // a page for selection of the clone destination
    protected CloneDestinationPage cloneDestinationPage;

    /**
     * the current selected repository info.
     */
    private volatile GitRepositoryInfo currentGitRepositoryInfo;

    /**
     * Construct CodeStarProjectCheckoutWizard by not providing a data model will
     * open up the CodeStarProjectCheckoutPage.
     */
    public CodeStarProjectCheckoutWizard() {
        super();
        setWindowTitle(UIConstants.CODESTAR_PROJECT_CHECKOUT_WIZARD_TITLE);
        setDefaultPageImageDescriptor(
                AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        setNeedsProgressMonitor(true);

        dataModel = new CodeStarProjectCheckoutWizardDataModel();
        checkoutPage = new CodeStarProjectCheckoutPage(this.dataModel);

        sourceBranchPage = createSourceBranchPage();
        cloneDestinationPage = createCloneDestinationPage();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection arg1) {
        this.workbench = workbench;
    }

    @Override
    final public void addPages() {
        addPage(checkoutPage);
        addPage(sourceBranchPage);
        addPage(cloneDestinationPage);
    }

    @Override
    public boolean performFinish() {
        try {
            final File destinationFile = cloneDestinationPage.getDestinationFile();
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.subTask("Cloning repository...");
                    try {
                        new CloneGitRepositoryJob(CodeStarProjectCheckoutWizard.this, sourceBranchPage, cloneDestinationPage, getGitRepositoryInfo())
                            .execute(monitor);
                    } catch (URISyntaxException e) {
                        throw new InvocationTargetException(e);
                    }
                    GitCredentialsManager.getGitCredentials().put(
                            AwsToolkitCore.getDefault().getAccountManager()
                                    .getAllAccountNames()
                                    .get(dataModel.getAccountId()),
                            new GitCredential(
                                    dataModel.getGitCredentialsDataModel()
                                            .getUsername(), dataModel
                                            .getGitCredentialsDataModel()
                                            .getPassword()));
                    monitor.subTask("Importing project...");
                    IFile fileToOpen = new ImportProjectJob(dataModel.getProjectId(), destinationFile)
                            .execute(monitor);

                    if (fileToOpen != null) {
                        WorkbenchUtils.selectAndReveal(fileToOpen, workbench); // show in explorer
                        WorkbenchUtils.openFileInEditor(fileToOpen, workbench); // show in editor
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            CodeStarAnalytics.trackImportProject(EventResult.FAILED);
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
            return false;
        } catch (InterruptedException e) {
            CodeStarAnalytics.trackImportProject(EventResult.CANCELED);
            CodeStarPlugin.getDefault().reportException(
                    UIText.GitCreateProjectViaWizardWizard_AbortedMessage, e);
            return false;
        }
        CodeStarAnalytics.trackImportProject(EventResult.SUCCEEDED);
        return true;
    }

    @Override
    public boolean performCancel() {
        CodeStarAnalytics.trackImportProject(EventResult.CANCELED);
        return super.performCancel();
    }

    private SourceBranchPage createSourceBranchPage() {
        return new SourceBranchPage() {
            @Override
            public void setVisible(boolean visible) {
                if (visible) {
                    setSelection(getRepositorySelection());
                    setCredentials(getCredentials());
                }
                super.setVisible(visible);
            }
        };
    }

    private CloneDestinationPage createCloneDestinationPage() {
        return new CloneDestinationPage() {
            @Override
            public void setVisible(boolean visible) {
                if (visible)
                    setSelection(getRepositorySelection(),
                            sourceBranchPage.getAvailableBranches(),
                            sourceBranchPage.getSelectedBranches(),
                            sourceBranchPage.getHEAD());
                super.setVisible(visible);
            }
        };
    }

    /**
     * @return the repository specified in the data model.
     */
    private RepositorySelection getRepositorySelection() {

        try {
            return new RepositorySelection(new URIish(
                    dataModel.getRepoHttpUrl()), null);
        } catch (URISyntaxException e) {
            CodeStarPlugin.getDefault().reportException(
                    UIText.GitImportWizard_errorParsingURI, e);
            return null;
        } catch (Exception e) {
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @return the credentials
     */
    protected UserPasswordCredentials getCredentials() {
        try {
            return getGitRepositoryInfo().getCredentials();
        } catch (Exception e) {
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
            return null;
        }
    }

    /**
     * currentGitRepositoryInfo should be updated along with the data model.
     *
     * @return The GitRepositoryInfo that is being currently working in.
     * @throws URISyntaxException
     */
    public GitRepositoryInfo getGitRepositoryInfo() throws URISyntaxException {
        if (currentGitRepositoryInfo == null
                || !dataModel.getRepoHttpUrl().equals(
                        currentGitRepositoryInfo.getCloneUri())) {
            currentGitRepositoryInfo = new GitRepositoryInfo(
                    dataModel.getRepoHttpUrl());
            currentGitRepositoryInfo.setRepositoryName(dataModel.getRepoName());
        }
        currentGitRepositoryInfo.setShouldSaveCredentialsInSecureStore(true);
        currentGitRepositoryInfo.setCredentials(dataModel
                .getGitCredentialsDataModel().getUsername(), dataModel
                .getGitCredentialsDataModel().getPassword());
        return currentGitRepositoryInfo;
    }
}
