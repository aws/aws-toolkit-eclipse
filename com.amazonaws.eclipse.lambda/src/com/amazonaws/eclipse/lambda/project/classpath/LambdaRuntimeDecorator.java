/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.classpath;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * This class decorates the Project Explorer entry for the AWS Lambda Runtime
 * classpath container
 */
public class LambdaRuntimeDecorator implements ILabelDecorator {

    public Image decorateImage(Image image, Object element) {
        // TODO Auto-generated method stub
        return null;
    }

    public String decorateText(String text, Object element) {
        if (element instanceof ClassPathContainer
                && text.startsWith(LambdaRuntimeClasspathContainer.DESCRIPTION)) {
            ClassPathContainer classpathContainer = (ClassPathContainer)element;
            try {
                LambdaRuntimeClasspathContainer runtimeContainer = (LambdaRuntimeClasspathContainer) JavaCore.getClasspathContainer(
                        classpathContainer.getClasspathEntry().getPath(),
                        classpathContainer.getJavaProject());
                return text + " [" + runtimeContainer.getVersion() + "]";
            } catch (JavaModelException e) {
                return null;
            }
        }
        return null;
    }

    public void dispose() {
        // no-op
    }

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public void addListener(ILabelProviderListener listener) {
        // no-op
    }

    public void removeListener(ILabelProviderListener listener) {
        // no-op
    }

}
