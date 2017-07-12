/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.amazonaws.eclipse.core.egit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Data class representing selection of remote repository made by user.
 * Selection is either a URI or a remote repository configuration.
 * <p>
 * Each immutable instance has at least one of two class fields (URI, remote
 * config) set to null. null value indicates that it has illegal value or this
 * form of repository selection is not selected.
 */
public class RepositorySelection {
    private URIish uri;

    private RemoteConfig config;

    static final RepositorySelection INVALID_SELECTION = new RepositorySelection(
            null, null);

    /**
     * @param uri
     *            the new specified URI. null if the new URI is invalid or user
     *            chosen to specify repository as remote config instead of URI.
     * @param config
     *            the new remote config. null if user chosen to specify
     *            repository as URI.
     */
    public RepositorySelection(final URIish uri, final RemoteConfig config) {
        if (config != null && uri != null)
            throw new IllegalArgumentException(
                    "URI and config cannot be set at the same time."); //$NON-NLS-1$
        this.config = config;
        this.uri = uri;
    }

    /**
     * Return the selected URI.
     * <p>
     * If pushMode is <code>true</code> and a remote configuration was selected,
     * this will try to return a push URI from that configuration, otherwise a
     * URI; if no configuration was selected, the URI entered in the URI field
     * will be returned.<br>
     * If pushMode is <code>false</code> and a remote configuration was
     * selected, this will try to return a URI from that configuration,
     * otherwise <code>null</code> will be returned; if no configuration was
     * selected, the URI entered in the URI field will be returned
     *
     * @param pushMode
     *            the push mode
     * @return the selected URI, or <code>null</code> if there is no valid
     *         selection
     */
    public URIish getURI(boolean pushMode) {
        if (isConfigSelected())
            if (pushMode) {
                if (config.getPushURIs().size() > 0)
                    return config.getPushURIs().get(0);
                else if (config.getURIs().size() > 0)
                    return config.getURIs().get(0);
                else
                    return null;
            } else {
                if (config.getURIs().size() > 0)
                    return config.getURIs().get(0);
                else if (config.getPushURIs().size() > 0)
                    return config.getPushURIs().get(0);
                else
                    return null;
            }
        return uri;
    }

    /**
     * @return the selected URI, <code>null</code> if a configuration was
     *         selected
     */
    public URIish getURI() {
        if (isConfigSelected())
            return null;
        return uri;
    }

    /**
     * @return list of all push URIs - either the one specified as custom URI or
     *         all push URIs of the selected configuration; if not push URIs
     *         were specified, the first URI is returned
     */
    public List<URIish> getPushURIs() {
        if (isURISelected())
            return Collections.singletonList(uri);
        if (isConfigSelected()) {
            List<URIish> pushUris = new ArrayList<>();
            pushUris.addAll(config.getPushURIs());
            if (pushUris.isEmpty())
                pushUris.add(config.getURIs().get(0));
            return pushUris;
        }
        return null;
    }

    /**
     * @return the selected remote configuration. null if user chosen to select
     *         repository as URI.
     */
    public RemoteConfig getConfig() {
        return config;
    }

    /**
     * @return selected remote configuration name or null if selection is not a
     *         remote configuration.
     */
    public String getConfigName() {
        if (isConfigSelected())
            return config.getName();
        return null;
    }

    /**
     * @return true if selection contains valid URI or remote config, false if
     *         there is no valid selection.
     */
    public boolean isValidSelection() {
        return uri != null || config != null;
    }

    /**
     * @return true if user selected valid URI, false if user selected invalid
     *         URI or remote config.
     */
    public boolean isURISelected() {
        return uri != null;
    }

    /**
     * @return true if user selected remote configuration, false if user
     *         selected (invalid or valid) URI.
     */
    public boolean isConfigSelected() {
        return config != null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof RepositorySelection) {
            final RepositorySelection other = (RepositorySelection) obj;
            if (uri == null ^ other.uri == null)
                return false;
            if (uri != null && !uri.equals(other.uri))
                return false;

            if (config != other.config)
                return false;

            return true;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        if (uri != null)
            return uri.hashCode();
        else if (config != null)
            return config.hashCode();
        return 31;
    }
}