package com.amazonaws.eclipse.lambda.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;

/**
 * This class decorates the Project Explorer entry for the AWS Lambda Runtime
 * classpath container
 */
public class LambdaFunctionProjectDecorator implements ILabelDecorator {

    public Image decorateImage(Image image, Object element) {
        // TODO Auto-generated method stub
        return null;
    }

    public String decorateText(String text, Object element) {

        IProject project = null;
        if (element instanceof IProject) {
            project = (IProject)element;
        } else if (element instanceof IJavaProject) {
            project = ((IJavaProject)element).getProject();
        }

        if (project != null) {
            try {
                LambdaFunctionProjectMetadata md = FunctionProjectUtil
                        .loadLambdaProjectMetadata(project);
                if (md != null && md.isValid()) {
                    return text + " [" + md.getLastDeploymentFunctionName() + "]";
                }
            } catch (Exception e) {
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
