/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Simple container for the instances and security group mappings displayed
 * by the viewer associated with this content/label provider.
 */
public class InstancesViewInput {
	/** The EC2 instances being displayed by this viewer */
	public final List<Instance> instances;

	/** A map of instance ids -> security groups */
	public final Map<String, List<String>> securityGroupMap;

	/**
	 * Constructs a new InstancesViewInput object with the specified list of
	 * instances and mapping of instances to security groups.
	 *
	 * @param instances
	 *            A list of the instances that should be displayed.
	 * @param securityGroupMap
	 *            A map of instance ids to the list of security groups in which
	 *            that instance was launched.
	 */
	public InstancesViewInput(final List<Instance> instances, final Map<String, List<String>> securityGroupMap) {
		this.instances = instances;
		this.securityGroupMap = securityGroupMap;
	}
}