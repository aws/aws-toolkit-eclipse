package ${packageName};

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import ${inputFqcn};
import ${outputFqcn};

public class ${className} implements RequestHandler<ServerlessInput, ServerlessOutput> {

    @Override
    public ServerlessOutput handleRequest(ServerlessInput serverlessInput, Context context) {
        // implement your Serverless Lambda function here
        return null;
    }
}