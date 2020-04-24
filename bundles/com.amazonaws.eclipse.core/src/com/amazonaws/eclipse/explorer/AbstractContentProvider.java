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
package com.amazonaws.eclipse.explorer;

import static com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType.EXPLORER_LOADING;
import static com.amazonaws.eclipse.core.telemetry.ToolkitAnalyticsUtils.publishBooleansEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * Abstract base class for AWS Explorer content providers. This class provides
 * basic implementations for ContentProvider methods as well as handling
 * refreshing the content when the current account or region changes. This class
 * also handles caching and returning results for previously loaded data.
 */
public abstract class AbstractContentProvider implements ITreeContentProvider, IRefreshable {

    /** Reference to the TreeViewer in which content will be displayed. */
    protected TreeViewer viewer;

    /** Cache for previously loaded data */
    protected Map<Object, Object[]> cachedResponses = new ConcurrentHashMap<>();

    protected BackgroundContentUpdateJobFactory backgroundJobFactory;

    /**
     * Creates a new AbstractContentProvider and registers it with the registry
     * of AWS Explorer ContentProviders.
     */
    public AbstractContentProvider() {
        ContentProviderRegistry.registerContentProvider(this);
    }

    /**
     * Loads the children for the specified parent element. Subclasses must
     * implement this method, and should use {@link DataLoaderThread}
     * implementations to load any remote AWS data asynchronously. Caching is
     * handling by {@link AbstractContentProvider}, so this method will only be
     * invoked if no data was found in the cache and remote data needs to be
     * loaded.
     * <p>
     * Subclasses should implement this method and <b>not</b> the getElement or
     * getChildren methods.
     *
     * @param parentElement
     *            The parent element indicating what child data needs to be
     *            loaded.
     *
     * @return The child elements for the specified parent, or null if they are
     *         being loaded asynchronously, and the super class will handle
     *         displaying a loading message.
     */
    public abstract Object[] loadChildren(Object parentElement);

    /**
     * Returns the service abbreviation uniquely identifying the service the
     * ContentProvider subclass is working with.
     *
     * @see ServiceAbbreviations
     *
     * @return the service abbreviation uniquely identifying the service the
     *         ContentProvider subclass is working with.
     */
    public abstract String getServiceAbbreviation();

    /**
     * Thread to asynchronously load data for an AWS Explorer ContentProvider.
     * This class takes care of several error cases, such as not being signed up
     * for a service yet and handles them correctly so that subclasses don't
     * have to worry about. Subclasses simply need to implement the loadData()
     * method to return their specific data.
     *
     * This class also takes care of storing the returned results from
     * loadData() into the ContentProvider's cache.
     */
    protected abstract class DataLoaderThread extends Thread {
        private final Object parentElement;

        /** Various AWS error codes indicating that a developer isn't signed up yet. */
        private final List<String> NOT_SIGNED_UP_ERROR_CODES =
            Arrays.asList("NotSignedUp", "SubscriptionCheckFailed", "OptInRequired");

        public DataLoaderThread(Object parentElement) {
            this.parentElement = parentElement;
        }

        /**
         * Returns the data being loaded by this thread, which is specific to
         * each individual ContentProvider (ex: SimpleDB domains, EC2 instances,
         * etc).
         *
         * @return The loaded data (ex: an array of EC2 instances, or SimpleDB
         *         domains, etc).
         */
        public abstract Object[] loadData();

        @Override
        public final void run() {
            try {
                cachedResponses.put(parentElement, loadData());
                if ( null != backgroundJobFactory ) {
                    backgroundJobFactory.startBackgroundContentUpdateJob(parentElement);
                }
            } catch (Exception e) {
                if ( e instanceof AmazonServiceException
                        && NOT_SIGNED_UP_ERROR_CODES.contains(((AmazonServiceException) e).getErrorCode()) ) {
                    cachedResponses.put(parentElement,
                            new Object[] { new NotSignedUpNode(((AmazonServiceException) e).getServiceName()) });
                } else {
                    cachedResponses.put(parentElement, new Object[] { new UnableToConnectNode() });
                }

                AwsToolkitCore.getDefault().logWarning("Error loading explorer data", e);
            }

            Display.getDefault().syncExec(new RefreshRunnable(viewer, parentElement));
        }
    }

    /**
     * Clears all cached responses and reinitializes the tree.
     */
    public synchronized void refresh() {
        this.cachedResponses.clear();

        Object[] children = this.getChildren(new AWSResourcesRootElement());
        if (children.length == 0) {
            return;
        }
        Object tempObject = null;
        if (children.length == 1) {
            tempObject = children[0];
        }

        final Object rootElement = tempObject;

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                viewer.getTree().deselectAll();
                viewer.refresh(rootElement);
                viewer.expandToLevel(1);
            }
        });

    }

    @Override
    public void refreshData() {
        refresh();
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public final Object[] getChildren(final Object parentElement) {
        if (!RegionUtils.isServiceSupportedInCurrentRegion(getServiceAbbreviation())) {
            return new Object[0];
        }

        if ( cachedResponses.containsKey(parentElement)) {
            if ( null != backgroundJobFactory ) {
                backgroundJobFactory.startBackgroundContentUpdateJob(parentElement);
            }

            if (!(parentElement instanceof AWSResourcesRootElement)) {
                publishBooleansEvent(EXPLORER_LOADING, parentElement.getClass().getSimpleName(), true);
            }

            return cachedResponses.get(parentElement);
        }

        if (!AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
            return new Object[0];
        }

        Object[] children = loadChildren(parentElement);

        if (children == null) {
            children = Loading.LOADING;
        }

        return children;
    }

    @Override
    public void dispose() {
        ContentProviderRegistry.unregisterContentProvider(this);
    }

    public void clearCachedResponse() {
        cachedResponses.clear();
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        this.viewer = (TreeViewer) viewer;
    }

    /**
     * Sets a background job factory which will create Job and execute it
     * periodically to update the content. The underlying Job object will be
     * scheduled whenever {@link AbstractContentProvider#getChildren(Object)}
     * method is called.
     */
    public void setBackgroundJobFactory(BackgroundContentUpdateJobFactory backgroundJobFactory) {
        this.backgroundJobFactory = backgroundJobFactory;
    }

    /** ExplorerNode alerting the user that they need to sign up for a service. */
    private static class NotSignedUpNode extends ExplorerNode {

        private static final class SignUpAction extends Action {
            public SignUpAction() {
                super.setText("Sign up");
                super.setImageDescriptor(AwsToolkitCore
                    .getDefault()
                    .getImageRegistry()
                    .getDescriptor(AwsToolkitCore.IMAGE_EXTERNAL_LINK)
                );
            }

            @Override
            public void run() {
                BrowserUtils.openExternalBrowser("https://aws-portal.amazon.com/gp/aws/developer/registration");
            }
        }

        public NotSignedUpNode(final String serviceName) {
            super(serviceName + " (not signed up)",
                  0,
                  loadImage(AwsToolkitCore.IMAGE_EXTERNAL_LINK),
                  new SignUpAction());
        }
    }

    /** ExplorerNode alerting users that we couldn't connect to AWS. */
    private static class UnableToConnectNode extends ExplorerNode {
        public UnableToConnectNode() {
            super("Unable to connect",
                  0,
                  loadImage(AwsToolkitCore.IMAGE_AWS_ICON));
        }
    }

    protected abstract class BackgroundContentUpdateJobFactory {

        private Map<Object, Job> backgroundJobs = new ConcurrentHashMap<>();

        /**
         * Defines the behavior of the background job. Returned boolean values
         * indicates whether the background should keep running.
         */
        protected abstract boolean executeBackgroundJob(Object parentElement) throws AmazonClientException;

        /**
         * This method will be used for setting the delay between subsequent
         * execution of the background job.
         */
        protected abstract long getRefreshDelay();

        private Job getBackgroundJobByParentElement(Object parentElement) {
            return backgroundJobs.get(parentElement);
        }

        public synchronized void startBackgroundContentUpdateJob(final Object parentElement) {
            Job currentJob = getBackgroundJobByParentElement(parentElement);
            if ( currentJob == null ) {
                /*
                 * Initiates a new background job for asynchronously updating the
                 * content. (e.g. updating TableNode status)
                 */
                final Job newJob = new Job("Updating contents") {

                    private Object updatedParentElement = parentElement;

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            if ( executeBackgroundJob(updatedParentElement) ) {
                                /* Reschedule the job after some delay */
                                this.schedule(getRefreshDelay());
                            } else {
                                /* If the background job has already finished its work,
                                 * remove the parent element from the map. */
                                backgroundJobs.remove(updatedParentElement);
                            }
                        } catch (AmazonClientException e) {
                            return new Status(Status.ERROR, AwsToolkitCore.getDefault().getPluginId(),
                                    "Unable to update the content: " + e.getMessage(), e);
                        }

                        return Status.OK_STATUS;
                    }

                };
                newJob.schedule();
                backgroundJobs.put(parentElement, newJob);
            } else {
                /* Restart the current job */
                currentJob.cancel();
                currentJob.schedule();
            }
        }
    }

}
