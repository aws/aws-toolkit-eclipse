package com.amazonaws.eclipse.lambda;

import java.util.LinkedList;
import java.util.List;

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

    public static List<FunctionConfiguration> getAllJavaFunctions(AWSLambda lambda) {

        List<FunctionConfiguration> allJavaFunctions = new LinkedList<FunctionConfiguration>();
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

    public static List<Role> getAllRoles(AmazonIdentityManagement iam) {

        List<Role> allRoles = new LinkedList<Role>();
        String nextMarker = null;

        do {
            ListRolesResult result = iam.listRoles(
                    new ListRolesRequest()
                            .withMarker(nextMarker));

            List<Role> roles = result.getRoles();
            if (roles != null) {
                allRoles.addAll(roles);
            }

            nextMarker = result.getMarker();

        } while (nextMarker != null);

        return allRoles;

    }

    private static boolean isJavaFunction(FunctionConfiguration function) {
        return JAVA_8.equals(function.getRuntime());
    }

}
