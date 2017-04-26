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
package com.amazonaws.eclipse.elasticbeanstalk;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.trace;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.eclipse.wst.server.core.util.HttpLaunchable;
import org.eclipse.wst.server.core.util.WebResource;

import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.util.StringUtils;

/**
 * Implementation of HttpLaunchable that delays resolving the environment's URL until getURL is
 * called. This allows us to create the HttpLaunchable object before the environment is created, and
 * before we know the final URL.
 */
public class ElasticBeanstalkHttpLaunchable extends HttpLaunchable {

    private URL url;
    private final IServer server;
    private final IModuleArtifact moduleArtifact;

    public ElasticBeanstalkHttpLaunchable(IServer server, IModuleArtifact moduleArtifact) {
        super((URL) null);
        this.server = server;
        this.moduleArtifact = moduleArtifact;
    }

    @Override
    public URL getURL() {
        initializeUrl();
        return url;
    }

    /**
     * Sets the host name for this URL used by this adapter.
     */
    public synchronized void setHost(String hostname) throws MalformedURLException {
        this.url = new URL("http://" + hostname + getModuleArtifactPath(moduleArtifact));
    }

    /**
     * Clears the host URL for this adapter
     */
    public synchronized void clearHost() {
        this.url = null;
    }

    private synchronized void initializeUrl() {
        if (url == null) {
            try {
                /*
                 * We need to ask Elastic Beanstalk for the environment's URL at some point. It
                 * could be here, or it could be cached in the environment object from earlier.
                 */
                Object serverDelegate = server.loadAdapter(ServerDelegate.class, null);
                if (serverDelegate instanceof Environment == false) {
                    return;
                }
                Environment environment = (Environment) serverDelegate;

                final String environmentCname = appendTrailingSlashIfNotPresent(getCname(environment));

                url = new URL("http://" + environmentCname + getModuleArtifactPath(moduleArtifact));
                trace("Initializing module artifact URL: " + url.toString());
            } catch (Exception e) {
                ElasticBeanstalkPlugin.getDefault().logError("Unable to determine environment URL:" + e.getMessage(), e);
            }
        }
    }

    private String getCname(Environment environment) {
        return new ElasticBeanstalkClientExtensions(environment).getEnvironmentCname(environment.getEnvironmentName());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[moduleArtifact=" + moduleArtifact.toString() + "]";
    }

    private String getModuleArtifactPath(IModuleArtifact moduleArtifact) {
        if (moduleArtifact instanceof Servlet) {
            return getServletArtifactPath((Servlet) moduleArtifact);
        } else if (moduleArtifact instanceof WebResource) {
            return getWebResourceArtifactPath((WebResource) moduleArtifact);
        } else {
            return null;
        }
    }

    private String getServletArtifactPath(Servlet servlet) {
        if (servlet.getAlias() != null) {
            return trimLeadingSlashIfPresent(servlet.getAlias());
        } else {
            return "servlet/" + servlet.getServletClassName();
        }
    }

    private String getWebResourceArtifactPath(WebResource resource) {
        final String path = trimLeadingSlashIfPresent(resource.getPath().toString());
        if (StringUtils.isNullOrEmpty(path)) {
            return "";
        } else {
            return path;
        }
    }

    private String trimLeadingSlashIfPresent(final String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private String appendTrailingSlashIfNotPresent(final String path) {
        if (path != null && !path.endsWith("/")) {
            return path + "/";
        }
        return path;
    }

}
