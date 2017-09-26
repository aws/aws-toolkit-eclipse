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
package com.amazonaws.eclipse.lambda.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionDeploymentMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;

/**
 * This class decorates the Project Explorer entry for the AWS Lambda Runtime
 * classpath container
 */
public class LambdaFunctionProjectDecorator implements ILabelDecorator {

    @Override
    public Image decorateImage(Image image, Object element) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Decorate handler class java file with the remote AWS Lambda function name.
     */
    @Override
    public String decorateText(String text, Object element) {
        if (element instanceof ICompilationUnit) {
            ICompilationUnit compilationUnit = (ICompilationUnit) element;
            IProject project = compilationUnit.getJavaProject().getProject();

            if (project == null) {
                return null;
            }
            try {
                LambdaFunctionProjectMetadata md = ProjectMetadataManager.loadLambdaProjectMetadata(project);
                if (md == null) {
                    return null;
                }
                for (IType type : compilationUnit.getTypes()) {
                    if (md.getHandlerMetadata().containsKey(type.getFullyQualifiedName())) {
                        LambdaFunctionDeploymentMetadata deployment = md.getHandlerMetadata().get(type.getFullyQualifiedName())
                                .getDeployment();
                        return text + " [" + (deployment == null ? "" : deployment.getAwsLambdaFunctionName()) + "]";
                    }
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    @Override
    public void dispose() {
        // no-op
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // no-op
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // no-op
    }

}
