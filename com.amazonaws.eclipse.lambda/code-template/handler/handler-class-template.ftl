<#if packageName?has_content>
package ${packageName};
</#if>

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
<#if additionalImports?has_content>

<#list additionalImports as import>
import ${import};
</#list>
</#if>

public class ${handlerClassName!""} implements RequestHandler<${inputType!""}, ${outputType!""}> {

    @Override
    public ${outputType!""} handleRequest(${inputType!""} input, Context context) {
        context.getLogger().log("Input: " + input);

        // TODO: implement your handler
        return null;
    }

}
