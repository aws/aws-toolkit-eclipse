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

import static com.amazonaws.eclipse.codecommit.CodeCommitUtil.nonNullString;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.codecommit.CodeCommitUtil;
import com.amazonaws.eclipse.codecommit.explorer.CodeCommitActionProvider.CloneRepositoryAction;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.model.Commit;
import com.amazonaws.services.codecommit.model.GetBranchRequest;
import com.amazonaws.services.codecommit.model.GetCommitRequest;
import com.amazonaws.services.codecommit.model.GetRepositoryRequest;
import com.amazonaws.services.codecommit.model.ListBranchesRequest;
import com.amazonaws.services.codecommit.model.RepositoryMetadata;
import com.amazonaws.util.StringUtils;

public class RepositoryEditor extends EditorPart {

    public final static String ID = "com.amazonaws.eclipse.codecommit.explorer.RepositoryEditor";

    private static final int DEFAUT_COMMIT_HISTORY_COUNT = 10;
    private RepositoryEditorInput repositoryEditorInput;
    private AWSCodeCommit client;

    private Text lastModifiedDateText;
    private Text repositoryDescriptionText;
    private Text cloneUrlHttpText;
    private Text cloneUrlSSHText;

    private Combo branchCombo;
    private TreeViewer viewer;

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        setSite(site);
        setInput(input);
        repositoryEditorInput = (RepositoryEditorInput) input;
        client = AwsToolkitCore.getClientFactory(repositoryEditorInput.getAccountId())
                .getCodeCommitClientByEndpoint(repositoryEditorInput.getRegionEndpoint());
        setPartName(input.getName());
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL);
        toolkit.decorateFormHeading(form.getForm());

        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());
        form.setText(repositoryEditorInput.getName());
        form.setImage(CodeCommitPlugin.getDefault().getImageRegistry().get(CodeCommitPlugin.IMG_REPOSITORY));
        form.getBody().setLayout(new GridLayout());

        createRepositorySummary(form.getBody(), toolkit);
        createCommitHistory(form.getBody(), toolkit);

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().update(true);
    }

    private void createRepositorySummary(Composite parent, FormToolkit toolkit) {
        final String repositoryName = repositoryEditorInput.getRepository().getRepositoryName();
        final Composite composite = toolkit.createComposite(parent);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        toolkit.createLabel(composite, "Last Modified Date: ");
        lastModifiedDateText = toolkit.createText(composite, "", SWT.READ_ONLY);
        lastModifiedDateText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        toolkit.createLabel(composite, "Repository Description: ");
        repositoryDescriptionText = toolkit.createText(composite, "", SWT.READ_ONLY | SWT.H_SCROLL | SWT.FLAT);
        repositoryDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        toolkit.createLabel(composite, "Clone URL Https: ");
        cloneUrlHttpText = toolkit.createText(composite, "", SWT.READ_ONLY);
        cloneUrlHttpText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        toolkit.createLabel(composite, "Clone URL SSH: ");
        cloneUrlSSHText = toolkit.createText(composite, "", SWT.READ_ONLY);
        cloneUrlSSHText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Button checkoutButton = toolkit.createButton(composite, "Check out", SWT.PUSH);
        checkoutButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new CloneRepositoryAction(repositoryEditorInput).run();
            }
        });

        new LoadSummaryDataThread().start();
    }

    private void createCommitHistory(Composite parent, FormToolkit toolkit) {
        Composite commitHistoryComposite = toolkit.createComposite(parent);
        commitHistoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        commitHistoryComposite.setLayout(new GridLayout());

        Composite headerComposite = toolkit.createComposite(commitHistoryComposite);
        headerComposite.setLayout(new GridLayout(2, true));
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Label label = toolkit.createLabel(headerComposite, "Commit History for Branch:");
        label.setFont(JFaceResources.getHeaderFont());
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        ComboViewer branchComboViewer = new ComboViewer(headerComposite, SWT.READ_ONLY | SWT.DROP_DOWN);
        branchCombo = branchComboViewer.getCombo();
        branchCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        branchCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onBranchSelected();
            }
        });

        Composite composite = toolkit.createComposite(commitHistoryComposite);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        CommitContentProvider contentProvider = new CommitContentProvider();
        CommitLabelProvider labelProvider = new CommitLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());
        viewer.setInput(new Object());

        new LoadBranchesThread().start();
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Commit ID");
        createColumn(tree, columnLayout, "Message");
        createColumn(tree, columnLayout, "Committer");
        createColumn(tree, columnLayout, "Date");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));

        return column;
    }

    @Override
    public void setFocus() {}

    private class LoadSummaryDataThread extends Thread {
        @Override
        public void run() {

            final String repositoryName = repositoryEditorInput.getName();
            final RepositoryMetadata metadata = client.getRepository(
                            new GetRepositoryRequest()
                                    .withRepositoryName(repositoryName))
                    .getRepositoryMetadata();

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    lastModifiedDateText.setText(metadata.getLastModifiedDate().toString());
                    repositoryDescriptionText.setText(nonNullString(metadata.getRepositoryDescription()));
                    cloneUrlHttpText.setText(metadata.getCloneUrlHttp());
                    cloneUrlSSHText.setText(metadata.getCloneUrlSsh());
                }
            });
        }
    }

    private class LoadBranchesThread extends Thread {
        @Override
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    branchCombo.removeAll();
                    List<String> branches = client.listBranches(new ListBranchesRequest()
                            .withRepositoryName(repositoryEditorInput.getRepository().getRepositoryName()))
                        .getBranches();
                    if (!branches.isEmpty()) {
                        for (String branch : branches) {
                            branchCombo.add(branch);
                        }
                        String defautBranch = client.getRepository(new GetRepositoryRequest()
                                .withRepositoryName(repositoryEditorInput.getRepository().getRepositoryName()))
                                .getRepositoryMetadata().getDefaultBranch();
                        branchCombo.select(branchCombo.indexOf(defautBranch));
                    }
                    new LoadCommitHistoryThread().start();
                }
            });
        }
    }

    private class LoadCommitHistoryThread extends Thread {
        @Override
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    String repositoryName = repositoryEditorInput.getRepository().getRepositoryName();
                    String currentBranch = branchCombo.getText();
                    List<CommitRow> row = new ArrayList<>();
                    if (!StringUtils.isNullOrEmpty(currentBranch)) {
                        String commitId = client.getBranch(new GetBranchRequest()
                                .withRepositoryName(repositoryName)
                                .withBranchName(currentBranch))
                                .getBranch().getCommitId();

                        int count = DEFAUT_COMMIT_HISTORY_COUNT;
                        while (count-- > 0 && commitId != null) {
                            Commit commit = client.getCommit(new GetCommitRequest()
                                    .withRepositoryName(repositoryName)
                                    .withCommitId(commitId))
                                    .getCommit();
                            row.add(new CommitRow(commitId, commit));
                            List<String> parent = commit.getParents();
                            commitId = parent != null && !parent.isEmpty() ? parent.get(0) : null;
                        }
                    }
                    viewer.setInput(row);
                }
            });

        }
    }

    private class RefreshAction extends AwsAction {
        public RefreshAction() {
            super(AwsToolkitMetricType.EXPLORER_CODECOMMIT_REFRESH_REPO_EDITOR);
            this.setText("Refresh");
            this.setToolTipText("Refresh CodeCommit Repository");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(
                    AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void doRun() {
            new LoadSummaryDataThread().start();
            new LoadBranchesThread().start();
            actionFinished();
        }
    }

    private final class CommitContentProvider implements ITreePathContentProvider {

        private CommitRow[] commits;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof List) {
                commits = ((List<CommitRow>)newInput).toArray(new CommitRow[0]);
            } else {
                commits = new CommitRow[0];
            }
        }

        @Override
        public void dispose() {

        }

        @Override
        public Object[] getChildren(TreePath arg0) {
            return null;
        }

        @Override
        public Object[] getElements(Object arg0) {
            return commits;
        }

        @Override
        public TreePath[] getParents(Object arg0) {
            return null;
        }

        @Override
        public boolean hasChildren(TreePath arg0) {
            return false;
        }
    }

    private final class CommitLabelProvider implements ITableLabelProvider {

        @Override
        public void addListener(ILabelProviderListener arg0) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object arg0, String arg1) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener arg0) {

        }

        @Override
        public Image getColumnImage(Object arg0, int arg1) {
            return null;
        }

        @Override
        public String getColumnText(Object obj, int column) {
            if (obj instanceof CommitRow == false) return "";

            CommitRow message = (CommitRow) obj;
            switch (column) {
                case 0: return message.getCommitId();
                case 1: return message.getCommit().getMessage();
                case 2: return message.getCommit().getCommitter().getName();
                case 3: return CodeCommitUtil.codeCommitTimeToHumanReadible(message.getCommit().getCommitter().getDate());
            }

            return "";
        }

    }

    // POJO class acting as the row data model for the commit history table.
    private final class CommitRow {
        private final String commitId;
        private final Commit commit;

        public CommitRow(String commitId, Commit commit) {
            this.commitId = commitId;
            this.commit = commit;
        }

        public String getCommitId() {
            return commitId;
        }
        public Commit getCommit() {
            return commit;
        }
    }

    private void onBranchSelected() {
        new LoadCommitHistoryThread().start();
    }
}
