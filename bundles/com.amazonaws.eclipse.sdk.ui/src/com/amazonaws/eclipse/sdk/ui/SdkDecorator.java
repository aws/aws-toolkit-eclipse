/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.sdk.ui;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;

/**
 * This class decorates the Project Explorer entry for the AWS SDK for Java
 * classpath container to include a higlighted version number, making it easily
 * available at a glance rather than from the Properties window.
 */
public class SdkDecorator implements ILabelDecorator {

    private static final String AWS_JAVA_SDK_CONTAINER_NAME = "AWS SDK for Java";

    public Image decorateImage(Image image, Object element) {
        if (((ClassPathContainer) element).getLabel().startsWith(AWS_JAVA_SDK_CONTAINER_NAME)) {
            //TODO: re-enable this once we get a decent icon image
            //return AwsToolkitCore.getDefault().getImageRegistry().get("icon");
            return null;
        }
        return null;
    }

    public String decorateText(String text, Object element) {
        if (text.startsWith(AWS_JAVA_SDK_CONTAINER_NAME)) {
            try {
                AwsClasspathContainer awsContainer = (AwsClasspathContainer) JavaCore.getClasspathContainer(((ClassPathContainer) element).getClasspathEntry().getPath(), ((ClassPathContainer) element).getJavaProject());
                return text + " [" + awsContainer.getVersion() + "]";
            } catch (JavaModelException e) {
                return null;
            }
        }
        return null;
    }

    public void addListener(ILabelProviderListener listener) { }

    public void dispose() { }

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public void removeListener(ILabelProviderListener listener) { }

}
