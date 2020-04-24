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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorSupportProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.Policy;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.amazonaws.eclipse.core.accounts.AccountInfoProvider;
import com.amazonaws.eclipse.core.accounts.AwsPluginAccountManager;
import com.amazonaws.eclipse.core.diagnostic.ui.AwsToolkitErrorSupportProvider;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.preferences.PreferencePropertyChangeListener;
import com.amazonaws.eclipse.core.preferences.regions.DefaultRegionMonitor;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.telemetry.ClientContextConfig;
import com.amazonaws.eclipse.core.telemetry.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.telemetry.cognito.AWSCognitoCredentialsProvider;
import com.amazonaws.eclipse.core.telemetry.internal.NoOpToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.telemetry.internal.ToolkitAnalyticsManagerImpl;
import com.amazonaws.eclipse.core.ui.preferences.accounts.LegacyPreferenceStoreAccountMerger;
import com.amazonaws.eclipse.core.ui.setupwizard.InitialSetupUtils;

/**
 * Entry point for functionality provided by the AWS Toolkit Core plugin,
 * including access to AWS account information.
 */
public class AwsToolkitCore extends AbstractAwsPlugin {

    /**
     * The singleton instance of this plugin.
     * <p>
     * This static field will remain null until doBasicInit() is completed.
     *
     * @see #doBasicInit(BundleContext)
     * @see #getDefault()
     */
    private static AwsToolkitCore plugin = null;

    /**
     * Used for blocking method calls of getDefault() before the plugin instance
     * finishes basic initialization.
     */
    private static final CountDownLatch pluginBasicInitLatch = new CountDownLatch(1);

    /**
     * Used for blocking any method calls that requires the full initialization
     * of the plugin. (e.g. getAccountManager() method requires loading the
     * profile credentials from the file-system in advance).
     */
    private static final CountDownLatch pluginFullInitLatch = new CountDownLatch(1);

    // In seconds
    private static final int BASIC_INIT_MAX_WAIT_TIME = 60;
    private static final int FULL_INIT_MAX_WAIT_TIME = 60;

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
    public static final String IMAGE_EDIT = "edit";
    public static final String IMAGE_SAVE = "save";
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
    public static final String IMAGE_STACK = "stack";
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


    /**
     * Client factories for each individual account in use by the customer.
     */
    private final Map<String, AWSClientFactory> clientsFactoryByAccountId
            = new HashMap<>();

    /** OSGI ServiceTracker object for querying details of proxy configuration */
    @SuppressWarnings("rawtypes")
    private ServiceTracker proxyServiceTracker;

    /** Monitors for changes of default region */
    private DefaultRegionMonitor defaultRegionMonitor;

    /**
     * The AccountManager which persists account-related preference properties.
     * This field is only available after the plugin is fully initialized.
     */
    private AwsPluginAccountManager accountManager;

    /**
     * For tracking toolkit analytic sessions and events.
     */
    private ToolkitAnalyticsManager toolkitAnalyticsManager;

    /*
     * ======================================
     * APIs that require basic initialization
     * ======================================
     */

    /**
     * Returns the singleton instance of this plugin.
     * <p>
     * This method will be blocked if the singleton instance has not finished
     * the basic initialization.
     *
     * @return The singleton instance of this plugin.
     */
    public static AwsToolkitCore getDefault() {
        if (plugin != null)
            return plugin;

        try {
            pluginBasicInitLatch.await(BASIC_INIT_MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(
                    "Interrupted while waiting for the AWS toolkit core plugin to initialize",
                    e);
        }

        synchronized (AwsToolkitCore.class) {
            if (plugin == null) {
                throw new IllegalStateException(
                        "The core plugin is not initialized after waiting for " +
                        BASIC_INIT_MAX_WAIT_TIME + " seconds.");
            }

            return plugin;
        }
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

    /*
     * ======================================
     * APIs that require full initialization
     * ======================================
     */

    /**
     * Returns the client factory.
     *
     * This method will be blocked until the singleton instance of the plugin is
     * fully initialized.
     */
    public static AWSClientFactory getClientFactory() {
        return getClientFactory(null);
    }

    /**
     * Returns the client factory for the given account id. The client is
     * responsible for ensuring that the given account Id is valid and properly
     * configured.
     * This method will be blocked until the singleton instance of the plugin is
     * fully initialized.
     *
     * @param accountId
     *            The account to use for credentials, or null for the currently
     *            selected account.
     * @see AwsToolkitCore#getAccountInfo(String)
     */
    public static AWSClientFactory getClientFactory(String accountId) {
        return getDefault().privateGetClientFactory(accountId);
    }


    private synchronized AWSClientFactory privateGetClientFactory(String accountId) {
        waitTillFullInit();

        if ( accountId == null )
            accountId = getCurrentAccountId();
        if ( !clientsFactoryByAccountId.containsKey(accountId) ) {
            clientsFactoryByAccountId.put(
                    accountId,
                    // AWSClientFactory uses the accountId to retrieve the credentials
                    new AWSClientFactory(accountId));
        }
        return clientsFactoryByAccountId.get(accountId);
    }

    /**
     * Returns the account manager associated with this plugin.
     *
     * This method will be blocked until the plugin is fully initialized.
     */
    public AwsPluginAccountManager getAccountManager() {
        waitTillFullInit();

        return accountManager;
    }

    /**
     * Returns the current account Id
     * This method will be blocked until the plugin is fully initialized.
     *
     */
    public String getCurrentAccountId() {
        waitTillFullInit();

        return accountManager.getCurrentAccountId();
    }

    /**
     * Returns the currently selected account info.
     * This method will be blocked until the plugin is fully initialized.
     *
     * @return The user's AWS account info.
     */
    public AccountInfo getAccountInfo() {
        waitTillFullInit();

        return accountManager.getAccountInfo();
    }

    /**
     * Returns the toolkit analytics manager. This method blocks until the
     * plugin is fully initialized and it is guaranteed to return a non-null
     * value.
     */
    public ToolkitAnalyticsManager getAnalyticsManager() {
        waitTillFullInit();

        return toolkitAnalyticsManager;
    }

    /*
     * ======================================
     * Core plugin start-up workflow
     * ======================================
     */

    /**
     * We need to avoid any potentially blocking operations in this method, as
     * the documentation has explicitly called out:
     * <p>
     * This method is intended to perform simple initialization of the plug-in
     * environment. The platform may terminate initializers that do not complete
     * in a timely fashion.
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        long startTime = System.currentTimeMillis();
        logInfo("Starting the AWS toolkit core plugin...");

        // Publish the "global" plugin singleton immediately after the basic
        // initialization is done.

        doBasicInit(context);
        synchronized (AwsToolkitCore.class) {
            plugin = this;
        }
        pluginBasicInitLatch.countDown();

        // Then do full initialization

        doFullInit(context);
        pluginFullInitLatch.countDown();

        // All other expensive initialization tasks are executed
        // asynchronously (after all the plugins are started)
        new Job("Initaliazing AWS toolkit core plugin...") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                afterFullInit(context, monitor);
                return Status.OK_STATUS;
            }

        }.schedule();

        logInfo(String.format(
                "AWS toolkit core plugin initialized after %d milliseconds.",
                System.currentTimeMillis() - startTime));
    }

    /**
     * Any basic initialization workflow required prior to publishing the plugin
     * instance via getDefault().
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doBasicInit(BundleContext context) {
        try {

            registerCustomErrorSupport();

            // Initialize proxy tracker
            proxyServiceTracker = new ServiceTracker(context, IProxyService.class.getName(), null);
            proxyServiceTracker.open();

        } catch (Exception e) {
            reportException("Internal error when starting the AWS Toolkit plugin.", e);
        }
    }

    /**
     * The singleton plugin instance will be available via getDeault() BEFORE
     * this method is executed, but methods that are protected by
     * waitTillFullInit() will be blocked until this job is completed.
     */
    private void doFullInit(BundleContext context) {
        try {

            // Initialize region metadata
            RegionUtils.init();

            // Initialize AccountManager
            AccountInfoProvider accountInfoProvider = new AccountInfoProvider(
                    getPreferenceStore());
            // Load profile credentials. Do not bootstrap credentials file
            // if it still doesn't exist, and do not show warning if it fails to
            // load
            accountInfoProvider.refreshProfileAccountInfo(false, false);
            accountManager = new AwsPluginAccountManager(
                    getPreferenceStore(), accountInfoProvider);

            // start monitoring account preference changes
            accountManager.startAccountMonitors();

            // start monitoring the location and content of the credentials file
            accountManager.startCredentialsFileMonitor();

            // Start listening for region preference changes...
            defaultRegionMonitor = new DefaultRegionMonitor();
            getPreferenceStore().addPropertyChangeListener(defaultRegionMonitor);

            // Start listening to changes on default region and region default account preference,
            // and correspondingly update current account
            PreferencePropertyChangeListener resetAccountListenr = new PreferencePropertyChangeListener() {

                @Override
                public void watchedPropertyChanged() {
                    Region newRegion = RegionUtils.getCurrentRegion();
                    accountManager.updateCurrentAccount(newRegion);
                }

            };

            accountManager.addDefaultAccountChangeListener(resetAccountListenr);
            addDefaultRegionChangeListener(resetAccountListenr);

            // Initialize Telemetry
            toolkitAnalyticsManager = initializeToolkitAnalyticsManager();
            toolkitAnalyticsManager.startSession(accountManager, true);

        } catch (Exception e) {
            reportException("Internal error when starting the AWS Toolkit plugin.", e);
        }
    }

    /**
     * Any tasks that don't necessarily need to be run before the plugin starts
     * will be executed asynchronoulsy.
     */
    private void afterFullInit(BundleContext context, IProgressMonitor monitor) {

        // Merge legacy preference store based accounts
        try {
            LegacyPreferenceStoreAccountMerger
                    .mergeLegacyAccountsIntoCredentialsFile();
            accountManager.getAccountInfoProvider()
                    .refreshProfileAccountInfo(false, false);
        } catch (Exception e) {
            reportException("Internal error when scanning legacy AWS account configuration.", e);
        }

        // Initial setup wizard for account and analytics configuration
        try {
            InitialSetupUtils.runInitialSetupWizard();
        } catch (Exception e) {
            reportException( "Internal error when running intial setup wizard.", e);
        }

    }

    /**
     * This method blocks any method invocation that requires the full
     * initialization of this plugin instance.
     */
    private void waitTillFullInit() {
        try {
            boolean initComplete = pluginFullInitLatch
                    .await(FULL_INIT_MAX_WAIT_TIME, TimeUnit.SECONDS);

            if ( !initComplete ) {
                throw new IllegalStateException(
                        "The AWS toolkit core plugin didn't " +
                        "finish initialization after " +
                        FULL_INIT_MAX_WAIT_TIME + " seconds.");
            }

        } catch (InterruptedException e) {
            throw new IllegalStateException(
                    "Interrupted while waiting for the AWS " +
                    "toolkit core plugin to finish initialization.",
                    e);
        }
    }

    /**
     * Register the custom error-support provider, which provides additional UIs
     * for users to directly report errors to "aws-eclipse-errors@amazon.com".
     */
    private static void registerCustomErrorSupport() {
        ErrorSupportProvider existingProvider = Policy.getErrorSupportProvider();

        // Keep a reference to the previously configured provider
        AwsToolkitErrorSupportProvider awsProvider = new AwsToolkitErrorSupportProvider(
                existingProvider);

        Policy.setErrorSupportProvider(awsProvider);
    }

    public boolean isDebugMode() {
        return getBundleVersion().contains("qualifier");
    }

    private ToolkitAnalyticsManager initializeToolkitAnalyticsManager() {

        boolean enabled = getPreferenceStore().getBoolean(
                PreferenceConstants.P_TOOLKIT_ANALYTICS_COLLECTION_ENABLED);

        ToolkitAnalyticsManager toReturn = null;

        if (enabled) {
            try {
                if (isDebugMode()) {
                    toReturn = new ToolkitAnalyticsManagerImpl(
                            AWSCognitoCredentialsProvider.TEST_PROVIDER,
                            ClientContextConfig.TEST_CONFIG);
                } else {
                    toReturn = new ToolkitAnalyticsManagerImpl(
                            AWSCognitoCredentialsProvider.V2_PROVIDER,
                            ClientContextConfig.PROD_CONFIG);
                }

            } catch (Exception e) {
                logError("Failed to initialize analytics manager", e);
            }

        } else {
            logInfo("Toolkit analytics collection disabled");
        }

        if (toReturn == null) {
            toReturn = new NoOpToolkitAnalyticsManager();
        }

        return toReturn;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        toolkitAnalyticsManager.endSession(true);
        accountManager.stopAccountMonitors();
        getPreferenceStore().removePropertyChangeListener(defaultRegionMonitor);
        proxyServiceTracker.close();

        plugin = null;
        super.stop(context);
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
                IMAGE_EDIT,                   "/icons/edit.png",
                IMAGE_SAVE,                   "/icons/save.png",
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
                IMAGE_DOWNLOAD,               "/icons/download.png",
                IMAGE_STACK,                  "/icons/stack.png"
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
}
