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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

@SuppressWarnings("restriction")
public class Environment extends ServerDelegate {

    private static final String PROPERTY_REGION_ENDPOINT         = "regionEndpoint";
    private static final String PROPERTY_APPLICATION_NAME        = "applicationName";
    private static final String PROPERTY_APPLICATION_DESCRIPTION = "applicationDescription";
    private static final String PROPERTY_ENVIRONMENT_NAME        = "environmentName";
    private static final String PROPERTY_ENVIRONMENT_DESCRIPTION = "environmentDescription";
    private static final String PROPERTY_KEY_PAIR_NAME           = "keyPairName";
    private static final String PROPERTY_CNAME                   = "cname";
    private static final String PROPERTY_HEALTHCHECK_URL         = "healthcheckUrl";
    private static final String PROPERTY_SSL_CERT_ID             = "sslCertId";
    private static final String PROPERTY_SNS_ENDPOINT            = "snsEndpoint";

    private static Map<String, EnvironmentDescription> map = new HashMap<String, EnvironmentDescription>();

    @Override
    public void setDefaults(IProgressMonitor monitor) {
        // Disable auto publishing
        setAttribute("auto-publish-setting", 1);
    }

    public String getRegionEndpoint() {
       return getAttribute(PROPERTY_REGION_ENDPOINT, (String)null);
    }

    public void setRegionEndpoint(String regionEndpoint) {
        setAttribute(PROPERTY_REGION_ENDPOINT, regionEndpoint);
    }

    public String getApplicationName() {
        return getAttribute(PROPERTY_APPLICATION_NAME, (String)null);
    }

    public void setApplicationName(String applicationName) {
        setAttribute(PROPERTY_APPLICATION_NAME, applicationName);
    }

    public String getApplicationDescription() {
        return getAttribute(PROPERTY_APPLICATION_NAME, (String)null);
    }

    public void setApplicationDescription(String applicationDescription) {
        setAttribute(PROPERTY_APPLICATION_DESCRIPTION, applicationDescription);
    }

    public String getEnvironmentName() {
        return getAttribute(PROPERTY_ENVIRONMENT_NAME, (String)null);
    }

    public void setEnvironmentName(String environmentName) {
        setAttribute(PROPERTY_ENVIRONMENT_NAME, environmentName);
    }

    public String getEnvironmentDescription() {
        return getAttribute(PROPERTY_ENVIRONMENT_DESCRIPTION, (String)null);
    }

    public void setEnvironmentDescription(String environmentDescription) {
        setAttribute(PROPERTY_ENVIRONMENT_DESCRIPTION, environmentDescription);
    }

    public String getEnvironmentUrl() {
        EnvironmentDescription cachedEnvironmentDescription = getCachedEnvironmentDescription();
        if (cachedEnvironmentDescription == null) return null;
        if (cachedEnvironmentDescription.getCNAME() == null) return null;
        return "http://" + cachedEnvironmentDescription.getCNAME();
    }

    public String getCname() {
        return getAttribute(PROPERTY_CNAME, (String)null);
    }

    public void setCname(String cname) {
        setAttribute(PROPERTY_CNAME, (String)cname);
    }

    public String getKeyPairName() {
        return getAttribute(PROPERTY_KEY_PAIR_NAME, (String) null);
    }

    public void setKeyPairName(String keyPairName) {
        setAttribute(PROPERTY_KEY_PAIR_NAME, keyPairName);
    }

    public String getSslCertificateId() {
        return getAttribute(PROPERTY_SSL_CERT_ID, (String) null);
    }

    public void setSslCertificateId(String sslCertificateId) {
        setAttribute(PROPERTY_SSL_CERT_ID, sslCertificateId);
    }

    public String getHealthCheckUrl() {
        return getAttribute(PROPERTY_HEALTHCHECK_URL, (String) null);
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        setAttribute(PROPERTY_HEALTHCHECK_URL, healthCheckUrl);
    }

    public String getSnsEndpoint() {
        return getAttribute(PROPERTY_SNS_ENDPOINT, (String) null);
    }

    public void setSnsEndpoint(String snsEndpoint) {
        setAttribute(PROPERTY_SNS_ENDPOINT, snsEndpoint);
    }

    /*
     * TODO: We can't quite turn this on yet because WTPWarUtils runs an operation that tries to lock
     *       the whole workspace when it exports the WAR for a project.  If we can figure out how to
     *       get that to not lock the whole workspace, then we can turn this back on.
     */
//    public boolean isUseProjectSpecificSchedulingRuleOnPublish() {
//        return true;
//    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#canModifyModules(org.eclipse.wst.server.core.IModule[], org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IStatus canModifyModules(IModule[] add, IModule[] remove) {
        // If we're not adding any modules, we know this request is fine
        if (add == null) return Status.OK_STATUS;

        if (add.length > 1) return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
            "Only one web application can run in each AWS Elastic Beanstalk environment");

        for (IModule module : add) {
            String moduleTypeId = module.getModuleType().getId().toLowerCase();
            if (moduleTypeId.equals("jst.web") == false) {
                return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unsupported module type: " + module.getModuleType().getName());
            }

            if (module.getProject() != null) {
                IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
                if (status != null && !status.isOK()) return status;
            }
        }

        return Status.OK_STATUS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#getChildModules(org.eclipse.wst.server.core.IModule[])
     */
    @Override
    public IModule[] getChildModules(IModule[] module) {
        if (module == null) return null;

        IModuleType moduleType = module[0].getModuleType();

        if (module.length == 1 && moduleType != null && "jst.web".equalsIgnoreCase(moduleType.getId())) {
            IWebModule webModule = (IWebModule)module[0].loadAdapter(IWebModule.class, null);
            if (webModule != null) {
                return webModule.getModules();
            }
        }

        return new IModule[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#getRootModules(org.eclipse.wst.server.core.IModule)
     */
    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        String moduleTypeId = module.getModuleType().getId().toLowerCase();
        if (moduleTypeId.equals("jst.web")) {
            IStatus status = canModifyModules(new IModule[] {module}, null);
            if (status == null || !status.isOK()) {
                throw new CoreException(status);
            }

            return new IModule[] {module};
        }

        return J2EEUtil.getWebModules(module, null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.core.model.ServerDelegate#modifyModules(org.eclipse.wst.server.core.IModule[], org.eclipse.wst.server.core.IModule[], org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
        IStatus status = canModifyModules(add, remove);
        if (status == null || !status.isOK()) {
            throw new CoreException(status);
        }

        if (add != null && add.length > 0 && getServer().getModules().length > 0) {
            ServerWorkingCopy serverWorkingCopy = (ServerWorkingCopy)getServer();
            serverWorkingCopy.modifyModules(new IModule[0], serverWorkingCopy.getModules(), monitor);
        }
    }

    public void setCachedEnvironmentDescription(EnvironmentDescription environmentDescription) {
        map.put(getServer().getId(), environmentDescription);
    }

    public EnvironmentDescription getCachedEnvironmentDescription() {
        return map.get(getServer().getId());
    }

}
