/*
 * Copyright 2010-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.preferences;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Abstract base class for monitors for changes on a set of preference properties and
 * notifying listeners.
 */
public abstract class AbstractPreferencePropertyMonitor implements IPropertyChangeListener {

    private List<PreferencePropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    private final long notificationDelay;
    private NotifyListenersJob job = null;

    /**
     * Create a preference monitor with the given notification delay
     * 
     * @param notificationDelay
     *            If it's a positive value, then all notifications on the
     *            registered listeners will be executed asynchronously after the
     *            specified delay. Otherwise, the notification happens
     *            synchronously right after change events
     */
    public AbstractPreferencePropertyMonitor(long notificationDelay) {
        this.notificationDelay = notificationDelay;
        if (notificationDelay > 0) {
            job = new NotifyListenersJob();
        }
    }

    /**
     * Override this method to determine whether changes to the given preference
     * key should be watched by this monitor.
     */
    protected abstract boolean watchPreferenceProperty(String preferenceKey);

    public void addChangeListener(PreferencePropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(PreferencePropertyChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();

        if (watchPreferenceProperty(property)) {
            if (job == null) {
                notifyListeners();
            } else {
                job.schedule(notificationDelay);
            }
        }
    }
    
    private void notifyListeners() {
        for ( PreferencePropertyChangeListener listener : listeners ) {
            try {
                listener.watchedPropertyChanged();
            } catch ( Exception e ) {
                AwsToolkitCore.getDefault().logError(
                        "Couldn't notify listener of preferece property change: " + listener.getClass(), e);
            }
        }
    }
    
    private final class NotifyListenersJob extends Job {
        private NotifyListenersJob() {
            super("AWS preference property update");
            this.setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            AbstractPreferencePropertyMonitor.this.notifyListeners();
            return Status.OK_STATUS;
        }
    }
}
