package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;

public class OptOutHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        var logger = context.getLogger();
        logger.log("OptOut Lambda is started");
        try {
            //ToDo: Get file name from SQS
            new OptOutProcessing("optOutDummy.txt", logger).process();
        } catch (NullPointerException | CompletionException ex) {
            logger.log(ex.getMessage());
            outputStream.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new OptOutException(ex);
        } finally {
            outputStream.write("OptOut Lambda Completed".getBytes(StandardCharsets.UTF_8));
        }
    }

}
