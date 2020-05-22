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
package com.amazonaws.eclipse.codecommit.credentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.preference.IPreferenceStore;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.codecommit.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.AccountInfoChangeListener;
import com.amazonaws.util.StringUtils;

/**
 * Git credentials manager that manages Git credentials in between memory and disk.
 */
public class GitCredentialsManager {
    private static final Map<String, GitCredential> GIT_CREDENTIALS = new HashMap<>();

    public static void init() {
        AwsToolkitCore.getDefault().getAccountManager().addAccountInfoChangeListener(
                AccountInfoChangeListenerForGitCredentials.INSTANCE);
        loadGitCredentials();
        mergeAwsProfiles();
    }

    /**
     * Load Git credentials from the Git credentials file and do a merge to the current credentials.
     */
    public static void loadGitCredentials() {
        File gitCredentialsFile = getGitCredentialsFile();
        if (gitCredentialsFile.exists() && gitCredentialsFile.isFile()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(gitCredentialsFile))) {
                String line;
                int lineNumber = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    ++lineNumber;
                    List<String> tokens = splitStringByComma(line);
                    if (tokens.size() != 3) {
                        CodeCommitPlugin.getDefault().logWarning("Invalid csv file: " + gitCredentialsFile.getAbsolutePath(),
                                new ParseException("The csv file must have three columns!", lineNumber));
                    } else {
                        GIT_CREDENTIALS.put(tokens.get(0).trim(), new GitCredential(tokens.get(1).trim(), tokens.get(2).trim()));
                    }
                }
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Failed to load gitCredentials file for git credentials!", e);
            }
        }
    }

    /**
     * Merge AWS account profiles to Git credentials manager.
     */
    private static void mergeAwsProfiles() {
        Map<String, AccountInfo> accounts = AwsToolkitCore.getDefault().getAccountManager().getAllAccountInfo();
        for (Entry<String, AccountInfo> entry : accounts.entrySet()) {
            String accountName = entry.getValue().getAccountName();
            if (GIT_CREDENTIALS.containsKey(accountName)) continue;
            GIT_CREDENTIALS.put(accountName, new GitCredential("", ""));
        }
    }

    public static Map<String, GitCredential> getGitCredentials() {
        return GIT_CREDENTIALS;
    }

    public static GitCredential getGitCredential(String profile) {
        return GIT_CREDENTIALS.get(profile);
    }

    private static File getGitCredentialsFile() {
        IPreferenceStore store = CodeCommitPlugin.getDefault().getPreferenceStore();
        String filePath = store.getString(PreferenceConstants.GIT_CREDENTIALS_FILE_PREFERENCE_NAME);
        return new File(StringUtils.isNullOrEmpty(filePath) ? PreferenceConstants.DEFAULT_GIT_CREDENTIALS_FILE : filePath);
    }

    public static void saveGitCredentials() {
        File gitCredentialsFile = getGitCredentialsFile();
        if (!gitCredentialsFile.exists()) {
            try {
                gitCredentialsFile.createNewFile();
            } catch (IOException e) {
                AwsToolkitCore.getDefault().reportException("Failed to create gitCredentials file!", e);
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(gitCredentialsFile))) {
            for (Entry<String, GitCredential> entry : GIT_CREDENTIALS.entrySet()) {
                writer.println(String.format("%s,%s,%s",entry.getKey(), entry.getValue().getUsername(), entry.getValue().getPassword()));
            }
            writer.flush();
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logWarning("Failed to write git credential to file!", e);
        }
    }

    /**
     * Listens profile updating event to keep sync up with the Git credentials manager.
     */
    private static class AccountInfoChangeListenerForGitCredentials implements AccountInfoChangeListener {
        public static AccountInfoChangeListenerForGitCredentials INSTANCE = new AccountInfoChangeListenerForGitCredentials();

        private AccountInfoChangeListenerForGitCredentials() {}

        @Override
        public void onAccountInfoChange() {
            mergeAwsProfiles();
        }

    }

    /**
     * The String.split method incorrectly parses "profile,," to {"profile"} instead of {"profile", "", ""}.
     * This helper method correctly parses cvs lines.
     */
    private static List<String> splitStringByComma(String str) {
        List<String> tokens = new ArrayList<>();
        int fromIndex = 0, endIndex = 0;
        while ((endIndex = str.indexOf(',', fromIndex)) > 0) {
            tokens.add(str.substring(fromIndex, endIndex));
            fromIndex = endIndex + 1;
        }
        tokens.add(str.substring(fromIndex));
        return tokens;
    }
}
