<#if packageName?has_content>
package ${packageName};
</#if>

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;

public class ${handlerClassName} implements RequestHandler<KinesisEvent, Integer> {

    @Override
    public Integer handleRequest(KinesisEvent event, Context context) {
        context.getLogger().log("Input: " + event);

        for (KinesisEventRecord record : event.getRecords()) {
            String payload = new String(record.getKinesis().getData().array());
            context.getLogger().log("Payload: " + payload);
        }

        return event.getRecords().size();
    }
}
