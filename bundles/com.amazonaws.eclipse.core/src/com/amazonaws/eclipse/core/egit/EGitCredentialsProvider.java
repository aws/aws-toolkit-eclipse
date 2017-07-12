/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.amazonaws.eclipse.core.egit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements a {@link CredentialsProvider} for EGit. The provider
 * tries to retrieve the credentials (user, password) for a given URI from the
 * secure store. A login popup is shown if no credentials are available.
 */
@SuppressWarnings("restriction")
public class EGitCredentialsProvider extends CredentialsProvider {

    private String user;
    private String password;

    /**
     * Default constructor
     */
    public EGitCredentialsProvider() {
        // empty
    }

    /**
     * @param user
     * @param password
     */
    public EGitCredentialsProvider(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
            if (i instanceof CredentialItem.StringType)
                continue;
            else if (i instanceof CredentialItem.CharArrayType)
                continue;
            else if (i instanceof CredentialItem.YesNoType)
                continue;
            else if (i instanceof CredentialItem.InformationalMessage)
                continue;
            else
                return false;
        }
        return true;
    }

    @Override
    public boolean get(final URIish uri, final CredentialItem... items)
            throws UnsupportedCredentialItem {

        if (items.length == 0) {
            return true;
        }

        CredentialItem.Username userItem = null;
        CredentialItem.Password passwordItem = null;
        boolean isSpecial = false;

        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.Username)
                userItem = (CredentialItem.Username) item;
            else if (item instanceof CredentialItem.Password)
                passwordItem = (CredentialItem.Password) item;
            else
                isSpecial = true;
        }

        if (!isSpecial && (userItem != null || passwordItem != null)) {
            UserPasswordCredentials credentials = null;
            if ((user != null) && (password != null))
                credentials = new UserPasswordCredentials(user, password);
            else
                credentials = SecureStoreUtils.getCredentialsQuietly(uri);

            if (credentials == null) {
                credentials = getCredentialsFromUser(uri);
                if (credentials == null)
                    return false;
            }
            if (userItem != null)
                userItem.setValue(credentials.getUser());
            if (passwordItem != null)
                passwordItem.setValue(credentials.getPassword().toCharArray());
            return true;
        }

        // special handling for non-user,non-password type items
        final boolean[] result = new boolean[1];

        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();

                if (items.length == 1) {
                    CredentialItem item = items[0];
                    result[0] = getSingleSpecial(shell, uri, item);
                } else {
                    result[0] = getMultiSpecial(shell, uri, items);
                }
            }
        });

        return result[0];
    }

    @Override
    public void reset(URIish uri) {
        try {
            Activator.getDefault().getSecureStore().clearCredentials(uri);
            user = null;
            password = null;
        } catch (IOException e) {
            Activator.logError(MessageFormat.format(
                    UIText.EGitCredentialsProvider_FailedToClearCredentials,
                    uri), e);
        }
    }

    /**
     * Opens a dialog for a single non-user, non-password type item.
     * @param shell the shell to use
     * @param uri the uri of the get request
     * @param item the item to handle
     * @return <code>true</code> if the request was successful and values were supplied;
     *         <code>false</code> if the user canceled the request and did not supply all requested values.
     */
    private boolean getSingleSpecial(Shell shell, URIish uri, CredentialItem item) {
        if (item instanceof CredentialItem.InformationalMessage) {
            MessageDialog.openInformation(shell, UIText.EGitCredentialsProvider_information, item.getPromptText());
            return true;
        } else if (item instanceof CredentialItem.YesNoType) {
            CredentialItem.YesNoType v = (CredentialItem.YesNoType) item;
            String[] labels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
            int[] resultIDs = new int[] { IDialogConstants.YES_ID, IDialogConstants.NO_ID, IDialogConstants.CANCEL_ID };

            MessageDialog dialog = new MessageDialog(
                    shell,
                    UIText.EGitCredentialsProvider_question,
                    null,
                    item.getPromptText(),
                    MessageDialog.QUESTION_WITH_CANCEL,
                    labels,
                    0);
            dialog.setBlockOnOpen(true);
            int r = dialog.open();
            if (r < 0) {
                return false;
            }

            switch (resultIDs[r]) {
            case IDialogConstants.YES_ID: {
                v.setValue(true);
                return true;
            }
            case IDialogConstants.NO_ID: {
                v.setValue(false);
                return true;
            }
            default:
                // abort
                return false;
            }
        } else {
            // generically handles all other types of items
            return getMultiSpecial(shell, uri, item);
        }
    }

    /**
     * Opens a generic dialog presenting all CredentialItems to the user.
     * @param shell the shell to use
     * @param uri the uri of the get request
     * @param items the items to handle
     * @return <code>true</code> if the request was successful and values were supplied;
     *         <code>false</code> if the user canceled the request and did not supply all requested values.
     */
    private boolean getMultiSpecial(Shell shell, URIish uri, CredentialItem... items) {
        CustomPromptDialog dialog = new CustomPromptDialog(shell, uri, UIText.EGitCredentialsProvider_information, items);
        dialog.setBlockOnOpen(true);
        int r = dialog.open();
        if (r == Window.OK) {
            return true;
        }
        return false;
    }

    private UserPasswordCredentials getCredentialsFromUser(final URIish uri) {
        final AtomicReference<UserPasswordCredentials> aRef = new AtomicReference<>(
                null);
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                aRef.set(LoginService.login(shell, uri));
            }
        });
        return aRef.get();
    }
}