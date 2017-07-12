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
package com.amazonaws.eclipse.elasticbeanstalk.explorer;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public class ElasticBeanstalkContentProvider extends AbstractContentProvider implements ITreeContentProvider {

    private final IOpenListener listener = new IOpenListener() {

        @Override
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection) event.getSelection();

            Iterator<?> i = selection.iterator();
            while ( i.hasNext() ) {
                Object obj = i.next();
                if ( obj instanceof EnvironmentDescription ) {
                    EnvironmentDescription env = (EnvironmentDescription) obj;
                    OpenEnvironmentEditorAction action = new OpenEnvironmentEditorAction(env, RegionUtils.getCurrentRegion());
                    action.run();
                }
            }
        }
    };

    @Override
    public void dispose() {
        viewer.removeOpenListener(listener);
        super.dispose();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        this.viewer.addOpenListener(listener);
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.BEANSTALK;
    }

    @Override
    public Object[] loadChildren(final Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { ElasticBeanstalkRootElement.ROOT_ELEMENT };
        }

        if ( parentElement instanceof ElasticBeanstalkRootElement ) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory()
                            .getElasticBeanstalkClient();
                    List<ApplicationDescription> applications = beanstalk.describeApplications().getApplications();
                    return applications.toArray();
                }
            }.start();
        }

        if ( parentElement instanceof ApplicationDescription ) {
            final ApplicationDescription app = (ApplicationDescription) parentElement;

            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory()
                        .getElasticBeanstalkClient();
                    List<EnvironmentDescription> environments = beanstalk.describeEnvironments(
                        new DescribeEnvironmentsRequest().withApplicationName(app.getApplicationName()))
                        .getEnvironments();
                    return environments.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof ElasticBeanstalkRootElement ||
                element instanceof ApplicationDescription);
    }

}
