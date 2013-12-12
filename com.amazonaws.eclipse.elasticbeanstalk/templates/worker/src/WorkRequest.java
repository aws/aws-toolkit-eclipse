
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A POJO representation of a work request.
 */
class WorkRequest {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private String bucket;
    private String key;
    private String message;
    
    /**
     * Create a work request by parsing it from a JSON format.
     * 
     * @param json the JSON document to parse
     * @return the parsed work request
     * @throws IOException on error parsing the document
     */
    public static WorkRequest fromJson(final InputStream json) 
            throws IOException {
        
        return MAPPER.readValue(json, WorkRequest.class);
    }

    /**
     * @return the Amazon S3 bucket to write to
     */
    public String getBucket() {
        return bucket;
    }
    
    /**
     * @param value the Amazon S3 bucket to write to
     */
    public void setBucket(final String value) {
        bucket = value;
    }
    
    /**
     * @return the Amazon S3 object key to write to
     */
    public String getKey() {
        return key;
    }
    
    /**
     * @param value the Amazon S3 object key to write to
     */
    public void setKey(final String value) {
        key = value;
    }
    
    /**
     * @return the message to write to Amazon S3
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @param value the message to write to Amazon S3
     */
    public void setMessage(final String value) {
        message = value;
    }
    
    /**
     * Serialize the work request to JSON.
     * 
     * @return the serialized JSON
     * @throws IOException on error serializing the value
     */
    public String toJson() throws IOException {
        return MAPPER.writeValueAsString(this);
    }
}
