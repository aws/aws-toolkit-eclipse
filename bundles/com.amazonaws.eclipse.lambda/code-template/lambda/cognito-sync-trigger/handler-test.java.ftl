<#if packageName?has_content>
package ${packageName};
</#if>

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoEvent;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ${handlerTestClassName} {

    private CognitoEvent input;

    @Before
    public void createInput() throws IOException {
        input = TestUtils.parse("/${inputJsonFileName}", CognitoEvent.class);
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
    public void test${handlerClassName}() {
        ${handlerClassName} handler = new ${handlerClassName}();
        Context ctx = createContext();

        CognitoEvent output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        if (output != null) {
            System.out.println(output.toString());
        }
    }
}
