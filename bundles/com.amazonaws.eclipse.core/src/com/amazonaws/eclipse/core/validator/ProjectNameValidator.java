/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.validator;

import java.io.IOException;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.util.StringUtils;

/**
 * Project name validator. It validates project name that not null or empty,
 * a valid naming without unsupported characters, does not exist neither in
 * the workspace nor in the underlying root folder.
 */
public class ProjectNameValidator implements IValidator {

    @Override
    public IStatus validate(Object value) {
        String name = (String)value;

        final IWorkspace workspace= ResourcesPlugin.getWorkspace();

        if (StringUtils.isNullOrEmpty(name)) {
            return ValidationStatus.error("The project name must be provided!");
        }

        String errorMessage = checkProjectNameValid(workspace, name);
        if (errorMessage != null) {
            return ValidationStatus.error(errorMessage);
        }

        errorMessage = checkProjectAlreadyExist(workspace, name);
        if (errorMessage != null) {
            return ValidationStatus.error(errorMessage);
        }

        errorMessage = checkProjectPathAlreadyExist(workspace, name);
        if (errorMessage != null) {
            return ValidationStatus.error(errorMessage);
        }

        return ValidationStatus.ok();
    }

    private String checkProjectNameValid(IWorkspace workspace, String projectName) {
        final IStatus nameStatus= workspace.validateName(projectName, IResource.PROJECT);
        if (!nameStatus.isOK()) {
            return nameStatus.getMessage();
        }
        return null;
    }

    private String checkProjectAlreadyExist(IWorkspace workspace, String projectName) {
        final IProject handle = workspace.getRoot().getProject(projectName);
        if (handle.exists()) {
            return "A project with this name already exists.";
        }
        return null;
    }

    private String checkProjectPathAlreadyExist(IWorkspace workspace, String projectName) {
        IPath projectLocation= workspace.getRoot().getLocation().append(projectName);
        if (projectLocation.toFile().exists()) {
            try {
                //correct casing
                String canonicalPath= projectLocation.toFile().getCanonicalPath();
                projectLocation= new Path(canonicalPath);
            } catch (IOException e) {
                AwsToolkitCore.getDefault().logError(e.getMessage(), e);
            }

            String existingName= projectLocation.lastSegment();
            if (!existingName.equals(projectName)) {
                return "The name of the new project must be " + existingName;
            }
        }
        return null;
    }
}
