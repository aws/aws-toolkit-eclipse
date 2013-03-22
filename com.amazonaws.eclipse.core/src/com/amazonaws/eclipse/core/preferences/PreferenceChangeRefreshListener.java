/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * Responsible for listening to a specified preference store for property change
 * events, and if one of the preferences specified in the constructor is
 * encountered, the associated IRefreshable object will be refreshed.
 */
public class PreferenceChangeRefreshListener implements IPropertyChangeListener {

	/** The object to refresh when a preference changes */
	private final IRefreshable refreshable;

	/**
	 * The set of preferences that require the IRefreshable target of this
	 * listener to be refreshed.
	 */
	private final Set<String> refreshablePreferenceSet = new HashSet<String>();

	/**
	 * The preference store to which this preference change listener is listening.
	 */
	private final IPreferenceStore preferenceStore;

	/**
	 * Constructs a new preference change refresh listener that will refresh the
	 * specified {@code IRefreshable} object when it receives a notification
	 * that one of the specified preference properties changed on the specified
	 * preference store.
	 *
	 * @param refreshable
	 *            The object to be refreshed when the specified preferences are
	 *            changed.
	 * @param preferenceStore
	 *            The preference this listener is listening to.
	 * @param refreshablePreferences
	 *            The set of preferences that require the specified IRefreshable
	 *            object to be refreshed.
	 */
	public PreferenceChangeRefreshListener(IRefreshable refreshable, IPreferenceStore preferenceStore, String[] refreshablePreferences) {
		this.refreshable = refreshable;
		this.preferenceStore = preferenceStore;

		for (String property : refreshablePreferences) {
			refreshablePreferenceSet.add(property);
		}

		preferenceStore.addPropertyChangeListener(this);
	}

	/**
	 * Deactivates this preference change refresh listener and makes it stop
	 * listening for preference changes on the preference store that was
	 * specified in the constructor. Clients should be sure to call this method
	 * with they are done using a preference change refresh listener so that it
	 * doesn't hang around in memory and continue receiving notifications when
	 * preferences change.
	 */
	public void stopListening() {
		preferenceStore.removePropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		/*
		 * If the preference that changed isn't one that we're interested in
		 * refreshing on, just bail out.
		 */
		if (!refreshablePreferenceSet.contains(event.getProperty())) return;

		refreshable.refreshData();
	}

}
