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
