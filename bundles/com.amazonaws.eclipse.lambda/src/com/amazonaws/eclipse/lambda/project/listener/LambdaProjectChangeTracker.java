/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.listener;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

/**
 * Responsible for tracking local changes to all the projects in the current
 * workspace
 */
public class LambdaProjectChangeTracker {

    private final ConcurrentHashMap<IProject, Boolean> projectDirtyMap = new ConcurrentHashMap<>();

    private final IElementChangedListener projectChangeListener = new LambdaProjectElementChangeListener();

    public void start() {
        JavaCore.addElementChangedListener(projectChangeListener,
                ElementChangedEvent.POST_CHANGE);
    }

    public void stop() {
        JavaCore.removeElementChangedListener(projectChangeListener);
    }

    public void clearDirtyFlags() {
        projectDirtyMap.clear();
    }

    public boolean isProjectDirty(IJavaProject project) {
        return isProjectDirty(project.getProject());
    }

    /**
     * @return true if the project is marked as dirty or if the dirty flag is
     *         never set for this project.
     */
    public boolean isProjectDirty(IProject project) {
        Boolean dirtyFlag =  projectDirtyMap.get(project);
        if (dirtyFlag == null) {
            return true;
        }
        return dirtyFlag;
    }

    public void markProjectAsNotDirty(IProject project) {
        updateProjectDirtyFlag(project, false);
    }

    private void markProjectAsDirty(IProject project) {
        updateProjectDirtyFlag(project, true);
    }

    private void markProjectAsDirty(IJavaProject project) {
        markProjectAsDirty(project.getProject());
    }

    private void updateProjectDirtyFlag(IProject project, boolean dirty) {
        projectDirtyMap.put(project, dirty);

        LambdaPlugin.getDefault().trace(
                "Project [" + project.getName() + "] dirty flag set to "
                        + dirty);
    }

    /**
     * A JDT element change listener implementation that marks a project (and
     * its upstream dependencies) as dirty whenever an element change is
     * detected in the project.
     */
    private class LambdaProjectElementChangeListener implements
            IElementChangedListener {

        @Override
        public void elementChanged(ElementChangedEvent event) {

            JavaElementDeltaAcceptor.accept(event.getDelta(),
                    new JavaElementDeltaAcceptor.Visitor() {
                        @Override
                        protected boolean visit(IJavaProject project) {
                            markProjectAsDirty(project);

                            for (IProject dependent : project.getProject()
                                    .getReferencingProjects()) {
                                markProjectAsDirty(dependent);
                            }
                            return true;
                        }
                    });
        }

    }
}
