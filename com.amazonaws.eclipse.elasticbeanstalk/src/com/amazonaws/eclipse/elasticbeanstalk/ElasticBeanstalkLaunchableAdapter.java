/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.*;

import java.net.URL;
import java.util.List;

import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.eclipse.wst.server.core.util.HttpLaunchable;
import org.eclipse.wst.server.core.util.WebResource;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public class ElasticBeanstalkLaunchableAdapter extends LaunchableAdapterDelegate {

    public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) {
        if (!(moduleArtifact instanceof Servlet) &&
            !(moduleArtifact instanceof WebResource))
            return null;
        if (moduleArtifact.getModule().loadAdapter(IWebModule.class, null) == null)
            return null;

        return new ElasticBeanstalkHttpLaunchable(server, moduleArtifact);
    }

    /**
     * Implementation of HttpLaunchable that delays resolving the environment's
     * URL until getURL is called. This allows us to create the HttpLaunchable
     * object before the environment is created, and before we know the final
     * URL.
     */
    private class ElasticBeanstalkHttpLaunchable extends HttpLaunchable {
        private URL url;
        private final IServer server;
        private final IModuleArtifact moduleArtifact;

        public ElasticBeanstalkHttpLaunchable(IServer server, IModuleArtifact moduleArtifact) {
            super(null);
            this.server = server;
            this.moduleArtifact = moduleArtifact;
        }

        @Override
        public URL getURL() {
            initializeUrl();
            return url;
        }

        private void initializeUrl() {
            try {
                /*
                 * We need to ask Elastic Beanstalk for the environment's URL at some point.  It could
                 * be here, or it could be cached in the environment object from earlier.
                 */
                Environment environment = (Environment)server.loadAdapter(ServerDelegate.class, null);

                AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
                List<EnvironmentDescription> environments = beanstalk.describeEnvironments(new DescribeEnvironmentsRequest()
                    .withEnvironmentNames(environment.getEnvironmentName())).getEnvironments();

                if (environments.isEmpty()) return;

                String environmentCname = environments.get(0).getCNAME();

                // TODO: Only return the launchable if the environment is available?

                if (!environmentCname.endsWith("/")) environmentCname += "/";
                url = new URL("http://" + environmentCname + getModuleArtifactPath(moduleArtifact));
                trace("Initializing module artifact URL: " + url.toString());
            } catch (Exception e) {
                // TODO: Log an error status
                // return null;
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[moduleArtifact=" + moduleArtifact.toString() + "]";
        }

        private String getModuleArtifactPath(IModuleArtifact moduleArtifact) {
            String path = null;
            if (moduleArtifact instanceof Servlet) {
                Servlet servlet = (Servlet) moduleArtifact;
                if (servlet.getAlias() != null) {
                    path = servlet.getAlias();
                    if (path.startsWith("/")) path = path.substring(1);
                } else {
                    path = "servlet/" + servlet.getServletClassName();
                }
            } else if (moduleArtifact instanceof WebResource) {
                WebResource resource = (WebResource) moduleArtifact;
                path = resource.getPath().toString();
                if (path != null && path.startsWith("/") && path.length() > 0)
                    path = path.substring(1);
                if (path == null || path.length() == 0)
                    path = "";
            }

            return path;
        }
    }

}
