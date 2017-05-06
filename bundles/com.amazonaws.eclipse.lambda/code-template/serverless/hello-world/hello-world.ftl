package ${packageName};

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Lambda function that simply prints "Hello World" if the input String is not provided,
 * otherwise, print "Hello " with the provided input String.
 */
public class ${className} implements RequestHandler<String, String> {
    @Override
    public String handleRequest(String input, Context context) {
        String output = "Hello " + ((input != null && !input.isEmpty()) ? input : "World");
        context.getLogger().log(output);
        return output;
    }
}