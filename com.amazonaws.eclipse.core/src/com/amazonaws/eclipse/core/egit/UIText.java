/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, 2013 Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2011, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Ben Hammen <hammenb@gmail.com>
 * Copyright (C) 2014, Marc Khouzam <marc.khouzam@ericsson.com>
 * Copyright (C) 2014, Red Hat Inc.
 * Copyright (C) 2014, Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2015, SAP SE (Christian Georgi <christian.georgi@sap.com>)
 * Copyright (C) 2015, Jan-Ove Weichel <ovi.weichel@gmail.com>
 * Copyright (C) 2015, Laurent Delaigue <laurent.delaigue@obeo.fr>
 * Copyright (C) 2015, Denis Zygann <d.zygann@web.de>
 * Copyright (C) 2016, Lars Vogel <Lars.Vogel@vogella.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.amazonaws.eclipse.core.egit;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid
 * duplicating strings.
 */
public class UIText {
    /** */
    public static String SourceBranchPage_repoEmpty = "Source Git repository is empty";

    /** */
    public static String SourceBranchPage_title = "Branch Selection";

    /** */
    public static String SourceBranchPage_description = "Select branches to clone from remote repository. Remote tracking "
            + "branches will be created to track updates for these branches in the remote repository.";

    /** */
    public static String SourceBranchPage_branchList = "Branches of {0}";

    /** */
    public static String SourceBranchPage_selectAll = "Select All";

    /** */
    public static String SourceBranchPage_selectNone = "Deselect All";

    /** */
    public static String SourceBranchPage_errorBranchRequired = "At least one branch must be selected.";

    /** */
    public static String SourceBranchPage_remoteListingCancelled = "Operation canceled";

    /** */
    public static String SourceBranchPage_cannotCreateTemp = "Couldn't create temporary repository.";

    /** */
    public static String SourceBranchPage_CompositeTransportErrorMessage = "{0}:\n{1}";

    /** */
    public static String SourceBranchPage_AuthFailMessage = "{0}:\nInvalid password or missing SSH key.";

    /** */
    public static String CloneDestinationPage_title = "Local Destination";

    /** */
    public static String CloneDestinationPage_description = "Configure the local storage location for {0}.";

    /** */
    public static String CloneDestinationPage_groupDestination = "Destination";

    /** */
    public static String CloneDestinationPage_groupConfiguration = "Configuration";

    /** */
    public static String CloneDestinationPage_groupProjects = "Projects";

    /** */
    public static String CloneDestinationPage_promptDirectory = "&Directory";

    /** */
    public static String CloneDestinationPage_promptInitialBranch = "Initial branc&h";

    /** */
    public static String CloneDestinationPage_promptRemoteName = "Remote na&me";

    /** */
    public static String CloneDestinationPage_browseButton = "Bro&wse";

    /** */
    public static String CloneDestinationPage_cloneSubmodulesButton = "Clone &submodules";

    /** */
    public static String CloneDestinationPage_DefaultRepoFolderTooltip = "You can change the default parent folder in the Git preferences";

    /** */
    public static String CloneDestinationPage_errorDirectoryRequired = "Directory is required";

    /** */
    public static String CloneDestinationPage_errorInitialBranchRequired = "Initial branch is required";

    /** */
    public static String CloneDestinationPage_errorInvalidRemoteName = "Invalid remote name ''{0}''";

    /** */
    public static String CloneDestinationPage_errorNotEmptyDir = "{0} is not an empty directory.";

    /** */
    public static String CloneDestinationPage_errorRemoteNameRequired = "Remote name is required";

    /** */
    public static String CloneDestinationPage_importButton = "&Import all existing Eclipse projects after clone finishes";

    /** */
    public static String GitCreateProjectViaWizardWizard_AbortedMessage = "Action was aborted";

    /** */
    public static String GitImportWizard_errorParsingURI = "The URI of the repository to be cloned can't be parsed";

    /** */
    public static String CustomPromptDialog_provide_information_for = "Provide information for {0}";

    /** */
    public static String CustomPromptDialog_information_about = "Information about {0}";

    public static String EGitCredentialsProvider_FailedToClearCredentials = "Failed to clear credentials for {0} stored in secure store";

    public static String EGitCredentialsProvider_question = "Question";

    public static String EGitCredentialsProvider_information = "Information";

    public static String EGitCredentialsProvider_errorReadingCredentials = "Failed reading credentials from secure store";

    public static String LoginDialog_changeCredentials = "Change stored credentials";
    public static String LoginDialog_login = "Login";
    public static String LoginDialog_password = "Password";
    public static String LoginDialog_repository = "Repository";
    public static String LoginDialog_storeInSecureStore = "Store in Secure Store";
    public static String LoginDialog_user = "User";

    public static String SecureStoreUtils_writingCredentialsFailed = "Writing to secure store failed";

    public static String CloneFailureDialog_tile = "Transport Error";
    public static String CloneFailureDialog_dontShowAgain = "Don't show this dialog again";
    public static String CloneFailureDialog_checkList = "An error occurred when trying to contact {0}.\nSee the Error Log for more details\n\nPossible reasons:\nIncorrect URL\nNo network connection (e.g. wrong proxy settings)";
    public static String CloneFailureDialog_checkList_git = "\n.git is missing at end of repository URL";
    public static String CloneFailureDialog_checkList_ssh = "\nSSH is not configured correctly (see Eclipse preferences > Network Connections > SSH2)";
    public static String CloneFailureDialog_checkList_https = "\nSSL host could not be verified (set http.sslVerify=false in Git configuration)";


    public static String GitCloneWizard_failed = "Git repository clone failed.";
    public static String GitCloneWizard_jobName = "Cloning from {0}";
    public static String GitCloneWizard_errorCannotCreate = "Cannot create directory {0}.";
}