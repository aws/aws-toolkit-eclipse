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

package com.amazonaws.ec2.cluster;

import java.io.File;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.ec2.InstanceUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Represents an individual server/host running in EC2.  This class
 * is not intended to be used directly and should be subclasses by
 * clients instead.
 */
public abstract class Ec2Server {

    /** The EC2 instance running this server */
    protected Instance instance;

    /** True if this server is (or should be) in debug mode */
    private boolean debugMode;

    /**
     * Creates a new Ec2Server associated with the specified EC2 instance.
     *
     * @param instance
     *            The EC2 instance associated with this Ec2Server.
     */
    public Ec2Server(Instance instance) {
        this.instance = instance;
    }

    /**
     * Optional method that application server subclasses can implement to
     * perform additional server initialization. Subclasses who do chose to
     * implement this should be aware that they are responsible for detecting if
     * initializing is necessary or not inside their implementation. The
     * framework will call this during each launch.
     *
     * @throws Exception
     *             If there were any problems while initializing this
     *             application server.
     */
    public void initialize() throws Exception {}

    /**
     * Subclasses of Ec2Server must implement this method with the specific code
     * required to start their server application on the EC2 instance associated
     * with this server object.
     *
     * @throws Exception
     *             If any problems are encountered while starting the
     *             application server on this EC2 instance.
     */
    public abstract void stop() throws Exception;

    /**
     * Subclasses of Ec2Server must implement this method with the specific code
     * required to stop their server application on the EC2 instance associated
     * with this server object.
     *
     * @throws Exception
     *             If any problems are encountered while starting the
     *             application server on this EC2 instance.
     */
    public abstract void start() throws Exception;

    /**
     * Publishes the archive file (containing the project resources that the EC2
     * cluster management layer determined needed to be published) for the
     * project associated with the specified module to this host.
     *
     * @param archiveFile
     *            An archive file containing the project resources that need to
     *            be published. This is not necessarily the entire project, but
     *            just what the EC2 cluster management layer thinks needs to be
     *            published to this host, based on its knowledge of prior
     *            publish events.
     * @param moduleName
     *            The name of the module associated with the project being
     *            published.
     *
     * @throws Exception
     *             If any problems are encountered while publishing.
     */
    public abstract void publish(File archiveFile, String moduleName) throws Exception;

    /**
     * Publishes the server configuration files for this host.
     *
     * @param serverConfigurationDirectory
     *            The directory containing the server configuration files that need
     *            to be published to this server.
     *
     * @throws Exception
     *             If any problems are encountered while publishing.
     */
    public abstract void publishServerConfiguration(File serverConfigurationDirectory) throws Exception;

    /**
     * Sets whether or not this server should be running in debug mode.
     *
     * @param debugMode
     *            True if this server should be running in debug mode.
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Returns whether or not this server is (or should be) running in debug
     * mode.
     *
     * @return True if this server should be running in debug mode.
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Reloads the data about the EC2 instance represented by this server.
     *
     * @param ec2Client
     *            The EC2 client to use when refreshing the instance data.
     * @throws AmazonClientException
     *             If any problems are encountered refreshing the instance data.
     */
    public void refreshInstance(AmazonEC2 ec2Client) throws AmazonClientException {
        InstanceUtils instanceUtils = new InstanceUtils(ec2Client);

        Instance tempInstance = instanceUtils.lookupInstanceById(getInstanceId());

        if (tempInstance == null) {
            throw new AmazonClientException(
                "Unable to find a running instance with id: " + getInstanceId());
        }

        instance = tempInstance;
    }

    /**
     * Returns the IP address of this host.
     *
     * @return The IP address of this host.
     */
    public String getIp() {
        return instance.getPublicDnsName();
    }

    /**
     * Returns the ID of the Amazon EC2 instance this server represents.
     *
     * @return The ID of the Amazon EC2 instance this server represents.
     */
    public String getInstanceId() {
        return instance.getInstanceId();
    }

    /**
     * Returns the Amazon EC2 instance this server represents.
     *
     * @return The Amazon EC2 instance this server represents.
     */
    public Instance getInstance() {
        return instance;
    }

}
