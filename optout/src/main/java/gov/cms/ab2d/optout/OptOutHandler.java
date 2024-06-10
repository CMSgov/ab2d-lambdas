package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static gov.cms.ab2d.optout.OptOutConstants.ENDPOINT;

import java.util.ArrayList;
import java.util.List;

public class OptOutHandler implements RequestHandler<SQSEvent, Void> {

    private List<OptOutInformation> requestOptOutInformationList;

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        requestOptOutInformationList = new ArrayList<>();

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
            processSQSMessage(msg, context);
        }

        int totalNumberOfProcessedOptOuts = requestOptOutInformationList.size();
        int numberOfYOptOuts = 0;
        int numberOfNOptOuts = 0;

        for (OptOutInformation optOut : requestOptOutInformationList) {
            if (optOut.getOptOutFlag()) {
                numberOfYOptOuts++;
            } else {
                numberOfNOptOuts++;
            }
        }

        // TODO: Get the total number of opt-outs processed to date.
        // I Added a select statement in constants for that should do that
        // But need to double check if it is correct, since I think we may use a view
        // and not the 'real' table for performance reasons.
        
        String optOutCompletedMessage = String.format(
            "OptOut Lambda completed. Processed %d optout information records this run" +
            " with %d opted out, and %d opted in for data sharing.",
            totalNumberOfProcessedOptOuts, numberOfYOptOuts, numberOfNOptOuts
        );
        context.getLogger().log(optOutCompletedMessage);

        requestOptOutInformationList.clear();
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

            var optOutProcessing = processorInit(logger);
            optOutProcessing.process(getFileName(notification), getBucketName(notification), ENDPOINT);
            
            // Is there a reason the optOutInformationList is public?
            // Would it be better to make it private and use a getter of the processor?
            requestOptOutInformationList.addAll(optOutProcessing.optOutInformationList);
        } catch (Exception ex) {
            logger.log("An error occurred");
            throw new OptOutException("An error occurred", ex);
        }
    }

    public OptOutProcessor processorInit(LambdaLogger logger) {
        return new OptOutProcessor(logger);
    }

    public String getBucketName(S3EventNotification.S3EventNotificationRecord notificationRecord) {
        return notificationRecord.getS3().getBucket().getName();
    }

    public String getFileName(S3EventNotification.S3EventNotificationRecord notificationRecord) {
        return notificationRecord.getS3().getObject().getUrlDecodedKey();
    }
}
