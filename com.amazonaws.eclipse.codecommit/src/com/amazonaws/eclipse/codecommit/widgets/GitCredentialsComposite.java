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
package com.amazonaws.eclipse.codecommit.widgets;

import static com.amazonaws.eclipse.core.model.GitCredentialsDataModel.P_PASSWORD;
import static com.amazonaws.eclipse.core.model.GitCredentialsDataModel.P_SHOW_PASSWORD;
import static com.amazonaws.eclipse.core.model.GitCredentialsDataModel.P_USERNAME;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newPushButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.GitCredentialsDataModel;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.widget.CheckboxComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;

/**
 * A complex composite for configuring Git credentials.
 */
public class GitCredentialsComposite extends Composite {
    private static final String GIT_CREDENTIALS_DOC =
            "http://docs.aws.amazon.com/codecommit/latest/userguide/setting-up-gc.html#setting-up-gc-iam";
    private TextComplex usernameComplex;
    private TextComplex passwordComplex;
    private CheckboxComplex showPasswordComplex;
    private Button browseButton;

    public GitCredentialsComposite(Composite parent, DataBindingContext dataBindingContext, GitCredentialsDataModel dataModel) {
        this(parent, dataBindingContext, dataModel, null, null);
    }

    public GitCredentialsComposite(Composite parent, DataBindingContext dataBindingContext, GitCredentialsDataModel dataModel,
            IValidator usernameValidator, IValidator passwordValidator) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_BOTH));
        createControl(dataBindingContext, dataModel, usernameValidator, passwordValidator);
    }

    public void populateGitCredential(String username, String password) {
        usernameComplex.setText(username);
        passwordComplex.setText(password);
    }

    private void createControl(DataBindingContext context, GitCredentialsDataModel dataModel,
            IValidator usernameValidator, IValidator passwordValidator) {

        newLink(this, new WebLinkListener(), String.format(
                "You can manually copy and paste Git credentials for AWS CodeCommit below. "
                + "Alternately, you can import them from a downloaded .csv file. To learn how to generate Git credentials, see "
                + "<a href=\"%s\">Create Git Credentials for HTTPS Connections to AWS CodeCommit</a>.", GIT_CREDENTIALS_DOC), 2);

        usernameComplex = TextComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_USERNAME))
                .validator(usernameValidator == null ? new NotEmptyValidator("User name must be provided!") : usernameValidator)
                .labelValue("User name:")
                .defaultValue(dataModel.getUsername())
                .build();

        passwordComplex = TextComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_PASSWORD))
                .validator(passwordValidator == null ? new NotEmptyValidator("Password must be provided!") : passwordValidator)
                .labelValue("Password: ")
                .defaultValue(dataModel.getPassword())
                .build();

        showPasswordComplex = CheckboxComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_SHOW_PASSWORD))
                .labelValue("Show password")
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onShowPasswordCheckboxSelection();
                    }
                })
                .defaultValue(dataModel.isShowPassword())
                .build();

        browseButton = newPushButton(this, "Import from csv file");
        browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
                String path = dialog.open();
                if (path == null) return;
                GitCredential gitCredential = loadGitCredential(new File(path));
                usernameComplex.setText(gitCredential.getUsername());
                passwordComplex.setText(gitCredential.getPassword());
            }
        });
        onShowPasswordCheckboxSelection();
    }

    private void onShowPasswordCheckboxSelection() {
        passwordComplex.getText().setEchoChar(showPasswordComplex.getCheckbox().getSelection() ? '\0' : '*');
    }

    private GitCredential loadGitCredential(File csvFile) {
        BufferedReader bufferedReader = null;
        GitCredential gitCredential = new GitCredential("", "");
        try {
            bufferedReader = new BufferedReader(new FileReader(csvFile));
            String line = bufferedReader.readLine();    // the first line of the default csv file is metadata
            if (line == null) {
                throw new ParseException("The csv file is empty", 1);
            }
            line = bufferedReader.readLine();	// the second line of the default csv file contains the credentials separated with ','
            if (line == null) {
                throw new ParseException("Invalid Git credential csv file format!", 2);
            }
            String[] tokens = line.split(",");
            if (tokens.length != 2) {
                throw new ParseException(
                        "The csv file must have two columns!", 2);
            }
            gitCredential.setUsername(tokens[0].trim());
            gitCredential.setPassword(tokens[1].trim());
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logWarning("Failed to load gitCredentials file for Git credentials!", e);
            new MessageDialog(getShell(), "Error loading Git credentials!",
                    null, e.getMessage(), MessageDialog.ERROR, new String[] {"OK"}, 0).open();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                }
            }
        }
        return gitCredential;
    }
}
