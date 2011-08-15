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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    private static final Set<String> watchedProperties = new HashSet<String>();
    static {
        watchedProperties.add(PreferenceConstants.P_ACCESS_KEY);
        watchedProperties.add(PreferenceConstants.P_ACCOUNT_IDS);
        watchedProperties.add(PreferenceConstants.P_CERTIFICATE_FILE);
        watchedProperties.add(PreferenceConstants.P_CURRENT_ACCOUNT);
        watchedProperties.add(PreferenceConstants.P_PRIVATE_KEY_FILE);
        watchedProperties.add(PreferenceConstants.P_SECRET_KEY);
        watchedProperties.add(PreferenceConstants.P_USER_ID);
    }

    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        String bareProperty = property.substring(property.indexOf(":") + 1);
        
        if ( watchedProperties.contains(bareProperty) ) {
            // We delay the job running so that any other preferences being
            // updated can complete. We don't want to fire immediately when
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
            for ( AccountInfoChangeListener listener : listeners ) {
                try {
                    listener.currentAccountChanged();
                } catch ( Exception e ) {
                    AwsToolkitCore.getDefault().logException(
                            "Couldn't notify listener of account change: " + listener.getClass(), e);
                }
            }
            return Status.OK_STATUS;
        }
    }
}
