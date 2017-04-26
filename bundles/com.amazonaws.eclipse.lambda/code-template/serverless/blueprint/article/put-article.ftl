package ${packageName};

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import ${inputFqcn};
import ${outputFqcn};

/**
 * Lambda function that triggered by the API Gateway event "POST /". It reads all the query parameters as the metadata for this
 * article and stores them to a DynamoDB table. It reads the payload as the content of the article and stores it to a S3 bucket.
 */
public class ${className} implements RequestHandler<ServerlessInput, ServerlessOutput> {
    // DynamoDB table name for storing article metadata.
    private static final String ARTICLE_TABLE_NAME = System.getenv("ARTICLE_TABLE_NAME");
    // DynamoDB table attribute name for storing article id.
    private static final String ARTICLE_TABLE_ID_NAME = "id";
    // DynamoDB table attribute name for storing the bucket name where holds the article's content.
    private static final String ARTICLE_TABLE_BUCKET_NAME = "bucket";
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
                String keyName = UUID.randomUUID().toString();
                String content = serverlessInput.getBody();
                s3.putObject(new PutObjectRequest(
                        ARTICLE_BUCKET_NAME,
                        keyName,
                        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                        new ObjectMetadata())
                );
                
                Map<String, AttributeValue> attributes = convert(serverlessInput.getQueryStringParameters());
                attributes.putIfAbsent(ARTICLE_TABLE_ID_NAME, new AttributeValue().withS(UUID.randomUUID().toString()));
                attributes.put(ARTICLE_TABLE_BUCKET_NAME, new AttributeValue().withS(ARTICLE_BUCKET_NAME));
                attributes.put(ARTICLE_TABLE_KEY_NAME, new AttributeValue().withS(keyName));
                dynamoDb.putItem(new PutItemRequest()
                        .withTableName(ARTICLE_TABLE_NAME)
                        .withItem(attributes));
                output.setStatusCode(200);
                output.setBody("Successfully inserted article " + attributes.get(ARTICLE_TABLE_ID_NAME).getS());
            } catch (Exception e) {
                output.setStatusCode(500);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                output.setBody(sw.toString());
            }
        return output;
    }

    private Map<String, AttributeValue> convert(Map<String, String> map) {
        return Optional.ofNullable(map).orElseGet(HashMap::new).entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> new AttributeValue().withS(e.getValue())));
    }
}