/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentInfoDescription;
import com.amazonaws.services.elasticbeanstalk.model.RequestEnvironmentInfoRequest;
import com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest;


public class LogTailEditorSection extends ServerEditorSection {

    /** The section widget we're managing */
    private Section section;
    private FormToolkit toolkit;
    private Text log;

    private static final Object JOB_FAMILY = new Object();

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        toolkit = getFormToolkit(parent.getDisplay());

        section = toolkit.createSection(parent,
                Section.TITLE_BAR | Section.DESCRIPTION );
        section.setText("Environment Log");
        section.setDescription("Aggregate logs of your Elastic Beanstalk environment");

        Composite composite = toolkit.createComposite(section);
        FillLayout layout = new FillLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        layout.type = SWT.VERTICAL;
        composite.setLayout(layout);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        section.setLayout(layout);

        createLogViewer(composite);
        refresh();
    }

    /**
     * @param composite
     */
    private void createLogViewer(Composite composite) {
        log = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    }

    private class LoadEnvironmentLogJob extends Job {
        private final Environment environment;

        public LoadEnvironmentLogJob(Environment environment) {
            super("Loading log for environment " + environment.getEnvironmentName());
            this.environment = environment;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            AWSElasticBeanstalk elasticBeanstalk = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(
                    environment.getRegionEndpoint());
            try {
                elasticBeanstalk.requestEnvironmentInfo(new RequestEnvironmentInfoRequest().withEnvironmentName(
                        environment.getEnvironmentName()).withInfoType("tail"));
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equals("InvalidParameterValue")) return Status.OK_STATUS;
                else throw ase;
            }

            final List<EnvironmentInfoDescription> envInfos = elasticBeanstalk.retrieveEnvironmentInfo(
                    new RetrieveEnvironmentInfoRequest().withEnvironmentName(environment.getEnvironmentName())
                            .withInfoType("tail")).getEnvironmentInfo();
            HttpClient client = new HttpClient();
            DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(3, true);
            client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);

            // For each instance, there are potentially multiple tail samples.
            // We just display the last one for each instance.
            Map<String, EnvironmentInfoDescription> tails = new HashMap<String, EnvironmentInfoDescription>();
            for (EnvironmentInfoDescription envInfo : envInfos) {
                if ( !tails.containsKey(envInfo.getEc2InstanceId())
                        || tails.get(envInfo.getEc2InstanceId()).getSampleTimestamp()
                                .before(envInfo.getSampleTimestamp()) ) {
                    tails.put(envInfo.getEc2InstanceId(), envInfo);
                }
            }

            List<String> instanceIds = new ArrayList<String>();
            instanceIds.addAll(tails.keySet());
            Collections.sort(instanceIds);

            // Print the id and the log for each instance
            final StringBuilder builder = new StringBuilder();
            for ( String instanceId : instanceIds ) {
                builder.append("Log for ").append(instanceId).append(":").append("\n\n");

                EnvironmentInfoDescription envInfo = tails.get(instanceId);

                // The message is a url to fetch for logs
                HttpMethod method = new GetMethod(envInfo.getMessage());
                try {
                    client.executeMethod(method);
                    builder.append(method.getResponseBodyAsString());
                } catch ( Exception e ) {
                    builder.append("Exception fetching " + envInfo.getMessage());
                }
            }

            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    log.setText(builder.toString());
                }
            });

            return Status.OK_STATUS;
        }

        @Override
        public boolean belongsTo(Object family) {
            return family == JOB_FAMILY;
        }
    }

    /**
     * Refreshes the log.
     */
    void refresh() {
        /*
         * There's a race condition here, but the consequences are trivial.
         */
        if ( Job.getJobManager().find(JOB_FAMILY).length == 0 ) {
            Environment environment = (Environment) server.loadAdapter(Environment.class, null);
            new LoadEnvironmentLogJob(environment).schedule();
        }
    }

}
