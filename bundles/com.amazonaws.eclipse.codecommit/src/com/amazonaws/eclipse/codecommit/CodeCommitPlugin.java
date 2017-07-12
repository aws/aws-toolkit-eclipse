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
package com.amazonaws.eclipse.codecommit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.codecommit.credentials.GitCredential;
import com.amazonaws.eclipse.codecommit.credentials.GitCredentialsManager;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.util.StringUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class CodeCommitPlugin extends AbstractAwsPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.codecommit"; //$NON-NLS-1$

    // The icon IDs
    public static final String IMG_SERVICE = "codecommit-service";
    public static final String IMG_REPOSITORY = "codecommit-repo";

    // The shared instance
    private static CodeCommitPlugin plugin;

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        GitCredentialsManager.init();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static CodeCommitPlugin getDefault() {
        return plugin;
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = new ImageRegistry(Display.getCurrent());
        imageRegistry.put(IMG_SERVICE, ImageDescriptor.createFromFile(getClass(), "/icons/codecommit-service.png"));
        imageRegistry.put(IMG_REPOSITORY, ImageDescriptor.createFromFile(getClass(), "/icons/codecommit-repo.png"));

        return imageRegistry;
    }

    /**
     * Returns a CodeCommit client for the current account and region.
     */
    public static AWSCodeCommit getCurrentCodeCommitClient() {
        String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoint(ServiceAbbreviations.CODECOMMIT);
        return AwsToolkitCore.getClientFactory().getCodeCommitClientByEndpoint(endpoint);
    }

    /**
     * Utility method that returns whether the Git credentials are configured for the current profile.
     */
    public static boolean currentProfileGitCredentialsConfigured() {
        String profile = AwsToolkitCore.getDefault().getAccountInfo().getAccountName();
        GitCredential credential = GitCredentialsManager.getGitCredential(profile);
        return !StringUtils.isNullOrEmpty(credential.getUsername()) &&
                !StringUtils.isNullOrEmpty(credential.getPassword());
    }

}
