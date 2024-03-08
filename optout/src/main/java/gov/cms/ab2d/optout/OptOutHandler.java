package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URISyntaxException;

import static gov.cms.ab2d.optout.OptOutConstants.ENDPOINT;

public class OptOutHandler implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            processSQSMessage(msg, context);
        }
        context.getLogger().log("OptOut Lambda completed");
        return null;
    }

    public void processSQSMessage(SQSEvent.SQSMessage msg, Context context) {
        var logger = context.getLogger();
        try {
            logger.log("OptOut Lambda started. Processing message from SQS " + msg.getBody());

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(msg.getBody());
            var s3EventMessage = json.get("Message");
            var notification = S3EventNotification.parseJson(s3EventMessage.toString()).getRecords().get(0);

            var optOutProcessing = processorInit(getFileName(notification), getBucketName(notification), logger);
            optOutProcessing.process();
        } catch (Exception ex) {
            logger.log("An error occurred");
            throw new OptOutException("An error occurred", ex);
        }
    }

    public OptOutProcessor processorInit(String fileName, String bfdBucket, LambdaLogger logger) throws URISyntaxException {
        return new OptOutProcessor(fileName, bfdBucket, ENDPOINT, logger);
        //ToDo: uncomment when permanent credentials will be available
//        var creds = SecretManager.getS3Credentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY, ACCESS_TOKEN, logger);
//        if (creds.isPresent())
//            return new OptOutProcessing(msg, ENDPOINT, creds.get(), logger);
//        else {
//            logger.log("Can't get Credentials from Secret manager");
//            throw new OptOutException("Can't get Credentials from Secret manager");
//        }
    }

    public String getBucketName(S3EventNotification.S3EventNotificationRecord record) {
        return record.getS3().getBucket().getName();
    }

    public String getFileName(S3EventNotification.S3EventNotificationRecord record) {
        return record.getS3().getObject().getUrlDecodedKey();
    }
}
