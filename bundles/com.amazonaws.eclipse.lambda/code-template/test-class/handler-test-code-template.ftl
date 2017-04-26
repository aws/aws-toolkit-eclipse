<#if packageName?has_content>
package ${packageName};
</#if>

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
<#if additionalImports?has_content>

<#list additionalImports as import>
import ${import};
</#list>
</#if>

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ${handlerTestClassName} {

    private static ${inputType} input;

    @BeforeClass
    public static void createInput() throws IOException {
    <#if inputJsonFileName??>
        input = TestUtils.parse("${inputJsonFileName}", ${inputType}.class);
    <#else>
        // TODO: set up your sample input object here.
        input = null;
    </#if>
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

        ${outputType} output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        if (output != null) {
            System.out.println(output.toString());
        }
    }
}
