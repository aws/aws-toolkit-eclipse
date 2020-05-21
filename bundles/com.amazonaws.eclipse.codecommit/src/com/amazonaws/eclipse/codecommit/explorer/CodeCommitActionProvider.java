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
package com.amazonaws.eclipse.codecommit.explorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.codecommit.wizard.CloneRepositoryWizard;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.ui.DeleteResourceConfirmationDialog;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.model.CreateRepositoryRequest;
import com.amazonaws.services.codecommit.model.DeleteRepositoryRequest;
import com.amazonaws.services.codecommit.model.RepositoryNameIdPair;
import com.amazonaws.util.StringUtils;

public class CodeCommitActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        StructuredSelection selection = (StructuredSelection)getActionSite().getStructuredViewer().getSelection();
        @SuppressWarnings("rawtypes")
        Iterator iterator = selection.iterator();
        boolean rootElementSelected = false;
        List<RepositoryNameIdPair> repositories = new ArrayList<>();
        while ( iterator.hasNext() ) {
            Object obj = iterator.next();
            if ( obj instanceof RepositoryNameIdPair ) {
                repositories.add((RepositoryNameIdPair) obj);
            }
            if ( obj instanceof CodeCommitRootElement ) {
                rootElementSelected = true;
            }
        }

        if ( rootElementSelected && repositories.isEmpty()) {
            menu.add(new CreateRepositoryAction());
        } else if ( !rootElementSelected && !repositories.isEmpty() ) {
            if (repositories.size() == 1) {
                menu.add(new CloneRepositoryAction(repositories.get(0)));
                menu.add(new OpenRepositoryEditorAction(repositories.get(0)));
                menu.add(new DeleteRepositoryAction(repositories.get(0)));
            }
        }
    }

    private static class CreateRepositoryAction extends AwsAction {
        public CreateRepositoryAction() {
            super(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CREATE_REPO);
            this.setText("Create Repository");
            this.setToolTipText("Create a secure repository to store and share your code.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        protected void doRun() {
            NewRepositoryDialog dialog = new NewRepositoryDialog(Display.getDefault().getActiveShell());
            if (Window.OK == dialog.open()) {
                AWSCodeCommit client = CodeCommitPlugin.getCurrentCodeCommitClient();
                try {
                    client.createRepository(new CreateRepositoryRequest()
                        .withRepositoryName(dialog.getRepositoryName())
                        .withRepositoryDescription(dialog.getRepositoryDescription()));
                    ContentProviderRegistry.refreshAllContentProviders();
                    actionSucceeded();

                } catch (Exception e) {
                    actionFailed();
                    CodeCommitPlugin.getDefault().reportException("Failed to create repository!",
                            new AwsActionException(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CREATE_REPO.getName(), e.getMessage(), e));
                }
            } else {
                actionCanceled();
            }
            actionFinished();
        }

        private static class NewRepositoryDialog extends TitleAreaDialog {
            public NewRepositoryDialog(Shell parentShell) {
                super(parentShell);
            }

            private Text repositoryNameText;
            private Text repositoryDescriptionText;

            private String repositoryName;
            private String repositoryDescription;

            @Override
            public void create() {
                    super.create();
                    setTitle("Create Repository");
                    setMessage("Create a secure repository to store and share your code. "
                            + "Begin by typing a repository name and a description for your repository. "
                            + "Repository names are included in the URLs for that repository.",
                            IMessageProvider.INFORMATION);
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
            }

            @Override
            protected Control createDialogArea(Composite parent) {
                    Composite area = (Composite) super.createDialogArea(parent);
                    Composite container = new Composite(area, SWT.NONE);
                    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
                    GridLayout layout = new GridLayout(2, false);
                    container.setLayout(layout);

                    createRepositoryNameSection(container);
                    createRepositoryDescriptionSection(container);

                    return area;
            }

            private void createRepositoryNameSection(Composite container) {
                    Label repositoryNameLabel = new Label(container, SWT.NONE);
                    repositoryNameLabel.setText("Repository Name*: ");

                    GridData gridData = new GridData();
                    gridData.grabExcessHorizontalSpace = true;
                    gridData.horizontalAlignment = SWT.FILL;

                    repositoryNameText = new Text(container, SWT.BORDER);
                    repositoryNameText.setMessage("100 character limit");
                    repositoryNameText.setLayoutData(gridData);
                    repositoryNameText.addModifyListener(new ModifyListener() {
                        @Override
                        public void modifyText(ModifyEvent event) {
                            String inputRepositoryName = repositoryNameText.getText();
                            getButton(IDialogConstants.OK_ID).setEnabled(!StringUtils.isNullOrEmpty(inputRepositoryName));
                        }
                    });
            }

            private void createRepositoryDescriptionSection(Composite container) {
                    Label repositoryDescriptionLabel = new Label(container, SWT.NONE);
                    repositoryDescriptionLabel.setText("Repository Description: ");

                    GridData gridData = new GridData();
                    gridData.grabExcessHorizontalSpace = true;
                    gridData.horizontalAlignment = SWT.FILL;
                    repositoryDescriptionText = new Text(container, SWT.BORDER);
                    repositoryDescriptionText.setMessage("1000 character limit");
                    repositoryDescriptionText.setLayoutData(gridData);
            }

            @Override
            protected boolean isResizable() {
                    return true;
            }

            private void saveInput() {
                    repositoryName = repositoryNameText.getText();
                    repositoryDescription = repositoryDescriptionText.getText();
            }

            @Override
            protected void okPressed() {
                    saveInput();
                    super.okPressed();
            }

            public String getRepositoryName() {
                    return repositoryName;
            }

            public String getRepositoryDescription() {
                    return repositoryDescription;
            }
        }

    }

    public static class CloneRepositoryAction extends AwsAction {
        private final String accountId;
        private final String regionId;
        private final String repositoryName;

        public CloneRepositoryAction(RepositoryNameIdPair repository) {
            this(null, null, repository.getRepositoryName());
        }

        public CloneRepositoryAction(RepositoryEditorInput repositoryEditorInput) {
            this(repositoryEditorInput.getAccountId(),
                    repositoryEditorInput.getRegionId(),
                    repositoryEditorInput.getRepository().getRepositoryName());
        }

        private CloneRepositoryAction(String accountId, String regionId, String repositoryName) {
            super(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CLONE_REPO);
            this.accountId = accountId;
            this.regionId = regionId;
            this.repositoryName = repositoryName;

            this.setText("Clone Repository");
            this.setToolTipText("Create a secure repository to store and share your code.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_EXPORT));
        }

        @Override
        protected void doRun() {
            try {
                WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(),
                        new CloneRepositoryWizard(accountId, regionId, repositoryName));
                dialog.open();
                actionSucceeded();
            } catch (Exception e) {
                CodeCommitPlugin.getDefault().reportException("Failed to clone CodeCommit repository!",
                        new AwsActionException(AwsToolkitMetricType.EXPLORER_CODECOMMIT_CLONE_REPO.getName(), e.getMessage(), e));
                actionFailed();
            } finally {
                actionFinished();
            }
        }
    }

    private static class DeleteRepositoryAction extends AwsAction {
        private final RepositoryNameIdPair repository;

        public DeleteRepositoryAction(RepositoryNameIdPair repository) {
            super(AwsToolkitMetricType.EXPLORER_CODECOMMIT_DELETE_REPO);
            this.repository = repository;

            this.setText("Delete Repository");
            this.setToolTipText("Deleting this repository from AWS CodeCommit will remove the remote repository for all users.");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void doRun() {
            Dialog dialog = new DeleteResourceConfirmationDialog(Display.getDefault().getActiveShell(), repository.getRepositoryName(), "repository");
            if (dialog.open() != Window.OK) {
                actionCanceled();
                actionFinished();
                return;
            }

            Job deleteRepositoriesJob = new Job("Deleting Repository...") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    AWSCodeCommit codecommit = AwsToolkitCore.getClientFactory().getCodeCommitClient();

                    IStatus status = Status.OK_STATUS;

                    try {
                        codecommit.deleteRepository(new DeleteRepositoryRequest().withRepositoryName(repository.getRepositoryName()));
                        actionSucceeded();
                    } catch (Exception e) {
                        status = new Status(IStatus.ERROR, CodeCommitPlugin.getDefault().getPluginId(), e.getMessage(), e);
                        actionFailed();
                    } finally {
                        actionFinished();
                    }

                    ContentProviderRegistry.refreshAllContentProviders();

                    return status;
                }
            };

            deleteRepositoriesJob.schedule();
        }
    }

    public static class OpenRepositoryEditorAction extends AwsAction {
        private final RepositoryNameIdPair repository;

        public OpenRepositoryEditorAction(RepositoryNameIdPair repository) {
            super(AwsToolkitMetricType.EXPLORER_CODECOMMIT_OPEN_REPO_EDITOR);
            this.repository = repository;

            this.setText("Open in CodeCommit Repository Editor");
        }
        @Override
        public void doRun() {
            String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.CODECOMMIT);
            String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
            String regionId = RegionUtils.getCurrentRegion().getId();

            final IEditorInput input = new RepositoryEditorInput(repository, endpoint, accountId, regionId);

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        activeWindow.getActivePage().openEditor(input, RepositoryEditor.ID);
                        actionSucceeded();
                    } catch (PartInitException e) {
                        actionFailed();
                        CodeCommitPlugin.getDefault().logError("Unable to open the AWS CodeCommit Repository Editor.", e);
                    } finally {
                        actionFinished();
                    }
                }
            });
        }
    }

}
