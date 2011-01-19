/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Entry point for functionality provided by the AWS Toolkit Core plugin,
 * including access to AWS account information.
 */
public class AwsToolkitCore extends AbstractUIPlugin {

    /** The singleton instance of this plugin */
    private static AwsToolkitCore plugin;
    
    private static AWSClientFactory clientFactory;

    /** The ID of this plugin */
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.core";

    /** The ID of the main AWS Toolkit preference page */
    public static final String ACCOUNT_PREFERENCE_PAGE_ID =
        "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage";

    /** The ID of the AWS Toolkit Overview extension point */
    public static final String OVERVIEW_EXTENSION_ID = "com.amazonaws.eclipse.core.overview";

    /** The ID of the AWS Toolkit Overview editor */
    public static final String OVERVIEW_EDITOR_ID = "com.amazonaws.eclipse.core.ui.overview";

    /** ImageRegistry ID for the AWS Toolkit title */
    public static final String IMAGE_AWS_TOOLKIT_TITLE = "aws-toolkit-title";

    /** ImageRegistry ID for the external link icon */
    public static final String IMAGE_EXTERNAL_LINK = "external-link";

    /** ImageRegistry ID for the wrench image */
    public static final String IMAGE_WRENCH = "wrench";

    /** ImageRegistry ID for the scroll image */
    public static final String IMAGE_SCROLL = "scroll";

    /** ImageRegistry ID for the gears image */
    public static final String IMAGE_GEARS = "gears";

    /** ImageRegistry ID for the gear image */
    public static final String IMAGE_GEAR = "gear";

    /** ImageRegistry ID for the HTML document image */
    public static final String IMAGE_HTML_DOC = "html";

    /** ImageRegistry ID for the AWS logo image */
    public static final String IMAGE_AWS_LOGO = "logo";

    /** ImageRegistry ID for the AWS logo icon */
    public static final String IMAGE_AWS_ICON = "icon";


    /** OSGI ServiceTracker object for querying details of proxy configuration */
    private ServiceTracker proxyServiceTracker;

    /** Monitors for changes to AWS account information, and notifies listeners */
    private AccountInfoMonitor accountInfoMonitor;


    /**
     * Returns the singleton instance of this plugin.
     *
     * @return The singleton instance of this plugin.
     */
    public static AwsToolkitCore getDefault() {
        return plugin;
    }
    
    /**
     * Returns the IProxyService that allows callers to
     * access information on how the proxy is currently
     * configured.
     *
     * @return An IProxyService object that allows callers to
     *         query proxy configuration information.
     */
    public IProxyService getProxyService() {
        return (IProxyService)proxyServiceTracker.getService();
    }
    
    /**
     * Returns the client factory.
     */
    public static synchronized AWSClientFactory getClientFactory() {
        if (clientFactory == null)
            clientFactory = new AWSClientFactory();
        return clientFactory;       
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        proxyServiceTracker = new ServiceTracker(context, IProxyService.class.getName(), null);
        proxyServiceTracker.open();

        // Start listening for account changes...
        accountInfoMonitor = new AccountInfoMonitor();
        getPreferenceStore().addPropertyChangeListener(accountInfoMonitor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        getPreferenceStore().removePropertyChangeListener(accountInfoMonitor);
        proxyServiceTracker.close();
        super.stop(context);
    }

    /**
     * Returns the user's AWS account info.
     *
     * @return The user's AWS account info.
     */
    public AccountInfo getAccountInfo() {
        return new PluginPreferenceStoreAccountInfo(getPreferenceStore());
    }

    /**
     * Registers a listener to receive notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addAccountInfoChangeListener(AccountInfoChangeListener listener) {
        accountInfoMonitor.addAccountInfoChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when account info is
     * changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeAccountInfoChangeListener(AccountInfoChangeListener listener) {
        accountInfoMonitor.removeAccountInfoChangeListener(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = super.createImageRegistry();

        imageRegistry.put(IMAGE_AWS_LOGO, ImageDescriptor.createFromURL(getBundle().getEntry("icons/logo_aws.png")));
        imageRegistry.put(IMAGE_HTML_DOC, ImageDescriptor.createFromURL(getBundle().getEntry("icons/document_text.png")));
        imageRegistry.put(IMAGE_GEAR, ImageDescriptor.createFromURL(getBundle().getEntry("icons/gear.png")));
        imageRegistry.put(IMAGE_GEARS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/gears.png")));
        imageRegistry.put(IMAGE_SCROLL, ImageDescriptor.createFromURL(getBundle().getEntry("icons/scroll.png")));
        imageRegistry.put(IMAGE_WRENCH, ImageDescriptor.createFromURL(getBundle().getEntry("icons/wrench.png")));
        imageRegistry.put(IMAGE_AWS_TOOLKIT_TITLE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/aws-toolkit-title.png")));
        imageRegistry.put(IMAGE_EXTERNAL_LINK, ImageDescriptor.createFromURL(getBundle().getEntry("icons/icon_offsite.gif")));
        imageRegistry.put(IMAGE_AWS_ICON, ImageDescriptor.createFromURL(getBundle().getEntry("icons/aws-box.gif")));

        return imageRegistry;
    }
}
