/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.egit.jobs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * A Job that imports a Git repository to the workbench. If it is Maven structured, import as a Maven project.
 */
@SuppressWarnings("restriction")
public class ImportProjectJob {

    private final String projectName;
    private final File destinationFile;
    private final Repository repository;

    public ImportProjectJob(String projectName, File destinationFile) {
        this.projectName = projectName;
        this.destinationFile = destinationFile;
        this.repository = getTargetRepository();
    }

    public IFile execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        importAsGeneralProject(monitor);

        IProject project = ResourcesPlugin.getWorkspace()
                .getRoot().getProject(projectName);

        if (pomFileExists(project)) {

            convertToMavenProject(project);

            return project.getFile(IMavenConstants.POM_FILE_NAME);
        } else {
            return null;
        }
    }

    private void importAsGeneralProject(IProgressMonitor monitor)
            throws InvocationTargetException {
        final String[] projectName = new String[1];
        final boolean[] defaultLocation = new boolean[1];
        final String[] path = new String[1];
        final File[] repoDir = new File[1];
        // get the data from the page in the UI thread
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                projectName[0] = ImportProjectJob.this.projectName;
                defaultLocation[0] = true;
                path[0] = repository.getWorkTree().getPath();
                repoDir[0] = repository.getDirectory();
            }
        });
        try {
            IWorkspaceRunnable wsr = new IWorkspaceRunnable() {
                @Override
                public void run(IProgressMonitor actMonitor)
                        throws CoreException {
                    final IProjectDescription desc = ResourcesPlugin
                            .getWorkspace().newProjectDescription(
                                    projectName[0]);
                    desc.setLocation(new Path(path[0]));

                    IProject prj = ResourcesPlugin.getWorkspace().getRoot()
                            .getProject(desc.getName());
                    prj.create(desc, actMonitor);
                    prj.open(actMonitor);
                    ConnectProviderOperation cpo = new ConnectProviderOperation(prj, repoDir[0]);
                    cpo.execute(new NullProgressMonitor());

                    ResourcesPlugin.getWorkspace().getRoot()
                            .refreshLocal(IResource.DEPTH_ONE, actMonitor);
                }
            };
            ResourcesPlugin.getWorkspace().run(wsr, monitor);
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        }
    }

    private Repository getTargetRepository() {
        try {
            return org.eclipse.egit.core.Activator
                    .getDefault()
                    .getRepositoryCache()
                    .lookupRepository(
                            new File(destinationFile, Constants.DOT_GIT));
        } catch (IOException e) {
            AwsToolkitCore.getDefault().reportException(
                    "Error looking up repository at "
                            + destinationFile, e);
            return null;
        }
    }

    private boolean pomFileExists(IProject project) {
        return project.getFile(IMavenConstants.POM_FILE_NAME).exists();
    }

    private void convertToMavenProject(final IProject project) throws InterruptedException {
        Job job = new Job("Enable Maven nature.") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    ResolverConfiguration configuration = new ResolverConfiguration();
                    configuration.setResolveWorkspaceProjects(true);
                    configuration.setSelectedProfiles(""); //$NON-NLS-1$

                    final boolean hasMavenNature = project
                            .hasNature(IMavenConstants.NATURE_ID);

                    IProjectConfigurationManager configurationManager = MavenPlugin
                            .getProjectConfigurationManager();

                    configurationManager.enableMavenNature(project,
                            configuration, monitor);

                    if (!hasMavenNature) {
                        configurationManager.updateProjectConfiguration(
                                project, monitor);
                    }
                } catch (CoreException ex) {
                    AwsToolkitCore.getDefault().reportException(ex.getMessage(), ex);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        job.join();
    }
}
