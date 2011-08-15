/*
 * Copyright 2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

public class SolutionStacks {
    public static final String TOMCAT_6_32BIT_AMAZON_LINUX = "32bit Amazon Linux running Tomcat 6";
    public static final String TOMCAT_6_64BIT_AMAZON_LINUX = "64bit Amazon Linux running Tomcat 6";
	
    public static final String TOMCAT_7_32BIT_AMAZON_LINUX = "32bit Amazon Linux running Tomcat 7";
    public static final String TOMCAT_7_64BIT_AMAZON_LINUX = "64bit Amazon Linux running Tomcat 7";

	public static String lookupSolutionStackByServerTypeId(String serverTypeId) {
		if (serverTypeId.equalsIgnoreCase("com.amazonaws.eclipse.elasticbeanstalk.servers.environment")) {
			return TOMCAT_6_64BIT_AMAZON_LINUX;
		} else if (serverTypeId.equalsIgnoreCase("com.amazonaws.eclipse.elasticbeanstalk.servers.tomcat7")) {
			return TOMCAT_7_64BIT_AMAZON_LINUX;
		}

		throw new RuntimeException("Unknown server type: " + serverTypeId);
	}
	
	public static String lookupServerTypeIdBySolutionStack(String solutionStack) {
	    if (solutionStack.equals(TOMCAT_6_64BIT_AMAZON_LINUX) || solutionStack.equals(TOMCAT_6_32BIT_AMAZON_LINUX)) {
	        return ElasticBeanstalkPlugin.TOMCAT_6_SERVER_TYPE_ID;
	    } else if (solutionStack.equals(TOMCAT_7_64BIT_AMAZON_LINUX) || solutionStack.equals(TOMCAT_7_32BIT_AMAZON_LINUX)) {
	        return ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID;
	    } 
	    
	    throw new RuntimeException("Unknown solution stack: " + solutionStack);
	}

}