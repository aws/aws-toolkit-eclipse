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

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public class ElasticBeanstalkLabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public Image getDefaultImage(Object element) {
        ImageRegistry imageRegistry = ElasticBeanstalkPlugin.getDefault().getImageRegistry();
        if ( element instanceof ElasticBeanstalkRootElement ) {
            return imageRegistry.get(ElasticBeanstalkPlugin.IMG_SERVICE);
        }

        if ( element instanceof ApplicationDescription ) {
            return imageRegistry.get(ElasticBeanstalkPlugin.IMG_APPLICATION);
        }

        if ( element instanceof EnvironmentDescription ) {
            return imageRegistry.get(ElasticBeanstalkPlugin.IMG_ENVIRONMENT);
        }

        return null;
    }

    @Override
    public String getText(Object element) {
        if ( element instanceof ElasticBeanstalkRootElement ) {
            return "AWS Elastic Beanstalk";
        }

        if ( element instanceof ApplicationDescription ) {
            return ((ApplicationDescription) element).getApplicationName();
        }

        if ( element instanceof EnvironmentDescription ) {
            return ((EnvironmentDescription) element).getEnvironmentName();
        }

        return getExplorerNodeText(element);
    }

}
