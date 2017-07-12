/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.SyncEnvironmentsJob;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.ServerDefaultsUtils;
import com.amazonaws.eclipse.elasticbeanstalk.solutionstacks.SolutionStacks;
import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/**
 * The activator class controls the plug-in life cycle
 */
public class ElasticBeanstalkPlugin extends AbstractAwsPlugin implements IStartup {

    public static final String IMG_AWS_BOX = "aws-box";
    public static final String IMG_SERVER = "server";
    public static final String IMG_IMPORT = "import";
    public static final String IMG_EXPORT = "export";
    public static final String IMG_CLIPBOARD = "clipboard";
    public static final String IMG_ENVIRONMENT = "environment";
    public static final String IMG_SERVICE = "beanstalk-service";
    public static final String IMG_APPLICATION = "application";

    private static final String SUBTLE_DIALOG_FONT = "subtle-dialog";

    public static final String PLUGIN_ID = "com.amazonaws.eclipse.elasticbeanstalk"; //$NON-NLS-1$

    public static final String DEFAULT_REGION = "us-east-1";

    // The shared instance
    private static ElasticBeanstalkPlugin plugin;

    private SyncEnvironmentsJob syncEnvironmentsJob;

    private NewServerListener newServerListener;

    public static final String TOMCAT_6_SERVER_TYPE_ID = "com.amazonaws.eclipse.elasticbeanstalk.servers.environment"; //$NON-NLS-1$
    public static final String TOMCAT_7_SERVER_TYPE_ID = "com.amazonaws.eclipse.elasticbeanstalk.servers.tomcat7"; //$NON-NLS-1$
    public static final String TOMCAT_8_SERVER_TYPE_ID = "com.amazonaws.eclipse.elasticbeanstalk.servers.tomcat8"; //$NON-NLS-1$

    public static final Collection<String> SERVER_TYPE_IDS = new HashSet<>();
    private Font subtleDialogFont;

    static {
        SERVER_TYPE_IDS.add(TOMCAT_6_SERVER_TYPE_ID);
        SERVER_TYPE_IDS.add(TOMCAT_7_SERVER_TYPE_ID);
        SERVER_TYPE_IDS.add(TOMCAT_8_SERVER_TYPE_ID);
    }

    /**
     * Returns the shared plugin instance.
     *
     * @return the shared plugin instance.
     */
    public static ElasticBeanstalkPlugin getDefault() {
        return plugin;
    }

    public static void trace(String message) {
        if ( Platform.inDebugMode() )
            System.out.println(message);
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        plugin = this;

        syncEnvironmentsJob = new SyncEnvironmentsJob();
        syncEnvironmentsJob.schedule();

        newServerListener = new NewServerListener();
        ServerCore.addServerLifecycleListener(newServerListener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        syncEnvironmentsJob.cancel();
        ServerCore.removeServerLifecycleListener(newServerListener);
        super.stop(context);
        if (subtleDialogFont != null) subtleDialogFont.dispose();
        subtleDialogFont = null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    @Override
    public void earlyStartup() {
    }

    public void syncEnvironments() {
        if ( syncEnvironmentsJob != null )
            syncEnvironmentsJob.wakeUp();
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = new ImageRegistry(Display.getCurrent());
        imageRegistry.put(IMG_EXPORT, ImageDescriptor.createFromFile(getClass(), "/icons/export.gif"));
        imageRegistry.put(IMG_IMPORT, ImageDescriptor.createFromFile(getClass(), "/icons/import.gif"));
        imageRegistry.put(IMG_SERVER, ImageDescriptor.createFromFile(getClass(), "/icons/server.png"));
        imageRegistry.put(IMG_AWS_BOX, ImageDescriptor.createFromFile(getClass(), "/icons/aws-box.gif"));
        imageRegistry.put(IMG_CLIPBOARD, ImageDescriptor.createFromFile(getClass(), "/icons/clipboard.gif"));
        imageRegistry.put(IMG_ENVIRONMENT, ImageDescriptor.createFromFile(getClass(), "/icons/environment.png"));
        imageRegistry.put(IMG_SERVICE, ImageDescriptor.createFromFile(getClass(), "/icons/beanstalk-service.png"));
        imageRegistry.put(IMG_APPLICATION, ImageDescriptor.createFromFile(getClass(), "/icons/application.png"));

        return imageRegistry;
    }

    public Font getSubtleDialogFont() {
        return subtleDialogFont;
    }

    public void initializeSubtleDialogFont(Font baseFont) {
        if (getSubtleDialogFont() != null) return;

        FontData[] fontData = baseFont.getFontData();
        for (FontData fd : fontData) fd.setStyle(SWT.ITALIC);

        subtleDialogFont = new Font(Display.getDefault(), fontData);
    }

    /**
     * Returns all AWS Elastic Beanstalk servers known to ServerCore.
     */
    public static Collection<IServer> getExistingElasticBeanstalkServers() {
        List<IServer> elasticBeanstalkServers = new ArrayList<>();

        IServer[] servers = ServerCore.getServers();
        for ( IServer server : servers ) {
            if ( server.getServerType() == null) continue;
            if ( SERVER_TYPE_IDS.contains(server.getServerType().getId()) ) {
                elasticBeanstalkServers.add(server);
            }
        }

        return elasticBeanstalkServers;
    }

    /**
     * Returns the server matching the description given in the region given, if
     * it can be found, or null otherwise.
     */
    public static IServer getServer(EnvironmentDescription environmentDescription, Region region) {
        for ( IServer server : getExistingElasticBeanstalkServers() ) {
            if ( environmentsSame(environmentDescription, region, server) ) {
                return server;
            }
        }
        return null;
    }

    /**
     * Returns whether the environment given represents the server given.
     */
    public static boolean environmentsSame(EnvironmentDescription env, Region region, IServer server) {
        String beanstalkEndpoint = region.getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);

        Environment environment = (Environment) server.getAdapter(Environment.class);

        if (environment == null) return false;

        return environment.getApplicationName().equals(env.getApplicationName())
                && environment.getEnvironmentName().equals(env.getEnvironmentName())
                && environment.getRegionEndpoint().equals(beanstalkEndpoint);
    }

    /**
     * Imports an environment as a Server and returns it.
     *
     * @param monitor
     *            An optional progress monitor that will perform 3 units of work
     *            during the import process.
     * @throws CoreException
     *             If the environment cannot be imported
     */
    public static IServer importEnvironment(EnvironmentDescription environmentDescription, Region region, IProgressMonitor monitor)
            throws CoreException {
        String solutionStackName = environmentDescription.getSolutionStackName();
        String serverTypeId = null;
        try {
            serverTypeId = SolutionStacks.lookupServerTypeIdBySolutionStack(solutionStackName);
        } catch ( Exception e ) {
            throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, e.getMessage()));
        }

        final IServerType serverType = ServerCore.findServerType(serverTypeId);

        IRuntimeWorkingCopy runtime = serverType.getRuntimeType().createRuntime(null, monitor);
        IServerWorkingCopy serverWorkingCopy = serverType.createServer(null, null, runtime, monitor);
        ServerDefaultsUtils.setDefaultHostName(serverWorkingCopy, region.getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK));
        ServerDefaultsUtils.setDefaultServerName(serverWorkingCopy, environmentDescription.getEnvironmentName());

        /*
         * These values must be bootstrapped before we can ask the environment
         * about its configuration.
         */
        Environment env = (Environment) serverWorkingCopy.loadAdapter(Environment.class, monitor);
        env.setAccountId(AwsToolkitCore.getDefault().getCurrentAccountId());
        env.setRegionId(region.getId());
        env.setApplicationName(environmentDescription.getApplicationName());
        env.setEnvironmentName(environmentDescription.getEnvironmentName());
        env.setIncrementalDeployment(true);

        fillInEnvironmentValues(environmentDescription, env, monitor);
        monitor.subTask("Creating server");
        IServer server = serverWorkingCopy.save(true, monitor);
        runtime.save(true, monitor);
        monitor.worked(1);

        return server;
    }

    /**
     * Fills in the environment given with the values in the environment
     * description.
     *
     * @see ElasticBeanstalkPlugin#importEnvironment(EnvironmentDescription, IProgressMonitor)
     */
    private static void fillInEnvironmentValues(EnvironmentDescription elasticBeanstalkEnv, Environment env, IProgressMonitor monitor) {

        ElasticBeanstalkClientExtensions clientExt = new ElasticBeanstalkClientExtensions(env);
        monitor.subTask("Getting application info");
        ApplicationDescription applicationDesc = clientExt.getApplicationDescription(elasticBeanstalkEnv
                .getApplicationName());
        if (applicationDesc != null) {
            env.setApplicationDescription(applicationDesc.getDescription());
        }
        monitor.worked(1);

        monitor.subTask("Getting environment configuration");

        List<ConfigurationSettingsDescription> currentSettings = env.getCurrentSettings();
        for ( ConfigurationSettingsDescription settingDescription : currentSettings ) {
            for ( ConfigurationOptionSetting setting : settingDescription.getOptionSettings() ) {
                if ( setting.getNamespace().equals("aws:autoscaling:launchconfiguration")
                        && setting.getOptionName().equals("EC2KeyName") ) {
                    env.setKeyPairName(setting.getValue());
                }
            }
        }
        monitor.worked(1);

        env.setCname(elasticBeanstalkEnv.getCNAME());
        env.setEnvironmentDescription(elasticBeanstalkEnv.getDescription());
    }

    /**
     * Listens for the creation of new elastic beanstalk servers and syncs all
     * environments' status.
     */
    private class NewServerListener implements IServerLifecycleListener {

        @Override
        public void serverAdded(IServer server) {
            if ( SERVER_TYPE_IDS.contains(server.getServerType().getId()) ) {
                ElasticBeanstalkPlugin.getDefault().syncEnvironments();
            }
        }

        @Override
        public void serverChanged(IServer server) {
        }

        @Override
        public void serverRemoved(IServer server) {
        }
    }

}
