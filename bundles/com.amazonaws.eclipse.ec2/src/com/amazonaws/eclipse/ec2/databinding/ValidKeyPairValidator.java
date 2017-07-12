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
package com.amazonaws.eclipse.ec2.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairSelectionTable;
import com.amazonaws.services.ec2.model.KeyPairInfo;

public class ValidKeyPairValidator implements IValidator {

    private static final KeyPairManager keyPairManager = new KeyPairManager();
    private final String accountId;
    
    public ValidKeyPairValidator(String accountId) {
        super();
        this.accountId = accountId;
    }

    @Override
    public IStatus validate(Object value) {
        KeyPairInfo keyPair = (KeyPairInfo) value;
        if ( keyPair == null ) {
            return ValidationStatus.error("Select a valid key pair");
        } else if ( !keyPairManager.isKeyPairValid(accountId, keyPair.getKeyName()) ) {
            return ValidationStatus.error(KeyPairSelectionTable.INVALID_KEYPAIR_MESSAGE);
        } else {
            return ValidationStatus.ok();
        }
    }

}
