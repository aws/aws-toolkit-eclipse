package com.amazonaws.eclipse.lambda.upload.wizard.dialog;

import java.util.UUID;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.Role;

public class CreateBasicLambdaRoleDialog extends AbstractInputDialog {

    private static final String BASIC_ROLE_POLICY =
            "{" +
                "\"Version\": \"2012-10-17\"," +
                "\"Statement\": [" +
                    "{" +
                        "\"Effect\": \"Allow\"," +
                        "\"Action\": [" +
                            "\"logs:*\"" +
                        "]," +
                        "\"Resource\": \"arn:aws:logs:*:*:*\"" +
                    "}" +
                "]" +
            "}";

    private static final String ASSUME_ROLE_POLICY =
            "{" +
                "\"Version\": \"2012-10-17\"," +
                "\"Statement\": [" +
                    "{" +
                        "\"Sid\": \"\"," +
                        "\"Effect\": \"Allow\"," +
                        "\"Principal\": {" +
                            "\"Service\": \"lambda.amazonaws.com\"" +
                        "}," +
                        "\"Action\": \"sts:AssumeRole\"" +
                    "}" +
                "]" +
            "}";

    private Role createdRole;

    public CreateBasicLambdaRoleDialog(Shell parentShell) {
        super(
                parentShell,
                "Create Role",
                "Create a basic IAM role that allows Lambda Function to call AWS services on your behalf.",
                "Creating the Role...",
                "Role Name:",
                "lambda_basic_execution");
    }

    public Role getCreatedRole() {
        return createdRole;
    }

    @Override
    protected void performFinish(String input) {
        AmazonIdentityManagement iam = AwsToolkitCore.getClientFactory()
                .getIAMClient();

        CreateRoleResult result = iam.createRole(new CreateRoleRequest()
                .withRoleName(input)
                .withAssumeRolePolicyDocument(ASSUME_ROLE_POLICY));

        createdRole = result.getRole();

        String policyArn = iam.createPolicy(
                new CreatePolicyRequest()
                        .withPolicyName(getRandomPolicyName())
                        .withPolicyDocument(BASIC_ROLE_POLICY)
                 ).getPolicy().getArn();

        iam.attachRolePolicy(new AttachRolePolicyRequest()
                .withRoleName(input)
                .withPolicyArn(policyArn));

        // Sleep for 10 seconds so that the policy change can be fully populated

        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                setMessage("Role created. Waiting for the attached role policy to be fully available...");
            }
        });
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getRandomPolicyName() {
        return "lambda_basic_execution_role_policy_"
                + UUID.randomUUID().toString();
    }

}
