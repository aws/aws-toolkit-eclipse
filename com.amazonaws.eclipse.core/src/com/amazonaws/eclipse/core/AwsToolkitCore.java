/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.preferences.PreferencePropertyChangeListener;
import com.amazonaws.eclipse.core.preferences.accounts.PluginPreferenceStoreAccountInfo;
import com.amazonaws.eclipse.core.preferences.accounts.PluginPreferenceStoreAccountManager;
import com.amazonaws.eclipse.core.preferences.regions.DefaultRegionMonitor;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.setupwizard.InitialSetupUtils;

/**
 * Entry point for functionality provided by the AWS Toolkit Core plugin,
 * including access to AWS account information.
 */
public class AwsToolkitCore extends AbstractUIPlugin {

    /** The singleton instance of this plugin */
    private static AwsToolkitCore plugin;

    /**
     * Client factories for each individual account in use by the customer.
     */
    private static final Map<String, AWSClientFactory> clientsFactoryByAccountId = new HashMap<String, AWSClientFactory>();

    /** The ID of this plugin */
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.core";

    /** The ID of the main AWS Toolkit preference page */
    public static final String ACCOUNT_PREFERENCE_PAGE_ID =
        "com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage";

    /** The ID of the AWS Toolkit Overview extension point */
    public static final String OVERVIEW_EXTENSION_ID = "com.amazonaws.eclipse.core.overview";

    /** The ID of the AWS Toolkit Overview editor */
    public static final String OVERVIEW_EDITOR_ID = "com.amazonaws.eclipse.core.ui.overview";

    /** The ID of the AWS Explorer view */
    public static final String EXPLORER_VIEW_ID = "com.amazonaws.eclipse.explorer.view";

    public static final String IMAGE_CLOUDFORMATION_SERVICE = "cloudformation-service";
    public static final String IMAGE_CLOUDFRONT_SERVICE = "cloudfront-service";
    public static final String IMAGE_IAM_SERVICE = "iam-service";
    public static final String IMAGE_RDS_SERVICE = "rds-service";
    public static final String IMAGE_S3_SERVICE = "s3-service";
    public static final String IMAGE_SIMPLEDB_SERVICE = "simpledb-service";
    public static final String IMAGE_SNS_SERVICE = "sns-service";
    public static final String IMAGE_SQS_SERVICE = "sqs-service";

    public static final String IMAGE_REMOVE = "remove";
    public static final String IMAGE_ADD = "add";
    public static final String IMAGE_AWS_TOOLKIT_TITLE = "aws-toolkit-title";
    public static final String IMAGE_EXTERNAL_LINK = "external-link";
    public static final String IMAGE_WRENCH = "wrench";
    public static final String IMAGE_SCROLL = "scroll";
    public static final String IMAGE_GEARS = "gears";
    public static final String IMAGE_GEAR = "gear";
    public static final String IMAGE_HTML_DOC = "html";
    public static final String IMAGE_AWS_LOGO = "logo";
    public static final String IMAGE_AWS_ICON = "icon";
    public static final String IMAGE_TABLE = "table";
    public static final String IMAGE_BUCKET = "bucket";
    public static final String IMAGE_REFRESH = "refresh";
    public static final String IMAGE_DATABASE = "database";
    public static final String IMAGE_STACK = "database";
    public static final String IMAGE_QUEUE = "queue";
    public static final String IMAGE_TOPIC = "topic";
    public static final String IMAGE_START = "start";
    public static final String IMAGE_PUBLISH = "publish";
    public static final String IMAGE_EXPORT = "export";
    public static final String IMAGE_STREAMING_DISTRIBUTION = "streaming-distribution";
    public static final String IMAGE_DISTRIBUTION = "distribution";
    public static final String IMAGE_GREEN_CIRCLE = "red-circle";
    public static final String IMAGE_RED_CIRCLE = "green-circle";
    public static final String IMAGE_GREY_CIRCLE = "grey-circle";
    public static final String IMAGE_USER = "user";
    public static final String IMAGE_GROUP = "group";
    public static final String IMAGE_KEY = "key";
    public static final String IMAGE_ROLE = "role";
    public static final String IMAGE_INFORMATION = "information";
    public static final String IMAGE_DOWNLOAD = "download";

    public static final String IMAGE_FLAG_PREFIX = "flag-";

    public static final String IMAGE_WIZARD_CONFIGURE_DATABASE = "configure-database-wizard";


    /** OSGI ServiceTracker object for querying details of proxy configuration */
    private ServiceTracker proxyServiceTracker;

    /** Monitors for changes of default region */
    private DefaultRegionMonitor defaultRegionMonitor;

    /** The AccountManager which persists account-related preference properties */
    private PluginPreferenceStoreAccountManager accountManager;


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
        return getClientFactory(null);
    }

    /**
     * Returns the client factory for the given account id. The client is
     * responsible for ensuring that the given account Id is valid and properly
     * configured.
     *
     * @param accountId
     *            The account to use for credentials, or null for the currently
     *            selected account.
     * @see AwsToolkitCore#getAccountInfo(String)
     */
    public static synchronized AWSClientFactory getClientFactory(String accountId) {
        if ( accountId == null )
            accountId = getDefault().getCurrentAccountId();
        if ( !clientsFactoryByAccountId.containsKey(accountId) ) {
            clientsFactoryByAccountId.put(accountId, new AWSClientFactory(new PluginPreferenceStoreAccountInfo(getDefault()
                    .getPreferenceStore(), accountId)));
        }
        return clientsFactoryByAccountId.get(accountId);
    }

    /**
     * Returns the account manager associated with this plugin.
     */
    public PluginPreferenceStoreAccountManager getAccountManager() {
        return accountManager;
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

        // Create AccountManager and start monitoring account preference changes
        accountManager = new PluginPreferenceStoreAccountManager(getPreferenceStore());
        accountManager.startAccountMonitors();

        // Start listening for region preference changes...
        defaultRegionMonitor = new DefaultRegionMonitor();
        getPreferenceStore().addPropertyChangeListener(defaultRegionMonitor);

        bootstrapAccountPreferences();

        RegionUtils.init();

        // Start listening to changes on default region and region default account preference,
        // and correspondingly update current account
        PreferencePropertyChangeListener resetAccountListenr = new PreferencePropertyChangeListener() {

            public void watchedPropertyChanged() {
                Region newRegion = RegionUtils.getCurrentRegion();
                accountManager.updateCurrentAccount(newRegion);
            }
        };
        accountManager.addDefaultAccountChangeListener(resetAccountListenr);
        addDefaultRegionChangeListener(resetAccountListenr);

        InitialSetupUtils.runInitialSetupWizard();
    }

    /**
     * Bootstraps the current account preferences for new customers or customers
     * migrating from the legacy single-account or global-accounts-only preference
     */
    private void bootstrapAccountPreferences() {
        /* Bootstrap customers from legacy single-account preference */
        String currentAccount = getPreferenceStore().getString(PreferenceConstants.P_CURRENT_ACCOUNT);
        
        // Bootstrap new customers
        if ( currentAccount == null || currentAccount.length() == 0 ) {
            String accountId = UUID.randomUUID().toString();
            getPreferenceStore().putValue(PreferenceConstants.P_CURRENT_ACCOUNT, accountId);
            getPreferenceStore().putValue(PreferenceConstants.P_ACCOUNT_IDS, accountId);
            getPreferenceStore().putValue(accountId + ":" + PreferenceConstants.P_ACCOUNT_NAME,
                    PreferenceConstants.DEFAULT_ACCOUNT_NAME_BASE_64);

            for ( String prefName : new String[] { PreferenceConstants.P_ACCESS_KEY,
                    PreferenceConstants.P_CERTIFICATE_FILE, PreferenceConstants.P_PRIVATE_KEY_FILE,
                    PreferenceConstants.P_SECRET_KEY, PreferenceConstants.P_USER_ID, } ) {
                convertExistingPreference(accountId, prefName);
            }
        }

        /* Bootstrap customers from the global-accounts-only preference */
        String globalDefaultAccount = getPreferenceStore().getString(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT);
        if (globalDefaultAccount == null || globalDefaultAccount.length() == 0) {
            getPreferenceStore().putValue(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT,
                    getPreferenceStore().getString(PreferenceConstants.P_CURRENT_ACCOUNT));
        }
    }

    protected void convertExistingPreference(String accountId, String preferenceName) {
        getPreferenceStore().putValue(accountId + ":" + preferenceName,
                getPreferenceStore().getString(preferenceName));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        accountManager.stopAccountMonitors();
        getPreferenceStore().removePropertyChangeListener(defaultRegionMonitor);
        proxyServiceTracker.close();
        super.stop(context);
    }

    /**
     * Returns the currently selected account info.
     *
     * @return The user's AWS account info.
     */
    public AccountInfo getAccountInfo() {
        return accountManager.getAccountInfo();
    }

    /**
     * Returns the current account Id
     */
    public String getCurrentAccountId() {
        return accountManager.getCurrentAccountId();
    }

    /**
     * Registers a listener to receive notifications when the default
     * region is changed.
     *
     * @param listener
     *            The listener to add.
     */
    public void addDefaultRegionChangeListener(PreferencePropertyChangeListener listener) {
        defaultRegionMonitor.addChangeListener(listener);
    }

    /**
     * Stops a listener from receiving notifications when the default
     * region is changed.
     *
     * @param listener
     *            The listener to remove.
     */
    public void removeDefaultRegionChangeListener(PreferencePropertyChangeListener listener) {
        defaultRegionMonitor.removeChangeListener(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        String[] images = new String[] {
                IMAGE_WIZARD_CONFIGURE_DATABASE, "/icons/wizards/configure_database.png",

                IMAGE_CLOUDFORMATION_SERVICE, "/icons/cloudformation-service.png",
                IMAGE_CLOUDFRONT_SERVICE,     "/icons/cloudfront-service.png",
                IMAGE_IAM_SERVICE,            "/icons/iam-service.png",
                IMAGE_RDS_SERVICE,            "/icons/rds-service.png",
                IMAGE_S3_SERVICE,             "/icons/s3-service.png",
                IMAGE_SIMPLEDB_SERVICE,       "/icons/simpledb-service.png",
                IMAGE_SNS_SERVICE,            "/icons/sns-service.png",
                IMAGE_SQS_SERVICE,            "/icons/sqs-service.png",

                IMAGE_ADD,                    "/icons/add.png",
                IMAGE_REMOVE,                 "/icons/remove.gif",
                IMAGE_REFRESH,                "/icons/refresh.png",
                IMAGE_BUCKET,                 "/icons/bucket.png",
                IMAGE_AWS_LOGO,               "/icons/logo_aws.png",
                IMAGE_HTML_DOC,               "/icons/document_text.png",
                IMAGE_GEAR,                   "/icons/gear.png",
                IMAGE_GEARS,                  "/icons/gears.png",
                IMAGE_SCROLL,                 "/icons/scroll.png",
                IMAGE_WRENCH,                 "/icons/wrench.png",
                IMAGE_AWS_TOOLKIT_TITLE,      "/icons/aws-toolkit-title.png",
                IMAGE_EXTERNAL_LINK,          "/icons/icon_offsite.gif",
                IMAGE_AWS_ICON,               "/icons/aws-box.gif",
                IMAGE_PUBLISH,                "/icons/document_into.png",
                IMAGE_TABLE,                  "/icons/table.gif",
                IMAGE_DATABASE,               "/icons/database.png",
                IMAGE_QUEUE,                  "/icons/index.png",
                IMAGE_TOPIC,                  "/icons/sns_topic.png",
                IMAGE_START,                  "/icons/start.png",
                IMAGE_EXPORT,                 "/icons/export.gif",
                IMAGE_STREAMING_DISTRIBUTION, "/icons/streaming-distribution.png",
                IMAGE_DISTRIBUTION,           "/icons/distribution.png",
                IMAGE_GREEN_CIRCLE,           "/icons/green-circle.png",
                IMAGE_GREY_CIRCLE,            "/icons/grey-circle.png",
                IMAGE_RED_CIRCLE,             "/icons/red-circle.png",
                IMAGE_USER,                   "/icons/user.png",
                IMAGE_GROUP,                  "/icons/group.png",
                IMAGE_KEY,                    "/icons/key.png",
                IMAGE_ROLE,                   "/icons/role.png",
                IMAGE_INFORMATION,            "/icons/information.png",
                IMAGE_DOWNLOAD,               "/icons/download.png"
        };

        ImageRegistry imageRegistry = super.createImageRegistry();
        Iterator<String> i = Arrays.asList(images).iterator();
        while (i.hasNext()) {
            String id = i.next();
            String imagePath = i.next();
            imageRegistry.put(id, ImageDescriptor.createFromFile(getClass(), imagePath));
        }

        return imageRegistry;
    }

    /**
     * Convenience method for exception logging.
     */
    public void logException(String errorMessage, Throwable e) {
        getLog().log(new Status(Status.ERROR, PLUGIN_ID, errorMessage, e));
    }

    /**
     * Convenience method for logging a debug message at INFO level.
     */
    public void logInfo(String debugMessage) {
        getLog().log(new Status(Status.INFO, PLUGIN_ID, debugMessage, null));
    }
}
