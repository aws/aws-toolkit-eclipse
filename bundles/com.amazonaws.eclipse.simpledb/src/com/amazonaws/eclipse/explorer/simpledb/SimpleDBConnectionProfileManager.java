/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.simpledb;

import java.util.Properties;

import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.connection.ISimpleDBConnectionProfileConstants;

public class SimpleDBConnectionProfileManager {
    public IConnectionProfile findOrCreateConnectionProfile(final String endpoint) {
        // Attempt to use an existing ConnectionProfile
        for (IConnectionProfile profile : ProfileManager.getInstance().getProfileByProviderID(ISimpleDBConnectionProfileConstants.SIMPLEDB_PROFILE_PROVIDER_ID)) {
            Properties baseProperties = profile.getBaseProperties();

            // Ignore any connection profiles that have their AWS security credentials directly
            // included in their properties.
            String username = baseProperties.getProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID);
            if (username != null && username.length() > 0) {
                continue;
            }

            boolean isCorrectRegion = endpoint.equals(baseProperties.getProperty(ISimpleDBConnectionProfileConstants.ENDPOINT));
            boolean isCorrectAccount = AwsToolkitCore.getDefault().getCurrentAccountId().equals(baseProperties.getProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID));
            if (isCorrectRegion && isCorrectAccount) {
                return profile;
            }
        }

        // If we didn't find a suitable existing ConnectionProfile, create a new one...
        DriverInstance driverInstance = DriverManager.getInstance().getDriverInstanceByID(ISimpleDBConnectionProfileConstants.SIMPLEDB_DRIVER_ID);
        Properties baseProperties = (Properties)driverInstance.getPropertySet().getBaseProperties().clone();
        baseProperties.setProperty(ISimpleDBConnectionProfileConstants.ACCOUNT_ID, AwsToolkitCore.getDefault().getCurrentAccountId());
        baseProperties.setProperty(ISimpleDBConnectionProfileConstants.ENDPOINT, endpoint);

        try {
            String name = createUniqueConnectionProfileName(endpoint);
            String description = "Connection to Amazon SimpleDB (" + endpoint + ")";
            return ProfileManager.getInstance().createProfile(name, description, ISimpleDBConnectionProfileConstants.SIMPLEDB_PROFILE_PROVIDER_ID, baseProperties);
        } catch (ConnectionProfileException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String createUniqueConnectionProfileName(final String endpoint) {
        String currentAccountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        String currentAccountName = AwsToolkitCore.getDefault().getAccountManager().getAllAccountNames().get(currentAccountId);

        String name;
        int suffix = 0;
        do {
            name = "Amazon SimpleDB (account: " + currentAccountName + ", endpoint: " + endpoint + ")";
            if (suffix++ > 0) {
                name += " " + suffix;
            }
        } while (ProfileManager.getInstance().getProfileByName(name) != null);

        return name;
    }
}
