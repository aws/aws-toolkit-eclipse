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
package com.amazonaws.eclipse.core.ui.dialogs;

import java.util.Arrays;

import org.eclipse.swt.widgets.Shell;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.model.SelectOrCreateKmsKeyDataModel.KmsKey;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.amazonaws.services.kms.model.OriginType;

public class CreateKmsKeyDialog extends AbstractInputDialog<KmsKey> {

    private final AwsResourceScopeParamBase param;
    private KmsKey kmsKey;

    public CreateKmsKeyDialog(Shell parentShell, AwsResourceScopeParamBase param) {
        super(parentShell,
                "Create KMS Key",
                "Create a KMS Key in " + RegionUtils.getRegion(param.getRegionId()).getName(),
                "Creating the KMS Key...",
                "KMS Key Alias Name:",
                "lambda-function-kms-key");
        this.param = param;
    }

    @Override
    protected void performFinish(String input) {
        AWSKMS kmsClient = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getKmsClientByRegion(param.getRegionId());
        AmazonIdentityManagement iamClient = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getIAMClientByRegion(param.getRegionId());

        User iamUser = iamClient.getUser().getUser();
        Policy policy = new Policy();
        Statement statement = new Statement(Effect.Allow);
        statement.setPrincipals(new Principal(iamUser.getArn()));
        statement.setActions(Arrays.asList(KMSActions.AllKMSActions));
        statement.setResources(Arrays.asList(new Resource("*")));
        policy.setStatements(Arrays.asList(statement));

        KeyMetadata keyMetadata = kmsClient.createKey(new CreateKeyRequest()
                .withKeyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .withOrigin(OriginType.AWS_KMS)
                .withPolicy(policy.toJson()))
            .getKeyMetadata();

        input = KmsKey.KMS_KEY_ALIAS_PREFIX + input;
        kmsClient.createAlias(new CreateAliasRequest()
                .withAliasName(input)
                .withTargetKeyId(keyMetadata.getKeyId()));
        KeyListEntry key = new KeyListEntry()
                .withKeyId(keyMetadata.getKeyId())
                .withKeyArn(keyMetadata.getArn());
        AliasListEntry alias = new AliasListEntry()
                .withAliasName(input)
                .withTargetKeyId(keyMetadata.getKeyId());
        kmsKey = new KmsKey(key, alias);
    }

    @Override
    public KmsKey getCreatedResource() {
        return kmsKey;
    }
}
