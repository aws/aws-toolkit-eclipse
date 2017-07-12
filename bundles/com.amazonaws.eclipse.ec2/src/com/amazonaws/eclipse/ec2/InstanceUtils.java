/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * Utilities for looking up instance information based on different criteria.
 */
public class InstanceUtils {

    /** The EC2 client to use when looking up instances */
    private final AmazonEC2 ec2Client;

    /**
     * Constructs a new InstanceUtils object ready to query instance information.
     * Callers should be aware that this object will always use the specified
     * so it won't dynamically pick up account changes, region changes, etc.
     * Callers need to take this into account in their usage.
     *
     * @param ec2Client The EC2 client to use when looking up instance.
     */
    public InstanceUtils(AmazonEC2 ec2Client) {
        this.ec2Client = ec2Client;
    }

    /**
     * Queries EC2 for an instance with the specified instance ID. If no
     * instance is found with that ID, null is returned.
     *
     * @param instanceId
     *            The ID of the instance to look up.
     *
     * @return The instance corresponding to the specified ID, otherwise null if
     *         no corresponding instance was found.
     *
     * @throws AmazonEC2Exception
     *             If any errors are encountered querying EC2.
     */
    public Instance lookupInstanceById(String instanceId) throws AmazonClientException {
        return lookupInstanceByIdAndState(instanceId, null);
    }

    /**
     * Queries EC2 for an instance with the specified instance ID and in the
     * specified state. If no instance is found with that ID and in that state,
     * null is returned.
     *
     * @param instanceId
     *            The ID of the instance to look up.
     * @param state
     *            The state of the instance to look up.
     *
     * @return The instance corresponding to the specified ID and in the
     *         specified state, otherwise null if no corresponding instance is
     *         found.
     *
     * @throws AmazonEC2Exception
     *             If any errors are encountered querying EC2.
     */
    public Instance lookupInstanceByIdAndState(String instanceId, String state) throws AmazonClientException {
        List<String> instanceIds = new ArrayList<>();
        instanceIds.add(instanceId);

        List<Instance> instances = lookupInstancesByIdAndState(instanceIds, state);
        if (instances.isEmpty()) return null;

        return instances.get(0);
    }

    /**
     * Queries EC2 for instances with the specified IDs and returns a list of
     * any that were found.
     *
     * @param instanceIds
     *            The list of instance IDs being searched for.
     *
     * @return A list of any instances corresponding to instance IDs in the
     *         specified list.
     *
     * @throws AmazonEC2Exception
     *             If any errors are encountered querying EC2.
     */
    public List<Instance> lookupInstancesById(List<String> instanceIds) throws AmazonClientException {
        return lookupInstancesByIdAndState(instanceIds, null);
    }

    /**
     * Queries EC2 for instances with the specified IDs and in the specified
     * state and returns a list of any that were found.
     *
     * @param serviceInstanceIds
     *            The list of instance IDs being searched for.
     * @param state
     *            The state of the instances being searched for.
     *
     * @return A list of any instances corresponding to instance IDs in the
     *         specified list and in the specified state.
     *
     * @throws AmazonClientException
     *             If any errors are encountered querying EC2.
     */
    public List<Instance> lookupInstancesByIdAndState(List<String> serviceInstanceIds, String state) throws AmazonClientException {
        AmazonEC2 ec2 = ec2Client;

        List<Instance> instances = new ArrayList<>();

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(serviceInstanceIds);
        DescribeInstancesResult response = ec2.describeInstances(request);

        List<Reservation> reservations = response.getReservations();
        for (Reservation reservation : reservations) {
            for (Instance instance : reservation.getInstances()) {
                if (state == null || instance.getState().getName().equals(state)) {
                    instances.add(instance);
                }
            }
        }

        return instances;
    }

}
