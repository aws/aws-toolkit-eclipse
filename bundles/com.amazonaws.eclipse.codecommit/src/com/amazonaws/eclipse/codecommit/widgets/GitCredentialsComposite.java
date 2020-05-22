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
import java.util.HashSet;
import java.util.Set;

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
import com.amazonaws.eclipse.codecommit.CodeCommitConstants;
import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.GitCredentialsDataModel;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.widget.CheckboxComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateServiceSpecificCredentialRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.LimitExceededException;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.ServiceSpecificCredential;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.util.StringUtils;

/**
 * A complex composite for configuring Git credentials.
 */
public class GitCredentialsComposite extends Composite {
    private static final String AUTO_CREATE_GIT_CREDENTIALS_TILTE = "Auto-create Git credentials";
    private static final String IAM_USER_PREFIX = "EclipseToolkit-CodeCommitUser";
    private static final String GIT_CREDENTIALS_DOC =
            "http://docs.aws.amazon.com/codecommit/latest/userguide/setting-up-gc.html#setting-up-gc-iam";
    private static final String CREATE_SERVICE_SPECIFIC_CREDENTIALS_DOC =
            "http://docs.aws.amazon.com/IAM/latest/APIReference/API_CreateServiceSpecificCredential.html";

    private final DataBindingContext dataBindingContext;
    private final GitCredentialsDataModel dataModel;

    private TextComplex usernameComplex;
    private TextComplex passwordComplex;
    private CheckboxComplex showPasswordComplex;
    private Button browseButton;
    private Button createGitCredentialsButton;

    private IValidator usernameValidator;
    private IValidator passwordValidator;

    public GitCredentialsComposite(Composite parent, DataBindingContext dataBindingContext, GitCredentialsDataModel dataModel) {
        this(parent, dataBindingContext, dataModel, null, null);
    }

    public GitCredentialsComposite(Composite parent, DataBindingContext dataBindingContext, GitCredentialsDataModel dataModel,
            IValidator usernameValidator, IValidator passwordValidator) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_BOTH));
        this.dataModel = dataModel;
        this.dataBindingContext = dataBindingContext;
        this.usernameValidator = usernameValidator;
        this.passwordValidator = passwordValidator;
        createControl();
    }

    public void populateGitCredential(String username, String password) {
        usernameComplex.setText(username);
        passwordComplex.setText(password);
    }

    private void createControl() {
        createUsernamePasswordSection();
        createButtonCompositeSection();
        onShowPasswordCheckboxSelection();
    }

    private void createUsernamePasswordSection() {
        newLink(this, new WebLinkListener(), String.format(
                "You can manually copy and paste Git credentials for AWS CodeCommit below. "
                + "Alternately, you can import them from a downloaded .csv file. To learn how to generate Git credentials, see "
                + "<a href=\"%s\">Create Git Credentials for HTTPS Connections to AWS CodeCommit</a>. You can also authorize the AWS "
                + "Toolkit for Eclipse to create a new set of Git credentials under the current selected account. see "
                + "<a href=\"%s\">CreateServiceSpecificCredential</a> for more information.", GIT_CREDENTIALS_DOC, CREATE_SERVICE_SPECIFIC_CREDENTIALS_DOC), 2);

        usernameComplex = TextComplex.builder(this, dataBindingContext, PojoObservables.observeValue(dataModel, P_USERNAME))
                .addValidator(usernameValidator == null ? new NotEmptyValidator("User name must be provided!") : usernameValidator)
                .labelValue("User name:")
                .defaultValue(dataModel.getUsername())
                .build();

        passwordComplex = TextComplex.builder(this, dataBindingContext, PojoObservables.observeValue(dataModel, P_PASSWORD))
                .addValidator(passwordValidator == null ? new NotEmptyValidator("Password must be provided!") : passwordValidator)
                .labelValue("Password: ")
                .defaultValue(dataModel.getPassword())
                .build();
    }

    private void createButtonCompositeSection() {
        Composite buttonComposite = new Composite(this, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(3, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 2;
        buttonComposite.setLayoutData(gridData);

        showPasswordComplex = CheckboxComplex.builder()
                .composite(buttonComposite)
                .dataBindingContext(dataBindingContext)
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

        browseButton = newPushButton(buttonComposite, "Import from csv file");
        browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
                String path = dialog.open();
                if (path == null) {
                    return;
                }
                GitCredential gitCredential = loadGitCredential(new File(path));
                populateGitCredential(gitCredential.getUsername(), gitCredential.getPassword());
            }
        });

        createGitCredentialsButton = newPushButton(buttonComposite, "Create Git credentials");
        createGitCredentialsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        createGitCredentialsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AmazonIdentityManagement iam = dataModel.getIamClient();
                String profileName = AwsToolkitCore.getDefault().getAccountManager().getAccountInfo(dataModel.getUserAccount()).getAccountName();
                try {
                    String userName = iam.getUser().getUser().getUserName();
                    if (StringUtils.isNullOrEmpty(userName)) {
                        if (!MessageDialog.openConfirm(getShell(), AUTO_CREATE_GIT_CREDENTIALS_TILTE,
                                String.format("Your profile is using root AWS credentials. AWS CodeCommit requires specific CodeCommit credentials from an IAM user. "
                                + "The toolkit can create an IAM user with CodeCommit credentials and associate the credentials with the %s Toolkit profile.\n\n"
                                + "Proceed to try and create an IAM user with credentials and associate with the %s Toolkit profile?", profileName, profileName))) {
                            return;
                        }
                        userName = createNewIamUser().getUserName();
                    }

                    ServiceSpecificCredential credential = iam.createServiceSpecificCredential(new CreateServiceSpecificCredentialRequest()
                            .withUserName(userName).withServiceName(CodeCommitConstants.CODECOMMIT_SERVICE_NAME))
                            .getServiceSpecificCredential();
                    populateGitCredential(credential.getServiceUserName(), credential.getServicePassword());
                } catch (LimitExceededException lee) {
                    MessageDialog.openWarning(getShell(), AUTO_CREATE_GIT_CREDENTIALS_TILTE, "You may already have created the maximum number of sets of Git credentials for AWS CodeCommit(two). "
                            + "Log into the AWS Management Console to download the credentials or obtain them from your administrator, and then import to the toolkit.");
                } catch (Exception ex) {
                    MessageDialog.openError(getShell(), AUTO_CREATE_GIT_CREDENTIALS_TILTE, "Error: " + ex.getMessage());
                }
            }
        });
    }

    // Create a new IAM user attched with the default policy, AWSCodeCommitPowerUser
    private User createNewIamUser() {
        AmazonIdentityManagement iam = dataModel.getIamClient();
        Set<String> userSet = new HashSet<>();
        ListUsersResult result = new ListUsersResult();
        do {
            result = iam.listUsers(new ListUsersRequest().withMarker(result.getMarker()));
            for (User user : result.getUsers()) {
                userSet.add(user.getUserName());
            }
        } while (result.isTruncated());

        String newIamUserName = IAM_USER_PREFIX;
        for (int i = 1; userSet.contains(newIamUserName); ++i) {
            newIamUserName = IAM_USER_PREFIX + "-" + i;
        }
        User newUser = iam.createUser(new CreateUserRequest().withUserName(newIamUserName)).getUser();
        iam.attachUserPolicy(new AttachUserPolicyRequest()
                .withUserName(newIamUserName)
                .withPolicyArn("arn:aws:iam::aws:policy/AWSCodeCommitPowerUser"));
        return newUser;
    }

    private void onShowPasswordCheckboxSelection() {
        passwordComplex.getText().setEchoChar(showPasswordComplex.getCheckbox().getSelection() ? '\0' : '*');
    }

    private GitCredential loadGitCredential(File csvFile) {
        GitCredential gitCredential = new GitCredential("", "");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile))) {
            String line = bufferedReader.readLine();    // the first line of the default csv file is metadata
            if (line == null) {
                throw new ParseException("The csv file is empty", 1);
            }
            line = bufferedReader.readLine();    // the second line of the default csv file contains the credentials separated with ','
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
        }
        return gitCredential;
    }
}
