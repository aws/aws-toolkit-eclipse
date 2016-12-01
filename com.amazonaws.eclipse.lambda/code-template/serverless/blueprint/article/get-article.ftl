package ${packageName};

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.util.IOUtils;
import ${inputFqcn};
import ${outputFqcn};

/**
 * Lambda function that triggered by the API Gateway event "GET /". It reads query parameter "id" for the article id and retrieves
 * the content of that article from the underlying S3 bucket and returns the content as the payload of the HTTP Response.
 */
public class ${className} implements RequestHandler<ServerlessInput, ServerlessOutput> {
    // DynamoDB table name for storing article metadata.
    private static final String ARTICLE_TABLE_NAME = System.getenv("ARTICLE_TABLE_NAME");
    // DynamoDB table attribute name for storing article id.
    private static final String ARTICLE_TABLE_ID_NAME = "id";
    // DynamoDB table attribute name for storing the bucket object key name that contains the article's content.
    private static final String ARTICLE_TABLE_KEY_NAME = "key";
    // S3 bucket name for storing article content.
    private static final String ARTICLE_BUCKET_NAME = System.getenv("ARTICLE_BUCKET_NAME");
    @Override
    public ServerlessOutput handleRequest(ServerlessInput serverlessInput, Context context) {
        // Using builder to create the clients could allow us to dynamically load the region from the AWS_REGION environment
        // variable. Therefore we can deploy the Lambda functions to different regions without code change.    
        AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.standard().build();
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
        ServerlessOutput output = new ServerlessOutput();
        
        try {
            if (serverlessInput.getQueryStringParameters() == null || serverlessInput.getQueryStringParameters().get(ARTICLE_TABLE_ID_NAME) == null) {
                    throw new Exception("Parameter " + ARTICLE_TABLE_ID_NAME + " in query must be provided!");
            }
            Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
            key.put(ARTICLE_TABLE_ID_NAME, new AttributeValue().withS(serverlessInput.getQueryStringParameters().get(ARTICLE_TABLE_ID_NAME)));
            Map<String, AttributeValue> item = dynamoDb.getItem(new GetItemRequest()
                    .withTableName(ARTICLE_TABLE_NAME)
                    .withKey(key))
                    .getItem();
            String s3ObjectKey = item.get(ARTICLE_TABLE_KEY_NAME).getS();
            String content = IOUtils.toString(s3.getObject(new GetObjectRequest(ARTICLE_BUCKET_NAME, s3ObjectKey)).getObjectContent());
            output.setStatusCode(200);
            output.setBody(content);
        } catch (Exception e) {
            output.setStatusCode(500);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            output.setBody(sw.toString());
        }

        return output;
    }
}