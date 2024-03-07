package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

import java.net.URISyntaxException;

import static gov.cms.ab2d.optout.OptOutConstants.ENDPOINT;

public class OptOutHandler implements RequestHandler<S3Event, Void> {

    @Override
    public Void handleRequest(S3Event s3event, Context context) {
        var logger = context.getLogger();

        S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getUrlDecodedKey();

        logger.log("OptOut Lambda started");
        try {
            var optOutProcessing = processorInit(srcKey, srcBucket, logger);
            optOutProcessing.process();
        } catch (Exception ex) {
            logger.log("An error occurred");
            throw new OptOutException("An error occurred", ex);
        }
        logger.log("OptOut Lambda completed");
        return null;
    }

    public OptOutProcessor processorInit(String fileName, String bucketName, LambdaLogger logger) throws URISyntaxException {
        return new OptOutProcessor(fileName, bucketName, ENDPOINT, logger);
        //ToDo: uncomment when permanent credentials will be available
//        var creds = SecretManager.getS3Credentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY, ACCESS_TOKEN, logger);
//        if (creds.isPresent())
//            return new OptOutProcessing(msg, ENDPOINT, creds.get(), logger);
//        else {
//            logger.log("Can't get Credentials from Secret manager");
//            throw new OptOutException("Can't get Credentials from Secret manager");
//        }
    }
}
