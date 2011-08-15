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
package com.amazonaws.eclipse.rds;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.AuthorizationAlreadyExistsException;
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSecurityGroupMembership;
import com.amazonaws.services.rds.model.DBSecurityGroupNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;

class ConfigureRDSDBConnectionRunnable implements IRunnableWithProgress {

    private static final String DEFAULT_SECURITY_GROUP_DESCRIPTION = "Security group for remote client tools";
    private static final String DEFAULT_SECURITY_GROUP_NAME = "Remote Tool Access Group";

    private final AmazonRDS rds;
    private final ImportDBInstanceDataModel wizardDataModel;
    private DatabaseConnectionFactory connectionFactory;
    private boolean completedSuccessfully = false;

    public ConfigureRDSDBConnectionRunnable(ImportDBInstanceDataModel wizardDataModel) {
        this.wizardDataModel = wizardDataModel;
        this.rds = AwsToolkitCore.getClientFactory().getRDSClient();

        if (wizardDataModel.getDbInstance().getEngine().startsWith("oracle")) {
            this.connectionFactory = new OracleConnectionFactory(wizardDataModel);
        } else if (wizardDataModel.getDbInstance().getEngine().startsWith("mysql")) {
            this.connectionFactory = new MySqlConnectionFactory(wizardDataModel);
        } else {
            throw new RuntimeException("Unsupported database engine: " + wizardDataModel.getDbInstance().getEngine());
        }
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Configuring database connection", 10);

        openSecurityGroupIngress(new SubProgressMonitor(monitor, 5));

        try {
            DriverInstance driverInstance = getDriver();

            /*
             * TODO: We should test the connection profile to make sure the password was correct and
             *      that we can actually connect to the host.
             *
             * If we aren't able to connect, we should warn users, and let them know some of the possible
             * reasons, such as an incorrect password, or if they're connected through a firewall (like our VPN),
             * and how to run their DB instance on other ports so corporate firewalls don't cause problems.
             */
            IConnectionProfile connectionProfile = createConnectionProfile(driverInstance, new SubProgressMonitor(monitor, 4));

            monitor.subTask("Connecting");
            RDSPlugin.connectAndReveal(connectionProfile);
            monitor.worked(1);

            completedSuccessfully = (connectionProfile.getConnectionState() == IConnectionProfile.CONNECTED_STATE);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }

    public boolean didCompleteSuccessfully() {
        return completedSuccessfully;
    }

    private void openSecurityGroupIngress(IProgressMonitor monitor) {
        monitor.beginTask("Configuring database security group", 3);

        // First make sure our security group exists...
        try {
            rds.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest()
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME)).getDBSecurityGroups();
        } catch (DBSecurityGroupNotFoundException e) {
            rds.createDBSecurityGroup(new CreateDBSecurityGroupRequest()
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME)
                    .withDBSecurityGroupDescription(DEFAULT_SECURITY_GROUP_DESCRIPTION));
            // TODO: And probably wait for it to sync?
        }
        monitor.worked(1);

        // Then make sure that it has usable permission...
        List<String> existingSecurityGroupNames = new ArrayList<String>();
        for (DBSecurityGroupMembership groupMembership : wizardDataModel.getDbInstance().getDBSecurityGroups()) {
            existingSecurityGroupNames.add(groupMembership.getDBSecurityGroupName());
        }

        if (existingSecurityGroupNames.contains(DEFAULT_SECURITY_GROUP_NAME) == false) {
            existingSecurityGroupNames.add(DEFAULT_SECURITY_GROUP_NAME);
            rds.modifyDBInstance(new ModifyDBInstanceRequest()
                .withDBInstanceIdentifier(wizardDataModel.getDbInstance().getDBInstanceIdentifier())
                .withDBSecurityGroups(existingSecurityGroupNames));
            // TODO: Then we probably need to wait for the group membership status to become available?
        }
        monitor.worked(1);

        try {
            rds.authorizeDBSecurityGroupIngress(new AuthorizeDBSecurityGroupIngressRequest()
                    .withCIDRIP("0.0.0.0/0")
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME));
            // TODO: Do we need to wait for this rule to be authorized?
        } catch (AuthorizationAlreadyExistsException e) {}
        monitor.worked(1);
    }

    /**
     * Returns a driver for connecting to the user's database. If an existing,
     * compatible driver is found, it will be used, otherwise a new driver will
     * be created and returned.
     *
     * For more information on creating DriverInstances, see:
     * http://stevenmcherry.wordpress.com/2009/04/24/programmatically-creating-dtp-driver-and-profile-definitions/
     *
     * @return A driver for connecting to the user's database.
     */
    private DriverInstance getDriver() {
        String targetId = "DriverDefn." + connectionFactory.getDriverTemplate() + "." + connectionFactory.createDriverName();
        for (DriverInstance driverInstance : DriverManager.getInstance().getAllDriverInstances()) {
            if (driverInstance.getId().equals(targetId)) return driverInstance;
        }

        Properties driverProperties = connectionFactory.createDriverProperties();

        IPropertySet propertySet = new PropertySetImpl(connectionFactory.createDriverName(), targetId);
        propertySet.setBaseProperties(driverProperties);
        DriverInstance driver = new DriverInstance(propertySet);
        DriverManager.getInstance().addDriverInstance(driver);
        return driver;
    }

    /**
     * Creates the DTP connection profile by assembling properties from
     * IJDBCDriverDefinitionConstants, IJDBCConnectionProfileConstants, and a
     * few AWS custom connection profile properties.
     */
    private IConnectionProfile createConnectionProfile(DriverInstance driverInstance, IProgressMonitor monitor)
            throws ConnectionProfileException {
        monitor.beginTask("Creating connection profile", 1);
        DBInstance dbInstance = wizardDataModel.getDbInstance();

        Properties profileProperties = driverInstance.getPropertySet().getBaseProperties();
        profileProperties.setProperty(IJDBCDriverDefinitionConstants.URL_PROP_ID, connectionFactory.createJdbcUrl());
        profileProperties.setProperty(IJDBCDriverDefinitionConstants.PASSWORD_PROP_ID, wizardDataModel.getDbPassword());
        profileProperties.setProperty(IJDBCDriverDefinitionConstants.USERNAME_PROP_ID, dbInstance.getMasterUsername());
        profileProperties.setProperty(IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID, dbInstance.getDBName());
        profileProperties.setProperty("org.eclipse.datatools.connectivity.driverDefinitionID", driverInstance.getId());

        /*
         * We add custom connection profile properties to help us easily recognize
         * the source RDS instance.
         */
        profileProperties.setProperty(RDSDriverDefinitionConstants.DB_INSTANCE_ID, dbInstance.getDBInstanceIdentifier());
        profileProperties.setProperty(RDSDriverDefinitionConstants.DB_REGION_ID, RegionUtils.getCurrentRegion().getId());
        profileProperties.setProperty(RDSDriverDefinitionConstants.DB_ACCCOUNT_ID, AwsToolkitCore.getDefault().getCurrentAccountId());

        String profileName = "Amazon RDS DB: " + dbInstance.getDBInstanceIdentifier() + " - " + RegionUtils.getCurrentRegion().getName();

        /*
         * if the connection profile already exists... just modify it
         */
        IConnectionProfile existingProfile = ProfileManager.getInstance().getProfileByName(profileName);
        if (existingProfile != null) {
            existingProfile.setBaseProperties(profileProperties);
            ProfileManager.getInstance().modifyProfile(existingProfile);
            monitor.worked(1);
            return existingProfile;
        } else {
            IConnectionProfile profile = ProfileManager.getInstance().createProfile(
                        profileName, profileName,
                        connectionFactory.getConnectionProfileProviderId(),
                        profileProperties);
            monitor.worked(1);
            return profile;
        }
    }
}