/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.ui.keypair;

/**
 * Simple interface to notify interested parties that the set of keys
 * displayed in a {@link KeyPairSelectionTable} has changed.
 */
public interface KeyPairRefreshListener {     
    
    /**
     * Notification that the set of key pairs displayed by a
     * {@link KeyPairSelectionTable} has been refreshed.
     */
    public void keyPairsRefreshed();
}
