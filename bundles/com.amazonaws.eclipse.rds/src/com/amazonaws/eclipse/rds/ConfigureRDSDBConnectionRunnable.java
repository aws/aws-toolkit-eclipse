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
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.rds.connectionfactories.DatabaseConnectionFactory;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.AuthorizationAlreadyExistsException;
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSecurityGroupMembership;
import com.amazonaws.services.rds.model.DBSecurityGroupNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBSecurityGroupsRequest;
import com.amazonaws.services.rds.model.ModifyDBInstanceRequest;
import com.amazonaws.services.rds.model.VpcSecurityGroupMembership;

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
        this.connectionFactory = DatabaseConnectionFactory.createConnectionFactory(wizardDataModel);
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Configuring database connection", 10);

        try {
            configureSecurityGroupPermissions(monitor);
            DriverInstance driverInstance = getDriver();

            /*
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

    /**
     * Opens a CIDR IP range ingress for the selected DB instance if the user
     * has selected to have permissions configured automatically.
     *
     * @param monitor
     *            ProgressMonitor for this runnable's progress.
     */
    private void configureSecurityGroupPermissions(IProgressMonitor monitor) {
        if (wizardDataModel.getConfigurePermissions() == false) {
            monitor.worked(5);
            return;
        }

        if (isVpcDbInstance()) {
            /*
             * DB Instances created with the most recent versions of the RDS
             * API will always use VPC security groups, which are owned in
             * the user's EC2 account.
             */
            openVPCSecurityGroupIngress(new SubProgressMonitor(monitor, 5));
        } else {
            /*
             * For older/legacy DB Instances, we need to modify the RDS
             * security groups instead of working with EC2 directly.
             */
            openLegacySecurityGroupIngress(new SubProgressMonitor(monitor, 5));
        }
    }

    /**
     * Returns true if the selected RDS DB Instance is an RDS VPC DB Instance.
     */
    private boolean isVpcDbInstance() {
        return wizardDataModel.getDbInstance().getVpcSecurityGroups().isEmpty() == false;
    }

    public boolean didCompleteSuccessfully() {
        return completedSuccessfully;
    }

    private void openVPCSecurityGroupIngress(IProgressMonitor monitor) {
        monitor.beginTask("Configuring database security group", 3);

        List<VpcSecurityGroupMembership> vpcSecurityGroups = wizardDataModel.getDbInstance().getVpcSecurityGroups();
        if (vpcSecurityGroups == null || vpcSecurityGroups.isEmpty()) {
            throw new RuntimeException("Expected a DB instance with VPC security groups!");
        }

        String vpcSecurityGroupId = vpcSecurityGroups.get(0).getVpcSecurityGroupId();

        try {
            AmazonEC2 ec2 = AwsToolkitCore.getClientFactory().getEC2Client();
            ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(vpcSecurityGroupId)
                .withIpPermissions(new IpPermission()
                    .withFromPort(wizardDataModel.getDbInstance().getEndpoint().getPort())
                    .withToPort(wizardDataModel.getDbInstance().getEndpoint().getPort())
                    .withIpProtocol("tcp")
                    .withIpRanges(wizardDataModel.getCidrIpRange())));
        } catch (AmazonServiceException ase) {
            // We can safely ignore InvalidPermission.Duplicate errors,
            // but will rethrow all other errors.
            if (!ase.getErrorCode().equals("InvalidPermission.Duplicate")) {
                throw ase;
            }
        }
    }

    /**
     * This method opens security group permissions for legacy RDS DB Instances.
     * Legacy DB Instances do not operate in VPC and connection permissions are
     * managed through RDS security groups (not directly through EC2 security
     * groups).
     */
    private void openLegacySecurityGroupIngress(IProgressMonitor monitor) {
        monitor.beginTask("Configuring database security group", 3);

        // First make sure our security group exists...
        try {
            rds.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest()
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME)).getDBSecurityGroups();
        } catch (DBSecurityGroupNotFoundException e) {
            rds.createDBSecurityGroup(new CreateDBSecurityGroupRequest()
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME)
                    .withDBSecurityGroupDescription(DEFAULT_SECURITY_GROUP_DESCRIPTION));
        }
        monitor.worked(1);

        // Then make sure that it has usable permission...
        List<String> existingSecurityGroupNames = new ArrayList<>();
        for (DBSecurityGroupMembership groupMembership : wizardDataModel.getDbInstance().getDBSecurityGroups()) {
            existingSecurityGroupNames.add(groupMembership.getDBSecurityGroupName());
        }

        if (existingSecurityGroupNames.contains(DEFAULT_SECURITY_GROUP_NAME) == false) {
            existingSecurityGroupNames.add(DEFAULT_SECURITY_GROUP_NAME);
            rds.modifyDBInstance(new ModifyDBInstanceRequest()
                .withDBInstanceIdentifier(wizardDataModel.getDbInstance().getDBInstanceIdentifier())
                .withDBSecurityGroups(existingSecurityGroupNames));
        }
        monitor.worked(1);

        try {
            rds.authorizeDBSecurityGroupIngress(new AuthorizeDBSecurityGroupIngressRequest()
                    .withCIDRIP(wizardDataModel.getCidrIpRange())
                    .withDBSecurityGroupName(DEFAULT_SECURITY_GROUP_NAME));
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
        if (wizardDataModel.isUseExistingDriverDefinition()) {
            return wizardDataModel.getDriverDefinition();
        }

        String targetId = "DriverDefn." + connectionFactory.getDriverTemplate() + "." + connectionFactory.createDriverName();
        for (DriverInstance driverInstance : DriverManager.getInstance().getAllDriverInstances()) {
            if (driverInstance.getId().equals(targetId)) return driverInstance;
        }

        Properties driverProperties = new Properties();
        if (wizardDataModel.getJdbcDriver() != null) {
            // The MySQL driver is currently shipped with the plugins, so in this one case,
            // the wizard data model won't have the driver file specified.
            driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, wizardDataModel.getJdbcDriver().getAbsolutePath());
        }
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, connectionFactory.getDriverClass());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID, connectionFactory.getDatabaseVendor());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID, connectionFactory.getDatabaseVersion());
        driverProperties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String.valueOf(true));
        driverProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, connectionFactory.getDriverTemplate());

        if (connectionFactory.getAdditionalDriverProperties() != null) {
            driverProperties.putAll(connectionFactory.getAdditionalDriverProperties());
        }

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

        if (dbInstance.getDBName() != null) {
            profileProperties.setProperty(IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID, dbInstance.getDBName());
        }

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
