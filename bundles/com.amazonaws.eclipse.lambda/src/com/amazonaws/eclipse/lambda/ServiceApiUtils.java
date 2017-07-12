/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda;

import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;

public class ServiceApiUtils {

    public static final String JAVA_8 = "java8";

    private static final String LAMBDA_DOT_AMAZONAWS_DOT_COM = "lambda.amazonaws.com";

    public static List<FunctionConfiguration> getAllJavaFunctions(AWSLambda lambda) {

        List<FunctionConfiguration> allJavaFunctions = new LinkedList<>();
        String nextMarker = null;

        do {
            ListFunctionsResult result = lambda.listFunctions
                    (new ListFunctionsRequest()
                            .withMarker(nextMarker));

            List<FunctionConfiguration> functions = result.getFunctions();
            if (functions != null) {
                for (FunctionConfiguration function : functions) {
                    if (isJavaFunction(function)) {
                        allJavaFunctions.add(function);
                    }
                }
            }

            nextMarker = result.getNextMarker();

        } while (nextMarker != null);

        return allJavaFunctions;
    }

    /**
     * @return all the roles that are allowed to be assumed by AWS Lambda service
     */
    public static List<Role> getAllLambdaRoles(AmazonIdentityManagement iam) {

        List<Role> allRoles = new LinkedList<>();
        String nextMarker = null;

        do {
            ListRolesResult result = iam.listRoles(
                    new ListRolesRequest()
                            .withMarker(nextMarker));

            List<Role> roles = result.getRoles();
            if (roles != null) {
                for (Role role : roles) {
                    if (canBeAssumedByLambda(role)) {
                        allRoles.add(role);
                    }
                }
            }

            nextMarker = result.getMarker();

        } while (nextMarker != null);

        return allRoles;

    }

    private static boolean isJavaFunction(FunctionConfiguration function) {
        return JAVA_8.equals(function.getRuntime());
    }

    private static boolean canBeAssumedByLambda(Role role) {
        if (role.getAssumeRolePolicyDocument() == null) {
            return false;
        }

        Policy assumeRolePolicy;
        try {
            String policyJson = URLDecoder.decode(role.getAssumeRolePolicyDocument(), "UTF-8");
            assumeRolePolicy = Policy.fromJson(policyJson);
        } catch (Exception e) {
            LambdaPlugin.getDefault().logWarning(
                    "Failed to parse assume role policy for ["
                            + role.getRoleName() + "]. "
                            + role.getAssumeRolePolicyDocument(), e);
            return false;
        }

        for (Statement statement : assumeRolePolicy.getStatements()) {
            if (statement.getEffect() == Effect.Allow
                    && hasStsAssumeRoleAction(statement.getActions())
                    && hasLambdaServicePrincipal(statement.getPrincipals())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStsAssumeRoleAction(List<Action> actions) {
        if (actions == null) {
            return false;
        }
        for (Action action : actions) {
            if (action.getActionName().equalsIgnoreCase(
                    SecurityTokenServiceActions.AllSecurityTokenServiceActions
                            .getActionName())) {
                return true;
            }
            if (action.getActionName().equalsIgnoreCase(
                    SecurityTokenServiceActions.AssumeRole.getActionName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLambdaServicePrincipal(List<Principal> principals) {
        if (principals == null) {
            return false;
        }
        for (Principal principal : principals) {
            if (principal.equals(Principal.All)) {
                return true;
            }
            if (principal.equals(Principal.AllServices)) {
                return true;
            }
            if (principal.getProvider().equalsIgnoreCase("Service")
                    && principal.getId().equalsIgnoreCase(LAMBDA_DOT_AMAZONAWS_DOT_COM)) {
                return true;
            }
        }
        return false;
    }

}
