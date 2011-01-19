/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Responsible for monitoring for account info changes and notifying
 * listeners.
 */
final class AccountInfoMonitor implements IPropertyChangeListener {

    private List<AccountInfoChangeListener> listeners = new ArrayList<AccountInfoChangeListener>();

    private Job notifyCredentialListenersJob = new NotifyCredentialListenersJob();

    public void addAccountInfoChangeListener(AccountInfoChangeListener listener) {
        listeners.add(listener);
    }

    public void removeAccountInfoChangeListener(AccountInfoChangeListener listener) {
        listeners.remove(listener);
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(PreferenceConstants.P_ACCESS_KEY) ||
            event.getProperty().equals(PreferenceConstants.P_SECRET_KEY)) {
            // We delay the job running so that any other preferences being
            // updated can complete.  We don't want to fire immediately when
            // only the access key has changed and the secret key hasn't
            // been updated yet.
            notifyCredentialListenersJob.schedule(1000);
        }
    }

    private final class NotifyCredentialListenersJob extends Job {
        private NotifyCredentialListenersJob() {
            super("AWS credentials update");
            this.setSystem(true);
        }

        protected IStatus run(IProgressMonitor monitor) {
            for (AccountInfoChangeListener listener : listeners) {
                listener.currentAccountChanged();
            }
            return Status.OK_STATUS;
        }
    }
}
