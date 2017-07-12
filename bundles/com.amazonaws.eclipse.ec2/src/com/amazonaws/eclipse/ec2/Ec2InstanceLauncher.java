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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;

/**
 * Responsible for launching EC2 instances and watching them to see if they
 * come up correctly.  The big value of this class is that it handles monitoring
 * the launched instances to see if they started correctly or if they failed.
 */
public class Ec2InstanceLauncher {

    /** Factory providing EC2 clients */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /** The name of the key pair to launch instances with */
    private String keyPairName;

    /** The name of the security group to launch instances with */
    private String securityGroupName;

    /** The number of instances to launch */
    private int numberOfInstances;

    /** The user data to pass to the launched instances */
    private String userData;

    /** The name of the zone to bring up instances in */
    private String availabilityZoneName;

    /** The id of the AMI to launch instances with */
    private String imageId;

    /** The type of instances to launch */
    private String instanceType;

    /** The endpoint of the EC2 region in which instances should be launched */
    private String regionEndpoint;

    /** The arn of the instance profile to launch with */
    private String instanceProfileArn;

    /**
     * Optional progress monitor so that this launcher can poll to see if the
     * user has canceled the launch request.
     */
    private IProgressMonitor progressMonitor;

    /** Shared logger */
    private static final Logger logger = Logger.getLogger(Ec2InstanceLauncher.class.getName());

    /**
     * Constructs a new EC2 instance launcher, ready to launch the specified
     * image with the specified key pair. Additional, optional launch parameters
     * can be configured by calling the setters.
     *
     * @param imageId
     *            The id of the image to launch.
     * @param keyPairName
     *            The name of the key pair with which to launch instances.
     */
    public Ec2InstanceLauncher(String imageId, String keyPairName) {
        this.imageId = imageId;
        this.keyPairName = keyPairName;
        this.numberOfInstances = 1;
        this.instanceType = InstanceTypes.getDefaultInstanceType().id;
    }

    /*
     * Setters
     */

    /**
     * Sets the EC2 region endpoint that should be used when launching these
     * instances.
     *
     * @param regionEndpoint
     *            The EC2 region endpoint that should be used when launching
     *            these instances.
     */
    public void setEc2RegionEndpoint(String regionEndpoint) {
        this.regionEndpoint = regionEndpoint;
    }

    /**
     * Sets the security group with which this launcher will launch instances.
     *
     * @param securityGroupName
     *            The name of the security group with which to launch instances.
     */
    public void setSecurityGroup(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    /**
     * Sets the number of instances this launcher will launch.
     *
     * @param numberOfInstances
     *            The number of instances to be launched.
     */
    public void setNumberOfInstances(int numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    /**
     * Sets the user data with which this launcher will launch instances.
     *
     * @param userData
     *            The user data to launch instances with.
     */
    public void setUserData(String userData) {
        this.userData = userData;
    }

    /**
     * Sets the isntance profile arn to launch with.
     */
    public void setInstanceProfileArn(String instanceProfileArn) {
        this.instanceProfileArn = instanceProfileArn;
    }

    /**
     * Sets the availability zone in which to launch instances.
     *
     * @param availabilityZoneName
     *            The availability zone in which to launch instances.
     */
    public void setAvailabilityZone(String availabilityZoneName) {
        this.availabilityZoneName = availabilityZoneName;
    }

    /**
     * The String ID representing the type of instances to launch.
     *
     * @param instanceType
     *            The instance type ID representing the type of instances to
     *            launch.
     */
    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    /**
     * Sets the optional progress monitor to use to check to see if the user has
     * canceled this launch.
     *
     * @param progressMonitor
     *            The progress monitor to use when checking to see if the user
     *            has canceled this launch.
     */
    public void setProgressMonitor(IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    /*
     * Launch methods
     */

    /**
     * Launches EC2 instances with the parameters configured in this launcher
     * and waits for them to all come online before returning.
     *
     * @throws AmazonClientException
     *             If any problems prevented the instances from starting up.
     * @throws OperationCanceledException
     *             If the user canceled the request for which these instances
     *             were being launched.
     */
    public List<Instance> launchAndWait()
            throws AmazonClientException, OperationCanceledException {

        return startInstances(true);
    }

    /**
     * Launches EC2 instances with the parameters configured in this launcher
     * and returns immediately, without waiting for the new instances to come
     * up.
     *
     * @throws AmazonClientException
     *             If any problems prevented the instances from starting up.
     */
    public void launch() throws AmazonClientException {
        startInstances(false);
    }


    /*
     * Private Interface
     */

    /**
     * Returns an AWS EC2 client ready to be used. If the caller specified an
     * EC2 region endpoint, this client will be fully configured to talk to that
     * region, otherwise it will be configured to work with the default region.
     *
     * @return An AWS EC2 client ready to be used.
     */
    private AmazonEC2 getEc2Client() {
        if (regionEndpoint != null) {
            return clientFactory.getEC2ClientByEndpoint(regionEndpoint);
        }

        return Ec2Plugin.getDefault().getDefaultEC2Client();
    }

    /**
     * Starts up instances and optionally waits for them to come online.
     *
     * @param waitForInstances
     *            True if the caller wants this method to block until the
     *            instances are online, in which case a List of instances will
     *            be returned, otherwise this method will request that the
     *            instances be launched and immediately return.
     *
     * @throws AmazonClientException
     *             If any problems were encountered communicating with Amazon
     *             EC2.
     * @throws OperationCanceledException
     *             If the user canceled the request for which these instances
     *             were being launched.
     */
    private List<Instance> startInstances(boolean waitForInstances)
            throws AmazonClientException, OperationCanceledException {

        logger.info("Requested to start " + numberOfInstances + " new instances.");

        AmazonEC2 ec2 = getEc2Client();

        List<String> securityGroupList = null;
        if (this.securityGroupName != null) {
            securityGroupList = Arrays.asList(new String[] {securityGroupName});
        }

        RunInstancesRequest request = new RunInstancesRequest();
        request.setImageId(imageId);
        request.setSecurityGroups(securityGroupList);
        request.setMinCount(numberOfInstances);
        request.setMaxCount(numberOfInstances);
        request.setKeyName(keyPairName);
        request.setInstanceType(instanceType);
        if ( instanceProfileArn != null ) {
            request.setIamInstanceProfile(new IamInstanceProfileSpecification().withArn(instanceProfileArn));
        }

        Placement placement = new Placement();
        placement.setAvailabilityZone(availabilityZoneName);
        request.setPlacement(placement);

        if (userData != null) {
            request.setUserData(userData);
        }

        Reservation initialReservation = ec2.runInstances(request).getReservation();

        // If the caller doesn't care about waiting for the instances to come up, go
        // ahead and return.
        if (!waitForInstances) {
            return null;
        }

        List<Instance> instances = initialReservation.getInstances();

        final Map<String, Instance> pendingInstancesById = new HashMap<>();
        for (Instance instance : instances) {
            pendingInstancesById.put(instance.getInstanceId(), instance);
        }

        final List<String> instanceIds = new ArrayList<>();
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }

        Map<String, Instance> startedInstancesById = new HashMap<>();

        do {
            pauseForInstancesToStartUp();

            DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
            describeRequest.setInstanceIds(instanceIds);
            List<Reservation> reservations = ec2.describeInstances(describeRequest).getReservations();
            for (Reservation reservation : reservations) {
                for (Instance instance : reservation.getInstances()) {
                    // TODO: state codes would be much better...
                    if (instance.getState().getName().equalsIgnoreCase("pending")) {
                        continue;
                    }

                    String instanceId = instance.getInstanceId();
                    pendingInstancesById.remove(instanceId);

                    startedInstancesById.put(instanceId, instance);
                }
            }
            // TODO: error detection for startup failures
        } while (!pendingInstancesById.isEmpty());

        return new ArrayList<>(startedInstancesById.values());
    }

    /**
     * Pauses for a few seconds so that instances have a chance to start up.
     * This method also examines the progress monitor (if set) to see if the
     * user has canceled the request for which these instances are being
     * launched. If the request has been canceled, this method will throw an
     * OperationCanceledException.
     */
    protected void pauseForInstancesToStartUp() throws OperationCanceledException {
        for (int i = 0; i < 5; i++) {
            if (progressMonitor != null) {
                if (progressMonitor.isCanceled()) {
                    throw new OperationCanceledException("Operation canceled while " +
                            "waiting for requested EC2 instances to come online.");
                }
            }

            try {Thread.sleep(1000);} catch (InterruptedException ie) {}
        }
    }

}
