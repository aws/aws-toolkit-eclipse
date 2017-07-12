/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.identitymanagement.role;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Principal.Services;
import com.amazonaws.auth.policy.Principal.WebIdentityProviders;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;

public class CreateRoleWizard extends Wizard {
    private CreateRoleFirstPage firstPage;
    private CreateRoleSecondPage secondPage;
    private CreateRoleThirdPage thirdPage;
    private CreateRoleWizardDataModel dataModel;
    private AmazonIdentityManagement iam;
    private IRefreshable refreshable;

    public  CreateRoleWizard (AmazonIdentityManagement iam, IRefreshable refreshable) {
        setNeedsProgressMonitor(false);
        setWindowTitle("Create New Role");
        setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        dataModel = new CreateRoleWizardDataModel();
        this.iam = iam;
        if (iam == null) {
            this.iam = AwsToolkitCore.getClientFactory().getIAMClient();
        }
        this.refreshable = refreshable;
    }


    public CreateRoleWizard() {
        this(AwsToolkitCore.getClientFactory().getIAMClient(), null);
    }

    @Override
    public boolean performFinish() {
        final CreateRoleRequest createRoleRequest = new CreateRoleRequest();
        createRoleRequest.setAssumeRolePolicyDocument(getAssumeRolePolicyDoc());
        createRoleRequest.setRoleName(dataModel.getRoleName());

        final GetInstanceProfileRequest getInstanceProfileRequest = new GetInstanceProfileRequest();
        getInstanceProfileRequest.setInstanceProfileName(dataModel.getRoleName());

        final CreateInstanceProfileRequest createInstanceProfileRequest = new CreateInstanceProfileRequest();
        createInstanceProfileRequest.setInstanceProfileName(dataModel.getRoleName());

        final AddRoleToInstanceProfileRequest addRoleToInstanceProfileRequest = new AddRoleToInstanceProfileRequest();
        addRoleToInstanceProfileRequest.setInstanceProfileName(dataModel.getRoleName());
        addRoleToInstanceProfileRequest.setRoleName(dataModel.getRoleName());

        final PutRolePolicyRequest putRolePolicyRequest = generatePutPolicyRequest();

        new Job("Creating role") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {

                boolean hasProfile = true;

                try {
                    iam.createRole(createRoleRequest);
                    if (putRolePolicyRequest != null) {
                        iam.putRolePolicy(putRolePolicyRequest);
                    }

                    try {
                    iam.getInstanceProfile(getInstanceProfileRequest);
                    } catch (Exception e) {
                        hasProfile = false;
                    }

                    if (hasProfile == false) {
                        iam.createInstanceProfile(createInstanceProfileRequest);
                        iam.addRoleToInstanceProfile(addRoleToInstanceProfileRequest);
                    }

                    if (refreshable != null) {
                        refreshable.refreshData();
                    }

                    return Status.OK_STATUS;
                } catch (Exception e) {
                    return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to create the role: " + e.getMessage(), e);
                }
            }
        }.schedule();

        return true;
    }

    @Override
    public void addPages() {
        firstPage = new CreateRoleFirstPage(this);
        secondPage = new CreateRoleSecondPage(this);
        thirdPage = new CreateRoleThirdPage(this);
        addPage(firstPage);
        addPage(secondPage);
        addPage(thirdPage);
    }

    private PutRolePolicyRequest generatePutPolicyRequest() {
        if (!dataModel.getGrantPermission()) {
            return null;
        } else {
            return new PutRolePolicyRequest()
                  .withRoleName(dataModel.getRoleName())
                  .withPolicyDocument(dataModel.getPolicyDoc())
                  .withPolicyName(dataModel.getPolicyName());
        }
    }

    public CreateRoleWizardDataModel getDataModel() {
        return dataModel;
    }

    private String getAssumeRolePolicyDoc() {
        Policy assumeRolePolicy = new Policy();
        Principal principal = null;
        if (dataModel.getServiceRoles()) {
            if (dataModel.getService().startsWith("Amazon EC2")) {
                principal = new Principal(Services.AmazonEC2);
            } else if (dataModel.getService().startsWith("AWS Data Pipeline")) {
                principal = new Principal(Services.AWSDataPipeline);
            } else if (dataModel.getService().startsWith("AWS OpsWorks")) {
                principal = new Principal(Services.AWSOpsWorks);
            } else if (dataModel.getService().startsWith("Amazon EC2 Role for Data Pipeline")) {
                principal = new Principal(Services.AmazonEC2);
            } else {
                principal = new Principal(Services.AmazonElasticTranscoder);
            }
        } else if (dataModel.getAccountRoles()) {
            principal = new Principal(dataModel.getAccountId());
        } else if (dataModel.getWebProviderRoles()) {
              if (dataModel.getWebProvider().equals("Facebook")) {
                  principal = new Principal(WebIdentityProviders.Facebook);
              } else if (dataModel.getWebProvider().equals("Google")) {
                  principal = new Principal(WebIdentityProviders.Google);
              } else {
                  principal = new Principal(WebIdentityProviders.Amazon);
              }
        } else {
            principal = new Principal(dataModel.getInternalAccountId());
        }

        Statement statement = new Statement(Effect.Allow);
        statement.setPrincipals(Arrays.asList(principal));

        Condition condition = generateCondition();
        if (condition != null) {
            statement.setConditions(Arrays.asList(condition));
        }

        Action action = generateAction();
        statement.setActions(Arrays.asList(action));

        assumeRolePolicy.setStatements(Arrays.asList(statement));
        return assumeRolePolicy.toJson();

    }

    private Condition generateCondition() {
        Condition condition = null;
        if (dataModel.getWebProviderRoles()) {
            condition = new Condition();
            condition.setType("StringEquals");
            if (dataModel.getWebProvider().equals("Facebook")) {
                condition.setConditionKey("graph.facebook.com:app_id");
            } else if (dataModel.getWebProvider().equals("Google")) {
                condition.setConditionKey("accounts.google.com:aud");
            } else {
                condition.setConditionKey("www.amazon.com:app_id");
            }

            List<String> value = new LinkedList<>();
            value.add(dataModel.getApplicationId());
            condition.setValues(value);
        } else if (dataModel.getThirdPartyRoles()) {
            if (dataModel.getThirdPartyRoles()) {
                condition = new Condition();
                condition.setType("StringEquals");
                condition.setConditionKey("sts:ExternalId");
                List<String> value = new LinkedList<>();
                value.add(dataModel.getExternalAccountId());
                condition.setValues(value);
             }
        }

        return condition;
    }

    private Action generateAction() {

        Action action = null;
        if (dataModel.getWebProviderRoles()) {
            action = SecurityTokenServiceActions.AssumeRoleWithWebIdentity;
        } else {
            action = SecurityTokenServiceActions.AssumeRole;
        }
        return action;
    }

}
