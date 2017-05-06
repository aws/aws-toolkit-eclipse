<#if packageName?has_content>
package ${packageName};
</#if>

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoEvent.DatasetRecord;

public class ${handlerClassName} implements RequestHandler<CognitoEvent, CognitoEvent> {

    private final String sampleKey1 = "SampleKey1";
    private final String sampleKey2 = "SampleKey2";
    private final String sampleKey3 = "sampleKey3";

    @Override
    public CognitoEvent handleRequest(CognitoEvent event, Context context) {
        context.getLogger().log("Input: " + event);

        CognitoEvent modifiedEvent = event;

        // Check for the event type
        if (event.getEventType() == "SyncTrigger") {
            // Modify value for a key
            if (event.getDatasetRecords().containsKey(sampleKey1)) {
                modifiedEvent.getDatasetRecords().get(sampleKey1).setNewValue("ModifyValue1");
                modifiedEvent.getDatasetRecords().get(sampleKey1).setOp("replace");
            }

            // Remove a key
            if (event.getDatasetRecords().containsKey(sampleKey2)) {
                modifiedEvent.getDatasetRecords().get(sampleKey2).setOp("remove");
            }

            // Add a key
            if (!event.getDatasetRecords().containsKey(sampleKey3)) {
                DatasetRecord record = new DatasetRecord();
                record.setNewValue("ModifyValue3");
                record.setOp("replace");
                modifiedEvent.getDatasetRecords().put(sampleKey3, record);
            }
        }

        return modifiedEvent;
    }

}