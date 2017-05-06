<#if packageName?has_content>
package ${packageName};
</#if>

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;

public class ${handlerClassName} implements RequestHandler<DynamodbEvent, Integer> {

    @Override
    public Integer handleRequest(DynamodbEvent event, Context context) {
        context.getLogger().log("Received event: " + event);

        for (DynamodbStreamRecord record : event.getRecords()) {
            context.getLogger().log(record.getEventID());
            context.getLogger().log(record.getEventName());
            context.getLogger().log(record.getDynamodb().toString());
        }
        return event.getRecords().size();
    }
}