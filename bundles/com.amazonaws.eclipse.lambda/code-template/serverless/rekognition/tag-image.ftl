package ${packageName};

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;

public class ${className} implements RequestHandler<S3Event, Void> {

    /**
     * This handler is triggered by an S3-put-object event, and then calls AWS Rekognition to tag the
     * newly created S3 object.
     */
    @Override
    public Void handleRequest(S3Event input, Context context) {
        context.getLogger().log("Input: " + input);

        // Create clients
        AmazonS3 s3Client = AmazonS3Client.builder().build();
        AmazonRekognition rekognitionClient = AmazonRekognitionClient.builder().build();

        // For every S3 object
        for (S3EventNotificationRecord event : input.getRecords()) {
            S3Entity entity = event.getS3();
            String bucketName = entity.getBucket().getName();
            String objectKey = entity.getObject().getKey();

            Image imageToTag = new Image().withS3Object(new S3Object().withName(objectKey).withBucket(bucketName));

            // Call Rekognition to identify image labels
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(imageToTag)
                    .withMaxLabels(5)
                    .withMinConfidence(77F);

            try {
                List<Label> labels = rekognitionClient.detectLabels(request).getLabels();
                // Add the labels tag to the object
                List<Tag> newTags = new ArrayList<>();

                if (labels.isEmpty()) {
                    System.out.println("No label is recognized!");
                } else {
                    System.out.println("Detected labels for " + imageToTag.getS3Object().getName());
                }
                for (Label label : labels) {
                    System.out.println(label.getName() + ": " + label.getConfidence().toString());
                    newTags.add(new Tag(label.getName(), label.getConfidence().toString()));
                }

                s3Client.setObjectTagging(new SetObjectTaggingRequest(
                        bucketName, objectKey, new ObjectTagging(newTags)));

            } catch (AmazonServiceException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
