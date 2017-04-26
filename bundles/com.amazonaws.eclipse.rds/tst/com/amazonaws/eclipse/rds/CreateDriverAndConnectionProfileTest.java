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
package com.amazonaws.eclipse.rds;

import java.util.Properties;

import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;


public class CreateDriverAndConnectionProfileTest {

//    public static Properties createProperties() {
//        Properties baseProperties = new Properties();
//        baseProperties.setProperty( IDriverMgmtConstants.PROP_DEFN_JARLIST, jarList );
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, "org.apache.derby.jdbc.ClientDriver"); //$NON-NLS-1$
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.URL_PROP_ID, driverURL);
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID, username);
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID, password);
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID, vendor);
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID, version);
//        baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_NAME_PROP_ID, databasename);
//        baseProperties.setProperty( IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID, String.valueOf( true ) );
//        baseProperties.setProperty( IDriverMgmtConstants.PROP_DEFN_TYPE, "org.eclipse.datatools.connectivity.db.derby102.clientDriver");
//
//        return baseProperties;
//    }


    public static void main(String[] args) throws Exception {
        printOutDtpStuff();
    }

    public static void printOutDtpStuff() {
        DriverInstance[] list = DriverManager.getInstance().getAllDriverInstances();
        for(int i = 0; i < list.length; i++){
           System.out.println("=============" );
           System.out.println("Driver ID: " + list[i].getId() );
           System.out.println("Driver Jar List: " + list[i].getJarList() );
           System.out.println("Driver Name: " + list[i].getName() );
           list[i].getPropertySet().getBaseProperties().list(System.out);
        }

        IConnectionProfile[] plist = ProfileManager.getInstance().getProfiles();
        for(int i = 0; i < plist.length; i++){
            System.out.println("=============" );
            System.out.println("Profile Name: " + plist[i].getName() );
            System.out.println("Profile Provider ID: " + plist[i].getProviderId() );
            System.out.println("Profile Provider Name: " + plist[i].getProviderName() );
            plist[i].getBaseProperties().list(System.out);
        }
    }
}
