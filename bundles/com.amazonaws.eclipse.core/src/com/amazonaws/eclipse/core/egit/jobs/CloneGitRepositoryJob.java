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
package com.amazonaws.eclipse.core.egit.jobs;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConfigureFetchAfterCloneTask;
import org.eclipse.egit.core.op.ConfigurePushAfterCloneTask;
import org.eclipse.egit.core.op.SetRepositoryConfigPropertyTask;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.egit.EGitCredentialsProvider;
import com.amazonaws.eclipse.core.egit.GitRepositoryInfo;
import com.amazonaws.eclipse.core.egit.SecureStoreUtils;
import com.amazonaws.eclipse.core.egit.UIText;
import com.amazonaws.eclipse.core.egit.GitRepositoryInfo.PushInfo;
import com.amazonaws.eclipse.core.egit.GitRepositoryInfo.RepositoryConfigProperty;
import com.amazonaws.eclipse.core.egit.ui.CloneDestinationPage;
import com.amazonaws.eclipse.core.egit.ui.SourceBranchPage;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;

/**
 * A UI sync job that manages cloning a remote Git repository to local.
 */
@SuppressWarnings("restriction")
public class CloneGitRepositoryJob {
    private final Wizard wizard;
    private final SourceBranchPage sourceBranchPage;
    private final CloneDestinationPage cloneDestinationPage;
    private final GitRepositoryInfo gitRepositoryInfo;

    public CloneGitRepositoryJob(Wizard wizard, SourceBranchPage sourceBranchPage, CloneDestinationPage cloneDestinationPage, GitRepositoryInfo gitRepositoryInfo) {
        this.wizard = wizard;
        this.sourceBranchPage = sourceBranchPage;
        this.cloneDestinationPage = cloneDestinationPage;
        this.gitRepositoryInfo = gitRepositoryInfo;
    }

    public void execute(IProgressMonitor monitor) {

        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    final CloneOperation cloneOperation = generateCloneOperation();
                    wizard.getContainer().run(true, true, new IRunnableWithProgress() {
                        @Override
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            executeCloneOperation(cloneOperation, monitor);
                        }
                    });
                    cloneDestinationPage.saveSettingsForClonedRepo();
                } catch (Exception e) {
                    AwsToolkitCore.getDefault()
                            .reportException("Failed to clone git repository.",
                                    new AwsActionException(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CLONE_REPO.getName(), e.getMessage(), e));
                }
            }
        });
    }

    /**
     * Do the clone using data which were collected on the pages
     * {@code validSource} and {@code cloneDestination}
     *
     * @param gitRepositoryInfo
     * @return if clone was successful
     * @throws URISyntaxException
     */
    private CloneOperation generateCloneOperation()
            throws URISyntaxException {

        final boolean allSelected;
        final Collection<Ref> selectedBranches;
        if (sourceBranchPage.isSourceRepoEmpty()) {
            // fetch all branches of empty repo
            allSelected = true;
            selectedBranches = Collections.emptyList();
        } else {
            allSelected = sourceBranchPage.isAllSelected();
            selectedBranches = sourceBranchPage.getSelectedBranches();
        }
        final File workdir = cloneDestinationPage.getDestinationFile();
        boolean created = workdir.exists();
        if (!created)
            created = workdir.mkdirs();

        if (!created || !workdir.isDirectory()) {
            final String errorMessage = NLS.bind(
                    UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
            ErrorDialog.openError(wizard.getShell(), wizard.getWindowTitle(),
                    UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
                            Activator.getPluginId(), 0, errorMessage, null));
            // let's give user a chance to fix this minor problem
            return null;
        }

        return createCloneOperation(allSelected,
                selectedBranches, workdir,
                cloneDestinationPage.getInitialBranch(),
                cloneDestinationPage.getRemote(),
                cloneDestinationPage.isCloneSubmodules());
    }

    private CloneOperation createCloneOperation(
            final boolean allSelected, final Collection<Ref> selectedBranches,
            final File workdir, final Ref ref, final String remoteName,
            final boolean isCloneSubmodules) throws URISyntaxException {

        URIish uri = new URIish(gitRepositoryInfo.getCloneUri());
        UserPasswordCredentials credentials = gitRepositoryInfo.getCredentials();
        int timeout = Activator.getDefault().getPreferenceStore()
                .getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
        wizard.setWindowTitle(NLS.bind(UIText.GitCloneWizard_jobName, uri.toString()));

        final CloneOperation op = new CloneOperation(uri, allSelected,
                selectedBranches, workdir, ref != null ? ref.getName() : null,
                remoteName, timeout);
        if (credentials != null)
            op.setCredentialsProvider(new EGitCredentialsProvider(credentials
                    .getUser(), credentials.getPassword()));
        else
            op.setCredentialsProvider(new EGitCredentialsProvider());
        op.setCloneSubmodules(isCloneSubmodules);

        configureFetchSpec(op, remoteName);
        configurePush(op, remoteName);
        configureRepositoryConfig(op);

        return op;
    }

    private void configureFetchSpec(CloneOperation op, String remoteName) {
        for (String fetchRefSpec : gitRepositoryInfo.getFetchRefSpecs())
            op.addPostCloneTask(new ConfigureFetchAfterCloneTask(remoteName,
                    fetchRefSpec));
    }

    private void configurePush(CloneOperation op, String remoteName) {
        for (PushInfo pushInfo : gitRepositoryInfo.getPushInfos())
            try {
                URIish uri = pushInfo.getPushUri() != null ? new URIish(
                        pushInfo.getPushUri()) : null;
                ConfigurePushAfterCloneTask task = new ConfigurePushAfterCloneTask(
                        remoteName, pushInfo.getPushRefSpec(), uri);
                op.addPostCloneTask(task);
            } catch (URISyntaxException e) {
                Activator.handleError(UIText.GitCloneWizard_failed, e, true);
            }
    }

    private void configureRepositoryConfig(CloneOperation op) {
        for (RepositoryConfigProperty p : gitRepositoryInfo
                .getRepositoryConfigProperties()) {
            SetRepositoryConfigPropertyTask task = new SetRepositoryConfigPropertyTask(
                    p.getSection(), p.getSubsection(), p.getName(),
                    p.getValue());
            op.addPostCloneTask(task);
        }
    }

    private IStatus executeCloneOperation(final CloneOperation op,
            final IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {

        final RepositoryUtil util = Activator.getDefault().getRepositoryUtil();

        op.run(monitor);
        util.addConfiguredRepository(op.getGitDir());
        try {
            if (gitRepositoryInfo.shouldSaveCredentialsInSecureStore())
                SecureStoreUtils.storeCredentials(gitRepositoryInfo.getCredentials(),
                        new URIish(gitRepositoryInfo.getCloneUri()));
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logWarning(e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }

}
