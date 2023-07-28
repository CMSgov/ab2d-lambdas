package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class OptOutProducer implements Runnable {

    private final BlockingQueue<OptOutMessage> queue;
    private final InputStream inputStream;
    private final CountDownLatch latch;
    private final LambdaLogger logger;

    public OptOutProducer(BlockingQueue<OptOutMessage> queue, InputStream inputStream, CountDownLatch latch, LambdaLogger logger) {
        this.queue = queue;
        this.inputStream = inputStream;
        this.latch = latch;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            process();
            logger.log("File processing completed");
        } catch (InterruptedException ex) {
            logger.log("File processing failed with exception: " + ex.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    private void process() throws InterruptedException {
        try {
            parseFile();
        } catch (IOException ex) {
            logger.log("File processing failed with exception: " + ex.getMessage());
        }
    }

    private void parseFile() throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    OptOutInformation information = new OptOutInformation(line, logger);
                    queue.put(new OptOutMessage(information, false));
                } catch (IllegalArgumentException ex) {
                    logger.log("Data is invalid");
                }
            }
        } finally {
            queue.put(new OptOutMessage(null, true));
        }

    }

}



