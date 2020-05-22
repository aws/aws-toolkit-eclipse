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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.eclipse.ec2.RemoteCommandUtils;

/**
 * Models a proxy that sits in front of servers and load balances requests
 * between them.
 */
public class Ec2WebProxy extends Ec2Server {

    /** The ID for our US-East proxy AMI */
    private static final String US_EAST_1_AMI_ID = "ami-d1ca2db8";

    /** The ID for our EU-West proxy AMI */
    private static final String EU_WEST_1_AMI_ID = "ami-10163e64";

    /** The ID for our US-West proxy AMI */
    private static final String US_WEST_1_AMI_ID = "ami-3b09587e";

    /** The ID for our AP-Southeast proxy AMI */
    private static final String AP_SOUTHEAST_1_AMI_ID = "ami-0df38c5f";


    /** The EC2 instances behind this proxy */
    private List<Instance> proxiedInstances;

    /** Shared utilities for executing remote commands */
    private static final RemoteCommandUtils remoteCommandUtils = new RemoteCommandUtils();

    /** Shared logger */
    private static final Logger logger = Logger.getLogger(Ec2WebProxy.class.getName());

    /**
     * The port on which this proxy should listen for requests, and on which the
     * servers behind this proxy are listening for requests.
     */
    private int serverPort = DEFAULT_PORT;

    /**
     * The default port for this proxy to listen for requests.
     */
    private static final int DEFAULT_PORT = 80;

    /**
     * Returns the ID of the AMI to use when starting this server in EC2, based
     * on the specified region. If an AMI isn't available for the specified
     * region, this method will throw an exception.
     *
     * @param region
     *            The name of the EC2 region (ex: 'us-east-1') in which the
     *            instance will be launched.
     *
     * @return The ID of the AMI to use when starting this server in EC2, based
     *         on the specified region.
     *
     * @throws Exception
     *             If there is no AMI registered for the specified region.
     */
    public static String getAmiIdByRegion(String region) throws Exception {
        if (region.equalsIgnoreCase("us-east-1")) {
            return US_EAST_1_AMI_ID;
        } else if (region.equalsIgnoreCase("eu-west-1")) {
            return EU_WEST_1_AMI_ID;
        } else if (region.equalsIgnoreCase("us-west-1")) {
            return US_WEST_1_AMI_ID;
        } else if (region.equalsIgnoreCase("ap-southeast-1")) {
            return AP_SOUTHEAST_1_AMI_ID;
        }

        throw new Exception("Unsupported region: '" + region + "'");
    }

    /**
     * Creates a new EC2 web proxy running on the specified instance.
     *
     * @param instance
     *            A started instance running the EC2 web proxy AMI.
     */
    public Ec2WebProxy(Instance instance) {
        super(instance);
    }

    /**
     * Sets the main server port for this proxy and the servers behind it. This
     * is the port on which this proxy will listen for requests, and the port on
     * which the servers behind it are listening for requests. It is not
     * currently possible to have the proxy listening on one port and have the
     * servers behind it listening on a different port.
     *
     * @param serverPort
     *            The port on which this proxy should listen for requests, and
     *            on which the servers behind this proxy are listening for
     *            requests.
     */
    public void setMainPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Connects to the proxy and starts the proxy software.
     *
     * @throws IOException
     *             If any problems are encountered connecting to the proxy and
     *             starting the proxy software.
     */
    public void startProxy() throws IOException {
        String newHaproxyLocation = "/env/haproxy/haproxy";

        String startCommand = newHaproxyLocation + " -D -f /etc/haproxy.cfg -p /var/run/haproxy.pid -sf $(</var/run/haproxy.pid)";

        remoteCommandUtils.executeRemoteCommand(startCommand, instance);
    }

    /**
     * Connects to the proxy and publishes the proxy configuration.
     *
     * @throws IOException
     *             If any problems were encountered publishing the
     *             configuration.
     */
    @Override
    public void publishServerConfiguration(File unused) throws Exception {
        HaproxyConfigurationListenSection section
            = new HaproxyConfigurationListenSection("proxy", serverPort);

        for (Instance instance : proxiedInstances) {
            section.addServer(instance.getPrivateDnsName() + ":" + serverPort);
        }

        String proxyConfiguration
            = getGlobalSection().toConfigString()
            + getDefaultsSection().toConfigString()
            + section.toConfigString();

        logger.fine("Publishing proxy configuration:\n" + proxyConfiguration);

        File f = File.createTempFile("haproxyConfig", ".cfg");
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(proxyConfiguration);
        }

        String remoteFile = "/tmp/" + f.getName();
        remoteCommandUtils.copyRemoteFile(f.getAbsolutePath(), remoteFile, instance);

        String remoteCommand = "cp " + remoteFile + " /etc/haproxy.cfg";
        remoteCommandUtils.executeRemoteCommand(remoteCommand, instance);
    }

    /**
     * Sets the instances that this proxy load balances between.
     *
     * @param instances
     *            The instances that this proxy will load balance between.
     */
    public void setProxiedHosts(List<Instance> instances) {
        this.proxiedInstances = instances;
    }

    /**
     * Returns an HaproxyConfigurationSection object already set up with the
     * defaults for the "global" section.
     *
     * @return An HaproxyConfigurationSection object already set up with the
     *         defaults for the "global" section.
     */
    private HaproxyConfigurationSection getGlobalSection() {
        HaproxyConfigurationSection section = new HaproxyConfigurationSection("global");

        section.addProperty("log 127.0.0.1", "local0");
        section.addProperty("log 127.0.0.1", "local1 notice");
        section.addProperty("maxconn",       "4096");
        section.addProperty("user",          "nobody");
        section.addProperty("group",         "nobody");

        return section;
    }

    /**
     * Returns an HaproxyConfigurationSection object already set up with the
     * defaults for the "defaults" section.
     *
     * @return An HaproxyConfigurationSection object already set up with the
     *         defaults for the "defaults" section.
     */
    private HaproxyConfigurationSection getDefaultsSection() {
        HaproxyConfigurationSection section = new HaproxyConfigurationSection("defaults");

        section.addProperty("log",        "global");
        section.addProperty("mode",       "http");
        section.addProperty("option",     "httplog");
        section.addProperty("option",     "dontlognull");
        section.addProperty("retries",    "3");
        section.addProperty("redispatch", "");
        section.addProperty("maxconn",    "2000");
        section.addProperty("contimeout", "5000");
        section.addProperty("clitimeout", "50000");
        section.addProperty("srvtimeout", "50000");

        return section;
    }


    /**
     * Models the HAProxy configuration data for a "listen" section.
     *
     * @author Jason Fulghum <fulghum@amazon.com>
     */
    private class HaproxyConfigurationListenSection extends HaproxyConfigurationSection {
        private int serverCount = 1;

        public HaproxyConfigurationListenSection(String sectionName, int port) {
            super("listen " + sectionName);

            this.addProperty("bind", ":" + port);
            this.addProperty("balance", "roundrobin");
        }

        public void addServer(String server) {
            this.addProperty("server s" + serverCount++, server);
        }
    }

    /**
     * Models an HAProxy configuration section.
     *
     * @author Jason Fulghum <fulghum@amazon.com>
     */
    private class HaproxyConfigurationSection {
        private final String sectionName;
        private final List<String[]> properties = new ArrayList<>();

        /**
         * Creates a new object with the specified section name.
         *
         * @param sectionName
         *            The name of this section that all properties will be
         *            listed under.
         */
        public HaproxyConfigurationSection(String sectionName) {
            this.sectionName = sectionName;
        }

        /**
         * Adds a property to this section. Note that multiple different
         * properties can have the same key.
         *
         * @param key
         *            The name of the property being set.
         * @param value
         *            The value of the property being set.
         */
        public void addProperty(String key, String value) {
            properties.add(new String[] {key, value});
        }

        /**
         * Returns a string representation of this section, designed for use in
         * an haproxy configuration file.
         *
         * @return A string representation of this section, designed for use in
         *         an haproxy configuration file.
         */
        public String toConfigString() {
            StringBuilder builder = new StringBuilder();
            builder.append(sectionName + "\n");

            for (String[] pair : properties) {
                String key = pair[0];
                String value = pair[1];

                builder.append("\t" + key + " \t" + value + "\n");
            }
            builder.append("\n");

            return builder.toString();
        }
    }

    public void setInstance(Instance newInstance) {
        this.instance = newInstance;
    }

    @Override
    public void start() throws Exception {
        startProxy();
    }

    @Override
    public void stop() throws Exception {
        // no-op since it's not needed yet
    }

    @Override
    public void publish(File archiveFile, String moduleName) throws Exception {
        // no-op since there's no content
    }

}
