package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class OptOutProducer implements Runnable {

    private final String path;
    private final BlockingQueue<OptOutMessage> queue;
    private final CountDownLatch latch;
    private final LambdaLogger logger;

    public OptOutProducer(String path, BlockingQueue<OptOutMessage> queue, CountDownLatch latch, LambdaLogger logger) {
        this.path = path;
        this.queue = queue;
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

    public void process() throws InterruptedException {
        try {
            for (File file : Objects.requireNonNull(new File(path).listFiles())) {
                if (file.isFile()) {
                    parseFile(file);
                }
            }
        } catch (IOException ex) {
            logger.log("File processing failed with exception: " + ex.getMessage());
        } finally {
            queue.put(new OptOutMessage(null, true));
        }
    }

    public void parseFile(File file) throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    OptOutInformation information = new OptOutInformation(line, logger);
                    queue.put(new OptOutMessage(information, false));
                } catch (IllegalArgumentException ex) {
                    logger.log("Data is invalid");
                }
            }
        }
    }
}



