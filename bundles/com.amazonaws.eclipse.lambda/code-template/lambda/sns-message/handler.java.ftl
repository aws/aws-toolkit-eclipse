<#if packageName?has_content>
package ${packageName};
</#if>

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class ${handlerClassName} implements RequestHandler<SNSEvent, String> {

    @Override
    public String handleRequest(SNSEvent event, Context context) {
        context.getLogger().log("Received event: " + event);
        String message = event.getRecords().get(0).getSNS().getMessage();
        context.getLogger().log("From SNS: " + message);
        return message;
    }
}
