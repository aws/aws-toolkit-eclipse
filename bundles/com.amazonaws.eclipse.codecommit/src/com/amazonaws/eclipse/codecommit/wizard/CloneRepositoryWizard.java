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
package com.amazonaws.eclipse.codecommit.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.amazonaws.eclipse.codecommit.CodeCommitAnalytics;
import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.codecommit.CodeCommitAnalytics.EventResult;
import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.codecommit.credentials.GitCredentialsManager;
import com.amazonaws.eclipse.codecommit.pages.GitCredentialsConfigurationPage;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.egit.GitRepositoryInfo;
import com.amazonaws.eclipse.core.egit.RepositorySelection;
import com.amazonaws.eclipse.core.egit.UIText;
import com.amazonaws.eclipse.core.egit.jobs.CloneGitRepositoryJob;
import com.amazonaws.eclipse.core.egit.jobs.ImportProjectJob;
import com.amazonaws.eclipse.core.egit.ui.CloneDestinationPage;
import com.amazonaws.eclipse.core.egit.ui.SourceBranchPage;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.model.GitCredentialsDataModel;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.model.GetRepositoryRequest;
import com.amazonaws.services.codecommit.model.RepositoryMetadata;

public class CloneRepositoryWizard extends Wizard implements IImportWizard {
    protected IWorkbench workbench;

    private final AWSCodeCommit client;
    private final String repositoryName;
    private final String currentProfile;
    private final GitCredentialsDataModel dataModel = new GitCredentialsDataModel();

    protected GitCredentialsConfigurationPage credentialConfigPage;
    // a page for repository branch selection
    protected SourceBranchPage sourceBranchPage;
    // a page for selection of the clone destination
    protected CloneDestinationPage cloneDestinationPage;

    private GitRepositoryInfo gitRepositoryInfo;

    public CloneRepositoryWizard(String accountId, String regionId, String repositoryName) throws URISyntaxException {
        super();
        if (accountId == null) {
            accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        }
        if (regionId == null) {
            regionId = RegionUtils.getCurrentRegion().getId();
        }
        this.client = AwsToolkitCore.getClientFactory(accountId).getCodeCommitClientByRegion(regionId);
        this.repositoryName = repositoryName;
        this.currentProfile = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(accountId).getAccountName();

        setWindowTitle("Clone AWS CodeCommit Repository");
        setNeedsProgressMonitor(true);
        GitCredential credential = GitCredentialsManager.getGitCredential(currentProfile);
        if (credential != null) {
            dataModel.setUsername(credential.getUsername());
            dataModel.setPassword(credential.getPassword());
        }
        dataModel.setUserAccount(accountId);
        dataModel.setRegionId(regionId);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public final void addPages() {
        if (!CodeCommitPlugin.currentProfileGitCredentialsConfigured()) {
            credentialConfigPage = new GitCredentialsConfigurationPage(dataModel);
            addPage(credentialConfigPage);
        }
        sourceBranchPage = createSourceBranchPage();
        cloneDestinationPage = createCloneDestinationPage();
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
                    GitCredentialsManager.getGitCredentials().put(currentProfile,
                            new GitCredential(dataModel.getUsername(), dataModel.getPassword()));
                    GitCredentialsManager.saveGitCredentials();
                    try {
                        new CloneGitRepositoryJob(CloneRepositoryWizard.this, sourceBranchPage, cloneDestinationPage, getTheGitRepositoryInfo())
                            .execute(monitor);
                    } catch (URISyntaxException e) {
                        throw new InvocationTargetException(e);
                    }
                    monitor.subTask("Importing project...");
                    IFile fileToOpen = new ImportProjectJob(repositoryName, destinationFile)
                            .execute(monitor);

                    if (fileToOpen != null) {
                        WorkbenchUtils.selectAndReveal(fileToOpen, workbench); // show in explorer
                        WorkbenchUtils.openFileInEditor(fileToOpen, workbench); // show in editor
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            CodeCommitAnalytics.trackCloneRepository(EventResult.FAILED);
            CodeCommitPlugin.getDefault().reportException("Failed to clone git repository.",
                    new AwsActionException(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CLONE_REPO.getName(), e.getMessage(), e));
            return false;
        } catch (InterruptedException e) {
            CodeCommitAnalytics.trackCloneRepository(EventResult.CANCELED);
            CodeCommitPlugin.getDefault().reportException(
                    UIText.GitCreateProjectViaWizardWizard_AbortedMessage, e);
            return false;
        }
        CodeCommitAnalytics.trackCloneRepository(EventResult.SUCCEEDED);
        return true;
    }

    @Override
    public boolean performCancel() {
        CodeCommitAnalytics.trackCloneRepository(EventResult.CANCELED);
        return super.performCancel();
    }

    private SourceBranchPage createSourceBranchPage() {
        return new SourceBranchPage() {
            @Override
            public void setVisible(boolean visible) {
                if (visible) {
                    try {
                        setSelection(getRepositorySelection());
                        setCredentials(getTheGitRepositoryInfo().getCredentials());
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
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
            return new RepositorySelection(new URIish(getTheGitRepositoryInfo().getCloneUri()), null);
        } catch (URISyntaxException e) {
            CodeCommitPlugin.getDefault().reportException(
                    UIText.GitImportWizard_errorParsingURI, e);
            return null;
        } catch (Exception e) {
            CodeCommitPlugin.getDefault().reportException(e.getMessage(), e);
            return null;
        }
    }

    private GitRepositoryInfo getTheGitRepositoryInfo() throws URISyntaxException {
        if (gitRepositoryInfo == null) {
            RepositoryMetadata metadata = client.getRepository(
                    new GetRepositoryRequest()
                    .withRepositoryName(repositoryName))
                    .getRepositoryMetadata();
            this.gitRepositoryInfo = createGitRepositoryInfo(
                    metadata.getCloneUrlHttp(), metadata.getRepositoryName(),
                    dataModel.getUsername(), dataModel.getPassword());
        }
        return this.gitRepositoryInfo;
    }

    private static GitRepositoryInfo createGitRepositoryInfo(String httpUrl, String repoName, String username, String password) throws URISyntaxException {
        GitRepositoryInfo info = new GitRepositoryInfo(httpUrl);
        info.setRepositoryName(repoName);
        info.setShouldSaveCredentialsInSecureStore(true);
        info.setCredentials(username, password);
        return info;
    }

}
