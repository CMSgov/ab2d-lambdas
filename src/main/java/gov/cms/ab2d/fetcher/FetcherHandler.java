package gov.cms.ab2d.fetcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;

import java.io.IOException;

/**
 * The entry point invoked by AWS
 */
@SuppressWarnings("unused")
public class FetcherHandler implements RequestHandler<KinesisEvent, String> {
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        logger.log("Kinesis event version.");
        String response = "200 OK";
        for (KinesisEventRecord eventRecord : event.getRecords()) {
            try {
                JsonNode node = objectMapper.readTree(eventRecord.getKinesis().getData().array());
                process(logger, node);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        return response;
    }

    private void process(LambdaLogger logger, JsonNode node) {
        String correlationId = node.get("collation_id").asText();
        long beneId = node.get("beneficiary_id").asLong();
        String sinceDateStr = node.get("since").asText();
        logger.log("Received fetch EOB message - corrId:" + correlationId +
                " beneId: "  + beneId + " since datestr: " + sinceDateStr + "\n");
    }
}

