package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

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

            var optOutProcessing = processingInit(msg.getBody(), logger);
            optOutProcessing.process();
        } catch (Exception ex) {
            logger.log("An error occurred");
            throw new OptOutException("An error occurred", ex);
        }
    }

    public OptOutProcessing processingInit(String msg, LambdaLogger logger) throws URISyntaxException {
        return new OptOutProcessing(msg, ENDPOINT, logger);
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
