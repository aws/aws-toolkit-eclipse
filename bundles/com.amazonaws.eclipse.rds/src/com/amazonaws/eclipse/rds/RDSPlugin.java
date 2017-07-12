/*
 * Copyright 2011-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.rds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.ui.dse.views.DataSourceExplorerView;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.rds.connectionfactories.MySqlConnectionFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class RDSPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.rds";

    // The shared instance
    private static RDSPlugin plugin;

    private static final String MYSQL_DRIVER_FILE_NAME = "mysql-connector-java-5.1.33-bin.jar";


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        createMySqlDriverDefinition();
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
     * We ship a version of the MySQL JDBC driver with the AWS Toolkit for
     * Eclipse, so when this plugin starts up, we make sure we have a DTP Driver
     * Definition created for MySQL.
     */
    private void createMySqlDriverDefinition() {
        MySqlConnectionFactory connectionFactory = new MySqlConnectionFactory(null);

        String targetId = "DriverDefn." + connectionFactory.getDriverTemplate() + "." + connectionFactory.createDriverName();
        DriverInstance existingDriver =  DriverManager.getInstance().getDriverInstanceByID(targetId);
        if (existingDriver != null) {
            if (existingDriver.getJarList().contains(MYSQL_DRIVER_FILE_NAME)) {
                return;
            } else {
                AwsToolkitCore.getDefault().logInfo(
                        "Removing RDS MySQL Driver instance configured with the legacy jdbc connector...");
                DriverManager.getInstance().removeDriverInstance(targetId);
            }
        }

        Properties driverProperties = new Properties();
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, connectionFactory.getDriverClass());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID, connectionFactory.getDatabaseVendor());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID, connectionFactory.getDatabaseVersion());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String.valueOf(true));
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, connectionFactory.getDriverTemplate());

        String jarList = installMySqlDriverInWorkspace().getAbsolutePath();
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, jarList );

        IPropertySet propertySet = new PropertySetImpl(connectionFactory.createDriverName(), targetId);
        propertySet.setBaseProperties(driverProperties);
        DriverInstance driver = new DriverInstance(propertySet);
        DriverManager.getInstance().addDriverInstance(driver);
    }

    /**
     * Unlike the other supported DB engines, the MySQL JDBC driver ships with
     * the AWS Toolkit for Eclipse.
     *
     * We copy the library out of the plugin directory because as new plugin
     * versions are installed, this location could become invalid once new
     * plugin versions replace this version and have a different path on disk.
     *
     * @return The file where the MySQL driver library was installed in the
     *         workspace.
     */
    private File installMySqlDriverInWorkspace() {
        Bundle bundle = Platform.getBundle(RDSPlugin.PLUGIN_ID);
        Path path = new Path("lib/" + MYSQL_DRIVER_FILE_NAME);
        URL fileURL = FileLocator.find(bundle, path, null);

        try {
            IPath stateLocation = Platform.getStateLocation(Platform.getBundle(RDSPlugin.PLUGIN_ID));
            File mysqlDriversDir = new File(stateLocation.toFile(), "mysqlDrivers");

            String jarPath = FileLocator.resolve(fileURL).getPath();
            File sourceFile = new File(jarPath);
            File destinationFile = new File(mysqlDriversDir, MYSQL_DRIVER_FILE_NAME);

            FileUtils.copyFile(sourceFile, destinationFile);
            return destinationFile;
        } catch (IOException e) {
            throw new RuntimeException("Unable to locate MySQL driver on disk.", e);
        }
    }

    /**
     * Connects the specified connection profile and selects and reveals it in
     * the Data Source Explorer view.
     *
     * @param profile
     *            The connection profile to connect and reveal.
     */
    public static void connectAndReveal(final IConnectionProfile profile) {
        IStatus connectStatus = profile.connect();
        if (connectStatus.isOK() == false) {
            Status status = new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, "Unable to connect to the database.  Make sure your password is correct and make sure you can access your database through your network and any firewalls you may be connecting through.");
            StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
            return;
        }

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.datatools.connectivity.DataSourceExplorerNavigator");
                    if (view instanceof DataSourceExplorerView) {
                        DataSourceExplorerView dse = (DataSourceExplorerView)view;
                        StructuredSelection selection = new StructuredSelection(profile);
                        dse.getCommonViewer().setSelection(selection, true);
                    }
                } catch (Exception e) {
                    Status status = new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, "Unable to reveal connection profile: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static RDSPlugin getDefault() {
        return plugin;
    }
}
