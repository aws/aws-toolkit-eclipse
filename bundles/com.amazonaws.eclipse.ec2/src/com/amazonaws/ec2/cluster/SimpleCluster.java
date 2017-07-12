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

import java.lang.reflect.Constructor;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Simple cluster implementation suitable for basic clusters that don't need a
 * lot of customization. Callers can create a new BasicCluster object and pass
 * in an implementation of the application server class that they want
 * instantiated for every host in the cluster.
 */
public class SimpleCluster extends Cluster {

    /**
     * The application server class that will be instantiated for any hosts
     * added to this cluster and contains all the logic required for working
     * with application servers in this cluster (publishing, initializing, etc)
     */
    private final Class<? extends Ec2Server> appServerClass;

    /**
     * Creates a new SimpleCluster object that will use the specified
     * application server class for all members of this cluster.
     *
     * @param clusterConfiguration
     *            The configuration details for this cluster instance, including
     *            the security group it runs in, the key pair used to access it,
     *            etc.
     * @param appServerClass
     *            The application server subclass that will be instantiated for
     *            all servers that are added to this cluster. This class must
     *            have a publicly accessible constructor taking a
     *            Instance object, otherwise this class will fail when it
     *            tries to instantiate objects of this class for any hosts added
     *            to this cluster.
     */
    public SimpleCluster(ClusterConfiguration clusterConfiguration, Class<? extends Ec2Server> appServerClass) {
        super(clusterConfiguration);
        this.appServerClass = appServerClass;
    }

    /**
     * {@inheritDoc}
     *
     * Simple implementation of createApplicationServer that instantiates the
     * application server class specified in the constructor for the specified
     * Amazon EC2 instance.
     *
     * @throws RuntimeException
     *             If there were any problems instantiating the application
     *             server class specified in this class' constructor.
     */
    @Override
    protected Ec2Server createApplicationServer(Instance instance) {
        try {
            Constructor<? extends Ec2Server> constructor = appServerClass.getConstructor(Instance.class);
            return constructor.newInstance(instance);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new application server object of type "
                    + appServerClass.getName());
        }
    }

}
