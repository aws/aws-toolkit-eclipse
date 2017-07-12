/*
 * Copyright 2009-2012 Amazon Technologies, Inc. 
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
package com.amazonaws.ec2.cluster;

/**
 * Interface for querying cluster configuration options such as what security
 * group the cluster runs in, what key pair is used to access the instances in
 * this cluster, etc.
 */
public interface ClusterConfiguration {

    /**
     * Returns the name of the security group in which this cluster's instances
     * run.
     * 
     * @return The name of the security group in which this cluster's instances
     *         run.
     */
    public String getSecurityGroupName();

    /**
     * Returns the desired size for this cluster.
     * 
     * @return The desired size for this cluster.
     */
    public int getClusterSize();

    /**
     * Returns the optional Elastic IP associated with this cluster.
     * 
     * @return The optional Elastic IP associated with this cluster.
     */
    public String getElasticIp();

    /**
     * Returns the name of the Amazon EC2 region in which this cluster is to
     * run.
     * 
     * @return The name of the Amazon EC2 region in which this cluster is to
     *         run.
     */
    public String getEc2RegionName();

    /**
     * Returns the main port on which this cluster is configured to listen for
     * requests.
     * 
     * @return The main port on which this cluster is configured to listen for
     *         requests.
     */
    public int getMainPort();

    /**
     * Returns the name of the key pair required to log into the instances in
     * this cluster.
     * 
     * @return The name of the key pair required to log into the instances in
     *         this cluster.
     */
    public String getKeyPairName();

    /**
     * Returns the ID of the Amazon EC2 instance type for the hosts in this
     * cluster.
     * 
     * @return The ID of the Amazon EC2 instance type for the hosts in this
     *         cluster.
     */
    public String getEc2InstanceType();

    /**
     * Returns the Amazon EC2 service endpoint with which this cluster
     * communicates.
     * 
     * @return The Amazon EC2 service endpoint with which this cluster
     *         communicates.
     */
    public String getEc2RegionEndpoint();

}
