/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.CheckAccountRunnable;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.ServerDefaultsUtils;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;

/**
 * Utilities for converting legacy tomcat cluster server type to elastic
 * beanstalk environment. This can be removed in a future release.
 */
public class LegacyConversionUtils {

    /**
     * Checks for the presence of the legacy tomcat cluster server type, which
     * is deprecated, and offers to remove them if found.
     */
    public static void convertLegacyTomcatClusters() {
        for ( IServer server : ServerCore.getServers() ) {
            // For eclipse 3.5, WTP hasn't yet exposed the getAttribute method
            // on IServer, so we have to cast to Server
            if ( server instanceof Server ) {
                String id = ((Server) server).getAttribute("server-type-id", "");
                if ( "com.amazonaws.eclipse.wtp.servers.tomcatCluster".equals(id) ) {

                    if ( AwsToolkitCore.getDefault().getAccountInfo().isValid() ) {

                        final IStatus status = testConnection();
                        if ( !status.isOK() ) {
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    CantConnectDialog dialog = new CantConnectDialog(status.getMessage());
                                    dialog.open();
                                }
                            });
                        } else {
                            Display.getDefault().asyncExec(new Runnable() {

                                public void run() {
                                    ConvertServersDialog dialog = new ConvertServersDialog();
                                    int result = dialog.open();
                                    if ( result == 0 ) {
                                        convertServers(dialog.removeServers);
                                    }
                                }
                            });
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Fills in the account identifier for any legacy, single-account environment to 
     */
    public static void convertSingleAccountEnvironments() throws Exception {
        String currentAccountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        
        for ( IServer server : ServerCore.getServers() ) {
            // For eclipse 3.5, WTP hasn't yet exposed the getAttribute method
            // on IServer, so we have to cast to Server
            if ( server instanceof Server ) {
                String id = ((Server) server).getAttribute("server-type-id", "");
                if ( ElasticBeanstalkPlugin.SERVER_TYPE_IDS.contains(id) ) {

                    if ( AwsToolkitCore.getDefault().getAccountInfo().isValid() ) {

                        final IStatus status = testConnection();
                        if ( !status.isOK() ) {
                            Display.getDefault().asyncExec(new Runnable() {

                                public void run() {
                                    CantConnectDialog dialog = new CantConnectDialog(status.getMessage());
                                    dialog.open();
                                }
                            });
                        } else {
                            IServerWorkingCopy workingCopy = server.createWorkingCopy();
                            Environment env = (Environment) workingCopy.loadAdapter(Environment.class, null);
                            if ( env.getAccountId() == null || env.getAccountId().length() == 0 ) {
                                env.setAccountId(currentAccountId);
                                workingCopy.save(true, null);
                            }
                        }
                    }
                }
            }
        }

    }
    
    private static IStatus testConnection() {
        try {
            new CheckAccountRunnable(AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(Region.DEFAULT.getEndpoint()));
            return Status.OK_STATUS;
        } catch ( AmazonServiceException ase ) {
            String errorMessage = "Unable to connect to AWS Elastic Beanstalk.  Make sure you've registered your AWS account for the AWS Elastic Beanstalk service.";
            return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, ase);
        } catch ( AmazonClientException ace ) {
            String errorMessage = "Unable to connect to AWS Elastic Beanstalk.  Make sure your computer is connected to the internet, and any network firewalls or proxys are configured appropriately.";
            return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, ace);
        } catch ( Throwable t ) {
            return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "", t);
        }
    }
    
    private static void convertServers(boolean remove) {
        Collection<IServer> serversToConvert = new LinkedList<IServer>();
        for ( IServer server : ServerCore.getServers() ) {
            String id = ((Server) server).getAttribute("server-type-id", "");
            if ( "com.amazonaws.eclipse.wtp.servers.tomcatCluster".equals(id) ) {
                serversToConvert.add(server);
            }
        }

        final IServerType serverType = ServerCore.findServerType(ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID);

        if ( serverType == null )
            throw new RuntimeException("Couldn't find server type");

        for ( IServer server : serversToConvert ) {
            try {
                IRuntimeWorkingCopy runtime = serverType.getRuntimeType().createRuntime(null, null);
                IServerWorkingCopy serverWorkingCopy = serverType.createServer(null, null, runtime,
                        null);
                ServerDefaultsUtils.setDefaultHostName(serverWorkingCopy,
                        Region.DEFAULT.getEndpoint());
                ServerDefaultsUtils.setDefaultServerName(serverWorkingCopy, server.getName());

                Environment env = (Environment) serverWorkingCopy.loadAdapter(Environment.class,
                        null);
                String appName = createApplication(server, env);
                fillInEnvironmentValues(server, appName, env);

                IModule firstModule = null;
                for (IModule module : server.getModules()) {
                    firstModule = module;
                    break;
                }
                if ( firstModule != null ) {
                    serverWorkingCopy.modifyModules(new IModule[] { firstModule }, new IModule[0],
                            null);
                }
                serverWorkingCopy.save(true, null);
                runtime.save(true, null);

                if ( remove ) {
                    server.delete();
                }
            } catch ( CoreException e ) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String createApplication(IServer server, Environment env) {
        AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory(env.getAccountId())
                .getElasticBeanstalkClientByEndpoint(Region.DEFAULT.getEndpoint());
        String applicationName = null;
        for (IModule module : server.getModules()) {
            applicationName = module.getProject().getName();
        }
        if (applicationName == null)
            applicationName = "ApplicationName";

        if ( beanstalk.describeApplications(
                new DescribeApplicationsRequest().withApplicationNames(applicationName))
                .getApplications().isEmpty() )
            beanstalk.createApplication(new CreateApplicationRequest().withApplicationName(applicationName)
                    .withDescription("Automatically created from tomcat cluster"));

        return applicationName;
    }

    private static void fillInEnvironmentValues(IServer server, String appName, Environment env) {
        String envName = server.getName();
        // Elastic Beanstalk needs a relatively short env name
        if (envName.length() > 23) {
            envName = envName.substring(0, 23);
            envName = envName.replaceAll("[^a-zA-Z0-9]", "");
        }
        env.setEnvironmentName(envName);
        env.setApplicationName(appName);
        env.setRegionEndpoint(Region.DEFAULT.getEndpoint());
        env.setAccountId(AwsToolkitCore.getDefault().getCurrentAccountId());
    }

    /**
     * Dialog to ask the user if they want to convert tomcat clusters to clear
     * box environments.
     */
    private static final class ConvertServersDialog extends MessageDialog {

        boolean removeServers;

        private ConvertServersDialog() {
            super(Display.getDefault().getActiveShell(), "Deprecated server type", AwsToolkitCore.getDefault().getImageRegistry()
                    .get(AwsToolkitCore.IMAGE_AWS_ICON), "Amazon EC2 Tomcat clusters are deprecated in favor of AWS Elastic Beanstalk environments. "
                    + "Would you like to create AWS Elastic Beanstalk environments for them instead?  " +
                            "Running EC2 instances will not be affected by this operation.", 0, new String[] { "OK", "Cancel" }, 0);
        }

        @Override
        protected Control createCustomArea(Composite parent) {
            final Button checkbox = new Button(parent, SWT.CHECK);
            checkbox.setText("Remove Tomcat cluster servers after conversion");
            checkbox.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {
                    removeServers = checkbox.getSelection();
                    System.out.println("set remove = " + removeServers);
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                    widgetSelected(e);
                }
            });

            final Hyperlink link = new Hyperlink(parent, SWT.NONE);
            link.setText("Click here to learn more about AWS Elastic Beanstalk");
            link.setHref("https://aws.amazon.com/elasticbeanstalk/");
            link.setUnderlined(true);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                public void linkActivated(HyperlinkEvent e) {
                    BrowserUtils.openExternalBrowser(link.getHref().toString());
                }
            });

            return parent;
        }
    }

}
