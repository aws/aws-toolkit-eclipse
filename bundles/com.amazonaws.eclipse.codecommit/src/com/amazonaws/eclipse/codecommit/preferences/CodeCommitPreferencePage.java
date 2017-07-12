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
package com.amazonaws.eclipse.codecommit.preferences;

import static com.amazonaws.eclipse.codecommit.model.CodeCommitPreferencePageDataModel.P_PROFILE;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.codecommit.credentials.GitCredentialsManager;
import com.amazonaws.eclipse.codecommit.model.CodeCommitPreferencePageDataModel;
import com.amazonaws.eclipse.codecommit.widgets.GitCredentialsComposite;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.validator.NoopValidator;

public class CodeCommitPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    public static final String ID = "com.amazonaws.eclipse.codecommit.preferences.CodeCommitPreferencePage";

    private final CodeCommitPreferencePageDataModel dataModel = new CodeCommitPreferencePageDataModel();
    private final DataBindingContext dataBindingContext = new DataBindingContext();

    private Combo profileCombo;
    private GitCredentialsComposite gitCredentialsComposite;
    private FileFieldEditor gitCredentailsFileLocation;

    public CodeCommitPreferencePage() {
        super("AWS CodeCommit Preferences");
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(CodeCommitPlugin.getDefault().getPreferenceStore());
        initDataModel();
    }

    @Override
    protected Control createContents(Composite parent) {
        final Composite composite = new Composite(parent, SWT.LEFT);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createGitCredentialsSection(composite);
        createGitCredentialsFileSection(composite);
        return composite;
    }

    private void createGitCredentialsSection(Composite composite) {
        Group gitCredentialsGroup = newGroup("Configure Git Credentials:", composite);
        gitCredentialsGroup.setLayout(new GridLayout(1, false));
        createProfileComboBoxSection(gitCredentialsGroup);
        gitCredentialsComposite = new GitCredentialsComposite(
                gitCredentialsGroup, dataBindingContext, dataModel.getGitCredentialsDataModel(),
            new NoopValidator(), new NoopValidator());
    }

    private void createProfileComboBoxSection(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        WizardWidgetFactory.newLabel(composite, "Profile: ");
        profileCombo = WizardWidgetFactory.newCombo(composite);
        Map<String, GitCredential> credentials = GitCredentialsManager.getGitCredentials();
        for (Entry<String, GitCredential> entry : credentials.entrySet()) {
            profileCombo.add(entry.getKey());
            profileCombo.setData(entry.getKey(), entry.getValue());
        }
        String defaultAccountName = dataModel.getProfile();
        profileCombo.select(profileCombo.indexOf(defaultAccountName));

        dataBindingContext.bindValue(SWTObservables.observeText(profileCombo),
                PojoObservables.observeValue(dataModel, P_PROFILE));
        profileCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                populateGitCredentialsComposite();
            }
        });
    }

    private void createGitCredentialsFileSection(Composite composite) {
        Group gitCredentialsFileGroup = newGroup("Configure Git Credentials File Path:", composite);
        gitCredentialsFileGroup.setLayout(new GridLayout(1, false));

        gitCredentailsFileLocation = new FileFieldEditor(
                PreferenceConstants.GIT_CREDENTIALS_FILE_PREFERENCE_NAME,
                "Git Credentials file:",
                true,
                gitCredentialsFileGroup);
        gitCredentailsFileLocation.setPage(this);
        gitCredentailsFileLocation.setPreferenceStore(getPreferenceStore());
        gitCredentailsFileLocation.load();
    }

    private void initDataModel() {
        String profileName = AwsToolkitCore.getDefault().getAccountInfo().getAccountName();
        GitCredential credential = GitCredentialsManager.getGitCredential(profileName);
        dataModel.setProfile(profileName);
        dataModel.getGitCredentialsDataModel().setUsername(credential.getUsername());
        dataModel.getGitCredentialsDataModel().setPassword(credential.getPassword());
    }

    private void populateGitCredentialsComposite() {
        String profile = dataModel.getProfile();
        GitCredential selectedGitCredential = GitCredentialsManager.getGitCredential(profile);
        if (selectedGitCredential != null) {
            gitCredentialsComposite.populateGitCredential(
                    selectedGitCredential.getUsername(), selectedGitCredential.getPassword());
        } else {
            gitCredentialsComposite.populateGitCredential(
                    "", "");
        }
        String userAccount = AwsToolkitCore.getDefault().getAccountManager().getAllAccountIds().get(profile);
        dataModel.getGitCredentialsDataModel().setUserAccount(userAccount);
        dataModel.getGitCredentialsDataModel().setRegionId(RegionUtils.getCurrentRegion().getId());
    }

    @Override
    protected void performDefaults() {
        if (gitCredentailsFileLocation != null) {
            gitCredentailsFileLocation.loadDefault();
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        onApplyButton();
        return super.performOk();
    }

    @Override
    public void performApply() {
        onApplyButton();
        super.performApply();
    }

    private void onApplyButton() {
        String previousLocation = getPreferenceStore().getString(PreferenceConstants.GIT_CREDENTIALS_FILE_PREFERENCE_NAME);
        String currentLocation = previousLocation;
        if (gitCredentailsFileLocation != null) {
            gitCredentailsFileLocation.store();
            currentLocation = gitCredentailsFileLocation.getStringValue();
        }
        if (previousLocation.equalsIgnoreCase(currentLocation)) {
            GitCredentialsManager.getGitCredentials().put(dataModel.getProfile(),
                    new GitCredential(dataModel.getGitCredentialsDataModel().getUsername(),
                            dataModel.getGitCredentialsDataModel().getPassword()));
            GitCredentialsManager.saveGitCredentials();
        } else {
            GitCredentialsManager.loadGitCredentials();
            populateGitCredentialsComposite();
        }
    }
}
