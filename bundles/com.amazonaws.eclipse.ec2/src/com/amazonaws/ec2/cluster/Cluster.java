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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2InstanceLauncher;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.InstanceUtils;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Models a cluster of hosts running in Amazon EC2. Specific application server
 * clusters will need to override this class to provide details on their
 * application servers.
 */
public abstract class Cluster {

    /** Shared logger */
    private static final Logger logger = Logger.getLogger(Cluster.class.getName());

    /** Shared factory for creating Amazon EC2 clients */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /**
     * The individual application servers that make up this cluster.
     */
    protected List<Ec2Server> applicationServers = new ArrayList<>();

    /** The optional proxy for this elastic cluster */
    private Ec2WebProxy webProxy;

    /**
     * A set of EC2 instance IDs representing servers who have had their
     * server configuration successfully published since the last time
     * the server configuration files were invalidated.
     */
    protected Set<String> serversWithUpToDateConfiguration = new HashSet<>();

    /**
     * The configuration that defines how a cluster instance runs, including the
     * security group in which the AMIs run, the key pair that provides access
     * to the instance, etc.
     */
    protected final ClusterConfiguration clusterConfiguration;

    /**
     * A map of the application server AMIs to launch for this cluster mapped by
     * Amazon EC2 region name.
     */
    private Map<String, String> amisByRegion = new HashMap<>();

    /**
     * True if this cluster should be running in debug mode, false otherwise.
     */
    private boolean debugMode;


    /**
     * Creates a new cluster with the specified cluster configuration.
     *
     * @param clusterConfiguration
     *            The configuration details for this cluster including what
     *            security group it runs in, what key pair is used to access it,
     *            etc.
     */
    public Cluster(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration;
    }

    /**
     * Sets whether or not this cluster should be running in debug mode.
     *
     * @param debugMode
     *            True if this cluster should be running in debug mode,
     *            otherwise false.
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;

        for (Ec2Server server : applicationServers) {
            server.setDebugMode(debugMode);
        }
    }

    /**
     * Adds a new host to this cluster.
     *
     * @param instance
     *            The Amazon EC2 instance to add to this cluster.
     */
    public void addHost(Instance instance) {
        Ec2Server server = createApplicationServer(instance);
        server.setDebugMode(debugMode);

        applicationServers.add(server);
    }

    /**
     * Adds the specified host as the proxy for this cluster.
     *
     * @param instance
     *            The Amazon EC2 instance that will serve as the proxy for this
     *            cluster.
     */
    public void addProxyHost(Instance instance) {
        webProxy = new Ec2WebProxy(instance);
        int serverPort = clusterConfiguration.getMainPort();
        if (serverPort != -1) {
            this.webProxy.setMainPort(serverPort);
        }
    }

    /**
     * Publishes the specified resources to this cluster. The default
     * implementation simply iterates over the hosts in the cluster and calls
     * publish on the individual application servers.
     *
     * Subclasses can override the default implementation if a specific cluster
     * of application servers requires a more involved publish process at the
     * cluster level.
     *
     * @see Ec2Server#publish(File, String) for more details on the data
     *      provided in the moduleArchive.
     *
     * @param moduleArchive
     *            The archive of resources to be deployed.
     * @param moduleName
     *            The name of the module being deployed.
     * @throws Exception
     *             If any problems were encountered while publishing.
     */
    public void publish(File moduleArchive, String moduleName) throws Exception {
        for (Ec2Server server : applicationServers) {
            server.publish(moduleArchive, moduleName);
        }
    }

    /**
     * Notifies this cluster that the server configuration files on the cluster
     * hosts are out of sync and need to be published the next time
     * publishServerConfiguration is called.
     */
    public void invalidateServerConfiguration() {
        serversWithUpToDateConfiguration.clear();
    }

    /**
     * Returns the number of hosts contained in this cluster.
     *
     * @return The number of hosts contained in this cluster.
     */
    public int size() {
        return applicationServers.size();
    }

    /**
     * Initializes this running cluster so that it's ready to be used. The exact
     * initialization performed depends on the specifics of the actual
     * application servers. The default implementation simply gives each
     * application server a chance to initialize themselves.
     *
     * Subclasses can override this method to perform initialization specific to
     * other types of application server clusters.
     *
     * @throws Exception
     *             If any problems were encountered while initializing this
     *             cluster.
     */
    public void initialize() throws Exception {
        for (Ec2Server server : applicationServers) {
            server.initialize();
        }
    }

    /**
     * Returns a list of the Amazon EC2 instance IDs for this cluster.
     *
     * @return A list of the Amazon EC2 instance IDs for this cluster.
     */
    public List<String> getInstanceIds() {
        List<String> instanceIds = new ArrayList<>();

        if (applicationServers == null) {
            return instanceIds;
        }

        for (Ec2Server server : applicationServers) {
            instanceIds.add(server.getInstanceId());
        }

        return instanceIds;
    }

    /**
     * Stops the application servers in this cluster.
     *
     * @throws Exception
     *             If any problems are encountered while stopping the
     *             application servers.
     */
    public void stopApplicationServers() throws Exception {
        if (applicationServers != null) {
            for (Ec2Server server : applicationServers) {
                server.stop();
            }
        }

        if (webProxy != null) {
            webProxy.stop();
        }
    }

    /**
     * Starts the application servers in this cluster.
     *
     * @throws Exception
     *             If any problems are encountered while starting the
     *             application servers.
     */
    public void startApplicationServers() throws Exception {
        if (applicationServers != null) {
            for (Ec2Server server : applicationServers) {
                server.start();
            }
        }

        if (webProxy != null) {
            webProxy.start();
        }
    }

    /**
     * Starts this cluster, launching any Amazon EC2 instances that need to be
     * launched, otherwise just reusing the existing hosts in the cluster if
     * they're available. If a proxy is required for this cluster, it will be
     * launched (if necessary) and configured here as well.
     *
     * @param monitor
     *            The progress monitor to use to report progress of starting
     *            this cluster.
     *
     * @throws Exception
     *             If there are any problems launching the cluster hosts,
     *             initializing the application server environments, configuring
     *             the proxy, etc.
     */
    public void start(IProgressMonitor monitor) throws Exception {
        if (monitor == null)
            monitor = new NullProgressMonitor();
        if (monitor.isCanceled())
            return;

        monitor.beginTask("Starting cluster", 30);

        // Automatic security group configuration...
        configureEc2SecurityGroupPermissions(clusterConfiguration
                .getSecurityGroupName());
        monitor.worked(10);

        // Service container launching...
        launchServiceContainerInstances(monitor);
        monitor.worked(10);

        // Proxy launching...
        if (clusterConfiguration.getClusterSize() > 1) {
            launchProxyInstance(monitor);
        } else {
            webProxy = null;
        }
        monitor.worked(10);

        String elasticIp = clusterConfiguration.getElasticIp();
        if (elasticIp != null) {
            associateElasticIp(elasticIp);
        }

        monitor.done();
    }

    /**
     * Returns the IP address (as a String) for the head of this cluster. If this
     * cluster is fronted by a proxy for load balancing, that address will be
     * returned.
     *
     * @return The IP address (as a String) for the head of this cluster.
     */
    public String getIp() {
        // if we're associated with an elastic IP, use that...
        if (clusterConfiguration.getElasticIp() != null) {
            return clusterConfiguration.getElasticIp();
        }

        // if we're using a proxy to load balance, use that...
        if (webProxy != null) {
            return webProxy.getIp();
        }

        // otherwise just use the first instance's public IP
        return applicationServers.get(0).getIp();
    }

    /**
     * Returns the IP address for one of the servers in this cluster (not
     * including the proxy if one is used for load balancing). Callers that need
     * the address of a real server and need to ensure that they aren't going
     * through the load balancer should use this method to ensure they get
     * direct access to one of the application servers.
     *
     * @return The IP address for one of the application servers in this
     *         cluster.
     */
    public String getInstanceIp() {
        return applicationServers.get(0).getIp();
    }


    /**
     * Publishes any server configuration files that need to be published based
     * on callers use of the invalidateServerConfiguration method.
     *
     * @param serverConfigurationDirectory
     *            The directory containing the app server specific configuration
     *            files.
     *
     * @throws Exception
     *             If any problems were encountered publishing the server
     *             configuration files.
     */
    public void publishServerConfiguration(File serverConfigurationDirectory) throws Exception {
        for (Ec2Server server : applicationServers) {
            if (isConfigurationDirty(server.getInstanceId())) {
                server.publishServerConfiguration(serverConfigurationDirectory);
                setConfigurationClean(server.getInstanceId());
            }
        }

        /*
         * TODO: it'd be nice to pass the progress monitor into
         * publishServerConfiguration and get a finer granularity on the
         * progress being made, but at the same time, we want to try to keep any
         * Eclipse specific dependencies out of the cluster management layer as
         * much as possible.
         *
         * We might consider building a simple interface that mirrors what we
         * need from IProgressMonitor and then a simple adapter.
         */
        // monitor.worked(10 * cluster.size());

        if (webProxy != null && isConfigurationDirty(webProxy.getInstanceId())) {
            int mainPort = clusterConfiguration.getMainPort();
            if (mainPort != -1) {
                webProxy.setMainPort(mainPort);
            }

            try {
                webProxy.publishServerConfiguration(null);
                webProxy.start();
                setConfigurationClean(webProxy.getInstanceId());
            } catch (Exception ioe) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to publish proxy configuration: "
                                + ioe.getMessage(), ioe);
                throw new CoreException(status);
            }
        }
    }

    /**
     * Registers the specified AMI for the specified Amazon EC2 region with this
     * cluster. When this cluster needs to launch Amazon EC2 instances, it will
     * use the registered AMIs and the configured region to determine which AMI
     * to launch.
     *
     * @param amiId
     *            The ID of the Amazon EC2 AMI that should be launched for
     *            application servers in this cluster in the specified Amazon
     *            EC2 region.
     * @param region
     *            The name of the Amazon EC2 region in which the specified AMI
     *            exists.
     */
    public void registerAmiForRegion(String amiId, String region) {
        amisByRegion.put(region, amiId);
    }

    /**
     * Returns the ID of the Amazon EC2 AMI for this cluster in the specified
     * region, otherwise null if there is no AMI supported for the specified
     * region.
     *
     * @param region
     *            The Amazon EC2 region in which the returned AMI exists.
     *
     * @return The ID of the Amazon EC2 AMI for this cluster in the specified
     *         region, otherwise null if there is no AMI supported for the
     *         specified region.
     */
    public String getAmiByRegion(String region) {
        return amisByRegion.get(region);
    }

    /**
     * Returns the set of Amazon EC2 region names in which this cluster can run.
     *
     * @return The set of Amazon EC2 region names in which this cluster can run.
     */
    public Set<String> getSupportedRegions() {
        return amisByRegion.keySet();
    }

    /**
     * Returns a unique ID for the Amazon EC2 resource responsible for load
     * balancing in this cluster.
     *
     * @return A unique ID for the Amazon EC2 resource responsible for load
     *         balancing in this cluster.
     */
    public String getProxyId() {
        if (webProxy == null) {
            return null;
        }

        return webProxy.getInstanceId();
    }


    /*
     * Protected Interface
     */

    /**
     * Subclasses must implement this callback method to create the actual
     * application server object for a specified Amazon EC2 instance. The
     * application server object is what defines the custom interaction required
     * for working with a specific application server.
     *
     * @param instance
     *            The Amazon EC2 instance with which the new application server
     *            is associated.
     *
     * @return An object extending the Ec2Server abstract class that provides
     *         the exact logic for working with a specific application server
     *         (publishing, initializing, etc).
     */
    protected abstract Ec2Server createApplicationServer(Instance instance);

    /**
     * Returns true if the specified instance ID needs to have its configuration
     * republished.
     *
     * @param instanceId
     *            The ID of the EC2 instance representing the server whose
     *            configuration files are in question.
     *
     * @return True if the specified instance ID has not been published since
     *         the last time the server configuration files were invalidated
     *         by the invalidateServerConfiguration method,
     *         otherwise false if the published server configuration files for
     *         the specified EC2 instance are up to date.
     */
    protected boolean isConfigurationDirty(String instanceId) {
        return !serversWithUpToDateConfiguration.contains(instanceId);
    }

    /**
     * Notifies this cluster that the specified EC2 instance ID has had its
     * server configuration files successfully published and is now up to date.
     *
     * @param instanceId
     *            The ID of the EC2 instance whose server configuration has been
     *            successfully published.
     */
    protected void setConfigurationClean(String instanceId) {
        serversWithUpToDateConfiguration.add(instanceId);
    }

    /**
     * Returns the Amazon EC2 client to use when working with this cluster.
     *
     * @return The Amazon EC2 client to use when working with this cluster.
     */
    protected AmazonEC2 getEc2Client() {
        String clusterEndpoint = clusterConfiguration.getEc2RegionEndpoint();

        if (clusterEndpoint != null && clusterEndpoint.length() > 0) {
            return clientFactory.getEC2ClientByEndpoint(clusterEndpoint);
        }

        // We should always have a region/endpoint configured in the cluster,
        // but just in case we don't, we'll still return something.
        return Ec2Plugin.getDefault().getDefaultEC2Client();
    }


    /*
     * Private Interface
     */

    /**
     * Configures the security group in which this cluster is running so that
     * the cluster can be remotely administered and accessed.
     *
     * @param securityGroup
     *            The security group to configure.
     *
     * @throws CoreException
     *             If any problems are encountered configuring the specified
     *             security group.
     */
    private void configureEc2SecurityGroupPermissions(String securityGroup)
            throws CoreException {
        String permissiveNetmask = "0.0.0.0/0";
        String strictNetmask = permissiveNetmask;
        try {
            /*
             * We use checkip.amazonaws.com to determine our IP and the most
             * restrictive netmask we can use to lock down security group
             * permissions, but it only works in the US region.
             */
            String region = clusterConfiguration.getEc2RegionName();
            region = region.toLowerCase();
        } catch (Exception e) {
            Status status = new Status(Status.INFO, Ec2Plugin.PLUGIN_ID,
                    "Unable to lookup netmask from checkip.amazon.com.  Defaulting to "
                            + strictNetmask);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        // We want locked down permissions for the control/management port (SSH)
        authorizeSecurityGroupIngressAndSwallowErrors(securityGroup, "tcp", 22,
                22, strictNetmask);

        // TODO: we'll eventually need the remote debugging port to be configurable
        authorizeSecurityGroupIngressAndSwallowErrors(securityGroup, "tcp",
                443, 443, strictNetmask);

        // We want more permissive permissions for the main, public port
        int mainPort = clusterConfiguration.getMainPort();
        if (mainPort != -1) {
            authorizeSecurityGroupIngressAndSwallowErrors(securityGroup, "tcp",
                    mainPort, mainPort, permissiveNetmask);
        }
    }

    /**
     * Calls out to EC2 to authorize the specified port range, protocol, netmask
     * for the specified security group. If any errors are encountered (ex: the
     * requested ingress is already included in the security group permissions),
     * they are silently swallowed.
     *
     * @param securityGroup
     *            The security group to add permissions to.
     * @param protocol
     *            The protocol for the new permissions.
     * @param fromPort
     *            The starting port of the port range.
     * @param toPort
     *            The ending port of the port range.
     * @param netmask
     *            The netmask associated with the new permissions.
     */
    private void authorizeSecurityGroupIngressAndSwallowErrors(
            String securityGroup, String protocol, int fromPort, int toPort,
            String netmask) {
        AmazonEC2 ec2 = getEc2Client();

        AuthorizeSecurityGroupIngressRequest request = new AuthorizeSecurityGroupIngressRequest();
        request.setGroupName(securityGroup);
        request.setFromPort(fromPort);
        request.setToPort(toPort);
        request.setIpProtocol(protocol);
        request.setCidrIp(netmask);

        try {
            ec2.authorizeSecurityGroupIngress(request);
        } catch (AmazonClientException e) {
            /*
             * We don't worry about these exceptions, since callers specifically
             * asked for them to be swallowed
             */
        }
    }

    /**
     * Launches any application server instances that need to be launched to
     * bring this cluster up to full size.
     *
     * @param monitor
     *            The progress monitor to use to report progress.
     * @throws CoreException
     *             If any problems are encountered launching the new Amazon EC2
     *             instances.
     * @throws OperationCanceledException
     *             If we detect that the user canceled the launch.
     */
    private void launchServiceContainerInstances(IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {

        // Calculate how many hosts we need to bring up based on the total size
        // of the cluster and how many hosts we already have up.
        int numberOfActiveHosts = size();
        int numberOfTotalHosts = clusterConfiguration.getClusterSize();
        int numberOfMissingHosts = numberOfTotalHosts - numberOfActiveHosts;
        if (numberOfMissingHosts < 0)
            numberOfMissingHosts = 0;

        /*
         * TODO: Add logic to shutdown extra instances if we're running too
         *       many.
         */

        // Launch as many hosts as we need to get our fleet to the right size
        List<Instance> instances = new ArrayList<>();
        if (numberOfMissingHosts > 0) {
            logger.info("Launching " + numberOfMissingHosts
                    + " service container instances");

            String keyPairName = clusterConfiguration.getKeyPairName();

            try {
                String region = clusterConfiguration.getEc2RegionName();
                String amiId = amisByRegion.get(region);

                if (amiId == null) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "This cluster doesn't have an AMI registered for the '" +  region + "' region");
                    throw new CoreException(status);
                }

                Ec2InstanceLauncher launcher = new Ec2InstanceLauncher(amiId,
                        keyPairName);
                launcher.setProgressMonitor(monitor);
                launcher.setInstanceType(clusterConfiguration
                        .getEc2InstanceType());
                launcher.setEc2RegionEndpoint(clusterConfiguration
                        .getEc2RegionEndpoint());
                launcher.setNumberOfInstances(numberOfMissingHosts);
                launcher.setSecurityGroup(clusterConfiguration
                        .getSecurityGroupName());
                instances = launcher.launchAndWait();

                // Mark the web proxy configuration as out of date so that it
                // gets published next time this cluster is deployed
                if (webProxy != null) {
                    serversWithUpToDateConfiguration.remove(webProxy
                            .getInstanceId());
                }
            } catch (OperationCanceledException oce) {
                // We want to let OperationCanceledExceptions propagate up so we
                // can deal with them at a higher layer.
                throw oce;
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to start cluster: " + e.getMessage(), e);
                throw new CoreException(status);
            }

            logger.info("Successfully started " + instances.size()
                    + " instance(s).");
        } else {
            logger.info("No missing service container instances need to be launched");
        }

        // Update the local list of application servers
        for (Instance instance : instances) {
            addHost(instance);
        }

        try {
            initialize();
        } catch (Exception e) {
            throw new CoreException(new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to fully initialize cluster", e));
        }
    }

    /**
     * Returns the EC2 instance ID of the instance attached to the specified
     * Elastic IP address, or null if the Elastic IP address wasn't found, or
     * isn't attached to an instance.
     *
     * @param elasticIp
     *            The Elastic IP address to check.
     *
     * @return The EC2 instance ID of the instance attached to the specified
     *         Elastic IP address, or null if no instance is attached to this
     *         address.
     *
     * @throws AmazonEC2Exception
     *             If any problems were encountered while looking up the
     *             specified Elastic IP.
     */
    private String lookupAttachedInstance(String elasticIp) throws AmazonClientException {
        DescribeAddressesRequest request = new DescribeAddressesRequest().withPublicIps(elasticIp);
        DescribeAddressesResult result = getEc2Client().describeAddresses(request);
        if (!result.getAddresses().isEmpty()) {
            return result.getAddresses().get(0).getInstanceId();
        }

        return null;
    }

    /**
     * Associates the specified Elastic IP with this cluster.
     *
     * @param elasticIp
     *            The Elastic IP to associate with this cluster.
     */
    private void associateElasticIp(String elasticIp) {
        AmazonEC2 ec2 = getEc2Client();
        InstanceUtils instanceUtils = new InstanceUtils(ec2);

        try {
            String instanceId;
            if (webProxy != null) {
                logger.info("Associating Elastic IP with proxy...");
                instanceId = webProxy.getInstanceId();
            } else {
                logger.info("Associating Elastic IP with application server...");
                instanceId = applicationServers.get(0).getInstanceId();
            }
            logger.info("  - Elastic IP '" + elasticIp + "' => '" + instanceId + "'");

            /*
             * Check if the ElasticIP is already associated with the correct
             * instance, and if so, we don't need to do anything...
             */
            String attachedInstance = this.lookupAttachedInstance(elasticIp);
            if (attachedInstance != null && attachedInstance.equals(instanceId)) {
                return;
            }

            String previousDnsName = instanceUtils.lookupInstanceById(instanceId).getPublicDnsName();

            AssociateAddressRequest request = new AssociateAddressRequest();
            request.setInstanceId(instanceId);
            request.setPublicIp(elasticIp);
            ec2.associateAddress(request);

            /*
             * When we associate the Elastic IP the public DNS name of that host
             * changes, so we need to refresh our Instance objects in
             * order to see the new DNS name. If we don't do that, then we'll
             * run into problems (sooner or later) where the old DNS name
             * doesn't work anymore. We check periodically to account for the
             * fact that Elastic IP changes can take different amounts of time
             * to show up.
             */
            String currentDnsName;
            int pollCount = 0;
            do {
                if (pollCount++ > 90) {
                    throw new Exception("Unable to detect that the Elastic IP was correctly associated");
                }

                try {Thread.sleep(5000);} catch (InterruptedException e) {}
                currentDnsName = instanceUtils.lookupInstanceById(instanceId).getPublicDnsName();
            } while (currentDnsName.equals(previousDnsName));


            if (webProxy != null) {
                webProxy.refreshInstance(getEc2Client());
            } else {
                applicationServers.get(0).refreshInstance(getEc2Client());
            }
        } catch (Exception e) {
            Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID,
                    "Unable to associate Elastic IP with cluster: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }

    /**
     * Launches the load balancing proxy instance if necessary.
     *
     * @param monitor
     *            The progress monitor for this method to use to report progress
     *            and check for a request from the user to cancel this
     *            operation.
     *
     * @throws CoreException
     *             If any problems are encountered that prevent this method from
     *             launching and configuring the load balancing proxy instance.
     * @throws OperationCanceledException
     *             If this method detects that the user requested to cancel this
     *             operation while this method is executing.
     */
    private void launchProxyInstance(IProgressMonitor monitor)
            throws CoreException, OperationCanceledException {

        List<String> instanceIds = getInstanceIds();

        List<Instance> proxiedInstances;
        try {
            InstanceUtils instanceUtils = new InstanceUtils(getEc2Client());
            proxiedInstances = instanceUtils.lookupInstancesById(instanceIds);
        } catch (Exception e) {
            Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to start proxy: " + e.getMessage(), e);
            throw new CoreException(status);
        }

        if (webProxy == null) {
            String keyPairName = clusterConfiguration.getKeyPairName();

            List<Instance> proxyInstances = null;
            try {
                String region = clusterConfiguration.getEc2RegionName();
                String amiId = Ec2WebProxy.getAmiIdByRegion(region);
                // TODO: availability zone support might be nice
                Ec2InstanceLauncher proxyLauncher = new Ec2InstanceLauncher(
                        amiId, keyPairName);
                proxyLauncher.setProgressMonitor(monitor);
                proxyLauncher.setEc2RegionEndpoint(clusterConfiguration
                        .getEc2RegionEndpoint());
                proxyLauncher.setInstanceType(clusterConfiguration
                        .getEc2InstanceType());
                proxyLauncher.setSecurityGroup(clusterConfiguration
                        .getSecurityGroupName());
                proxyInstances = proxyLauncher.launchAndWait();
            } catch (OperationCanceledException oce) {
                // We want to let OperationCanceledExceptions propagate up so we
                // can deal with them at a higher layer.
                throw oce;
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to start proxy: " + e.getMessage(), e);
                throw new CoreException(status);
            }

            webProxy = new Ec2WebProxy(proxyInstances.get(0));
            webProxy.setMainPort(clusterConfiguration.getMainPort());
        }

        webProxy.setProxiedHosts(proxiedInstances);
    }

}
