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
package com.amazonaws.eclipse.codestar.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.codecommit.credentials.GitCredentialsManager;
import com.amazonaws.eclipse.codecommit.widgets.GitCredentialsComposite;
import com.amazonaws.eclipse.codestar.CodeStarPlugin;
import com.amazonaws.eclipse.codestar.CodeStarUtils;
import com.amazonaws.eclipse.codestar.UIConstants;
import com.amazonaws.eclipse.codestar.model.CodeStarProjectCheckoutWizardDataModel;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.core.ui.RegionSelectionComposite;
import com.amazonaws.services.codecommit.model.RepositoryMetadata;
import com.amazonaws.services.codestar.model.DescribeProjectResult;

/**
 * The wizard page used to identify CodeStar project to be imported.
 */
public class CodeStarProjectCheckoutPage extends WizardPage {

    private final CodeStarProjectCheckoutWizardDataModel dataModel;

    private final DataBindingContext dataBindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    private static final String[] CODESTAR_PROJECT_TABLE_TITLE = {"Project Name", "Project ID", "Project Description"};

    private AccountSelectionComposite accountSelectionComposite;
    private RegionSelectionComposite regionSelectionComposite;
    private Table codeStarProjectTable;
    private Combo repositoryCombo;
    private GitCredentialsComposite gitCredentialsComposite;

    private final Map<String, GitCredential> gitCredentials = GitCredentialsManager.getGitCredentials();

    public CodeStarProjectCheckoutPage(final CodeStarProjectCheckoutWizardDataModel dataModel) {
        super(CodeStarProjectCheckoutPage.class.getName());
        setTitle(UIConstants.CODESTAR_PROJECT_CHECKOUT_PAGE_TITLE);
        setDescription(UIConstants.CODESTAR_PROJECT_CHECKOUT_PAGE_DESCRIPTION);
        this.dataModel = dataModel;
        this.dataBindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                populateValidationStatus();
            }
        });
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createAccountAndRegionSelectionComposite(composite);
        createProjectSectionComposite(composite);
        createGitCredentialsComposite(composite);

        initDefaults();

        setControl(composite);
    }

    private void initDefaults() {

        if (!AwsToolkitCore.getDefault().getAccountManager().validAccountsConfigured()) return;

        accountSelectionComposite.selectAccountId(AwsToolkitCore.getDefault().getCurrentAccountId());
        regionSelectionComposite.setSelection(0);

        dataModel.setAccountId(accountSelectionComposite.getSelectedAccountId());
        dataModel.setProjectRegionId(regionSelectionComposite.getSelectedRegion());

        onAccountOrRegionSelectionChange();
    }

    /*
     * Initialize account selection UI and set accountId in the model.
     */
    private void createAccountAndRegionSelectionComposite(Composite parent) {

        Group accountGroup = newGroup(parent, "Select AWS account and region:");
        accountGroup.setLayout(new GridLayout(1, false));

        accountSelectionComposite = new AccountSelectionComposite(accountGroup, SWT.None);
        accountSelectionComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataModel.setAccountId(accountSelectionComposite.getSelectedAccountId());
                onAccountOrRegionSelectionChange();
            }
        });

        regionSelectionComposite = new RegionSelectionComposite(accountGroup, SWT.None, ServiceAbbreviations.CODESTAR);
        regionSelectionComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataModel.setProjectRegionId(regionSelectionComposite.getSelectedRegion());
                onAccountOrRegionSelectionChange();
            }
        });

    }

    private void createProjectSectionComposite(Composite composite) {
        Group projectGroup = newGroup(composite, "Select AWS CodeStar project and repository:");
        projectGroup.setLayout(new GridLayout(2, false));

        codeStarProjectTable = newTable(projectGroup, 2, CODESTAR_PROJECT_TABLE_TITLE);
        codeStarProjectTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onCodeStarProjectTableSelection();
            }
        });
        newLabel(projectGroup, "Select repository: ", 1);
        repositoryCombo = newCombo(projectGroup, 1);
        repositoryCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRepositoryComboSelection();
            }
        });
    }

    private void createGitCredentialsComposite(Composite composite) {
        Group gitCredentialsGroup = newGroup(composite, "Configure Git credentials:");
        gitCredentialsGroup.setLayout(new GridLayout(1, false));
        this.gitCredentialsComposite = new GitCredentialsComposite(
                gitCredentialsGroup, dataBindingContext, dataModel.getGitCredentialsDataModel());
    }

    private void onCodeStarProjectTableSelection() {
        try {
            DescribeProjectResult selection = (DescribeProjectResult)codeStarProjectTable.getItem(codeStarProjectTable.getSelectionIndex()).getData();
            dataModel.setProjectName(selection.getName());
            dataModel.setProjectId(selection.getId());
            List<RepositoryMetadata> repositories = getCodeCommitRepositories();
            repositoryCombo.removeAll();
            for (RepositoryMetadata metadata : repositories) {
                repositoryCombo.add(metadata.getRepositoryName());
                repositoryCombo.setData(metadata.getRepositoryName(), metadata);
            }
            if (!repositories.isEmpty()) {
                repositoryCombo.select(0);
                onRepositoryComboSelection();
            } else {
                CodeStarPlugin.getDefault().logWarning("No CodeCommit repository found for this project.", null);
            }
        } catch (Exception e) {
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
        }
    }

    // dataModel.projectId must be specified before this call.
    private void onRepositoryComboSelection() {
        try {
            String selectedRepo = repositoryCombo.getItem(repositoryCombo.getSelectionIndex());
            RepositoryMetadata metadata = (RepositoryMetadata)repositoryCombo.getData(selectedRepo);
            dataModel.setRepoHttpUrl(metadata.getCloneUrlHttp());
            dataModel.setRepoName(metadata.getRepositoryName());
            populateValidationStatus();
        } catch (Exception e) {
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
        }
    }

    private Table newTable(Composite composite, int colspan, String[] headers) {
        Table table = new Table(composite, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        int columnWeight = 100 / headers.length;
        TableLayout layout = new TableLayout();
        for (int i = 0; i < headers.length; ++i) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(headers[i]);
            layout.addColumnData(new ColumnWeightData(columnWeight));
        }

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 100;
        data.widthHint = 200;
        data.horizontalSpan = colspan;
        table.setLayoutData(data);
        table.setLayout(layout);

        return table;
    }

    private void onAccountOrRegionSelectionChange() {
        try {
            populateCodeStarProjectUI();
        } catch (Exception e) {
            CodeStarPlugin.getDefault().reportException(e.getMessage(), e);
        }
    }

    private void populateCodeStarProjectUI() {

        populateGitCredentialsComposite();
        populateValidationStatus();

        codeStarProjectTable.removeAll();
        repositoryCombo.removeAll();

        for (Entry<String, DescribeProjectResult> project : getCodeStarProjects().entrySet()) {
            TableItem item = new TableItem(codeStarProjectTable, SWT.LEFT);
            item.setText(getTableItem(project.getValue()));
            item.setData(project.getValue());
        }

        dataModel.setProjectName(null);
        dataModel.setRepoHttpUrl(null);
        dataModel.setRepoName(null);
    }

    private void populateGitCredentialsComposite() {
        Map<String, String> accounts = AwsToolkitCore.getDefault().getAccountManager().getAllAccountNames();
        String profileName = accounts.get(dataModel.getAccountId());
        GitCredential selectedGitCredential = gitCredentials.get(profileName);
        if (selectedGitCredential != null) {
            gitCredentialsComposite.populateGitCredential(
                    selectedGitCredential.getUsername(), selectedGitCredential.getPassword());
        } else {
            gitCredentialsComposite.populateGitCredential(
                    "", "");
        }

        dataModel.getGitCredentialsDataModel().setUserAccount(dataModel.getAccountId());
        dataModel.getGitCredentialsDataModel().setRegionId(dataModel.getProjectRegionId());
    }

    private Map<String, DescribeProjectResult> getCodeStarProjects() {
        return CodeStarUtils.getCodeStarProjects(dataModel.getAccountId(), dataModel.getProjectRegionId());
    }

    private List<RepositoryMetadata> getCodeCommitRepositories() {
        return CodeStarUtils.getCodeCommitRepositories(
                dataModel.getAccountId(), dataModel.getProjectRegionId(), dataModel.getProjectId());
    }

    private String[] getTableItem(DescribeProjectResult project) {
        return new String[]{project.getName(), project.getId(), project.getDescription()};
    }

    private void populateValidationStatus() {

        if (!nonBindingDataModelStatusOk()) {
            super.setPageComplete(false);
            return;
        }

        IStatus status = getValidationStatus();

        if (status == null || status.getSeverity() == IStatus.OK) {
            setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            setErrorMessage(status.getMessage());
            super.setPageComplete(false);
        }
    }

    private boolean nonBindingDataModelStatusOk() {
        return dataModel.getAccountId() != null &&
                dataModel.getProjectId() != null &&
                dataModel.getProjectName() != null &&
                dataModel.getProjectRegionId() != null &&
                dataModel.getRepoHttpUrl() != null &&
                dataModel.getRepoName() != null;
    }

    private IStatus getValidationStatus() {
        if (aggregateValidationStatus == null) return null;
        Object value = aggregateValidationStatus.getValue();
        if (!(value instanceof IStatus)) return null;
        return (IStatus)value;
    }
}
