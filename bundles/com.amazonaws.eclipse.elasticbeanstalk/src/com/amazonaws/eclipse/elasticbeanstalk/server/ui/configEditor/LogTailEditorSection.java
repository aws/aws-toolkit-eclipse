/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import com.amazonaws.eclipse.core.AwsToolkitHttpClient;
import com.amazonaws.eclipse.core.HttpClientFactory;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentInfoDescription;
import com.amazonaws.services.elasticbeanstalk.model.RequestEnvironmentInfoRequest;
import com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest;
import com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoResult;


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
            AWSElasticBeanstalk elasticBeanstalk = AwsToolkitCore.getClientFactory(environment.getAccountId())
                    .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
            try {
                elasticBeanstalk.requestEnvironmentInfo(new RequestEnvironmentInfoRequest().withEnvironmentName(
                        environment.getEnvironmentName()).withInfoType("tail"));
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equals("InvalidParameterValue")) {
                    return Status.OK_STATUS;
                } else {
                    throw ase;
                }
            }

            RetrieveEnvironmentInfoResult infoResult;
            long pollingStartTime = System.currentTimeMillis();
            while (true) {
                infoResult = elasticBeanstalk.retrieveEnvironmentInfo(
                        new RetrieveEnvironmentInfoRequest()
                            .withInfoType("tail")
                            .withEnvironmentName(environment.getEnvironmentName()));

                // Break once we find environment info
                if (infoResult.getEnvironmentInfo().size() > 0) {
                    break;
                }

                // Or if we don't see any env info after waiting a while
                if (System.currentTimeMillis() - pollingStartTime > 1000*60*5) {
                    break;
                }

                // Otherwise, keep polling
                try {Thread.sleep(1000 * 5);}
                catch (InterruptedException e) { return Status.CANCEL_STATUS; }
            }
            final List<EnvironmentInfoDescription> envInfos = infoResult.getEnvironmentInfo();

            AwsToolkitHttpClient client = HttpClientFactory.create(ElasticBeanstalkPlugin.getDefault(), "https://s3.amazonaws.com");

            // For each instance, there are potentially multiple tail samples.
            // We just display the last one for each instance.
            Map<String, EnvironmentInfoDescription> tails = new HashMap<>();
            for (EnvironmentInfoDescription envInfo : envInfos) {
                if ( !tails.containsKey(envInfo.getEc2InstanceId())
                        || tails.get(envInfo.getEc2InstanceId()).getSampleTimestamp()
                                .before(envInfo.getSampleTimestamp()) ) {
                    tails.put(envInfo.getEc2InstanceId(), envInfo);
                }
            }

            List<String> instanceIds = new ArrayList<>();
            instanceIds.addAll(tails.keySet());
            Collections.sort(instanceIds);

            // Print the id and the log for each instance
            final StringBuilder builder = new StringBuilder();
            for ( String instanceId : instanceIds ) {
                builder.append("Log for ").append(instanceId).append(":").append("\n\n");

                EnvironmentInfoDescription envInfo = tails.get(instanceId);
                try {
                    // The message is a url to fetch for logs
                    InputStream content = client.getEntityContent(envInfo.getMessage());
                    if (content != null) {
                        builder.append(IOUtils.toString(content));
                    }
                } catch ( Exception e ) {
                    builder.append("Exception fetching " + envInfo.getMessage());
                }
            }

            Display.getDefault().syncExec(new Runnable() {
                @Override
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
