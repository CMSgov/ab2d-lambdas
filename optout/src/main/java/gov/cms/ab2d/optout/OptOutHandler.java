package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.concurrent.*;

public class OptOutHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("OptOut Lambda is started");

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            executorService.execute(new OptOutS3(true, latch, logger)); // download file from BFD S3 locally
            executorService.execute(new OptOutS3(false, latch, logger)); // copy files from BFD S3 to AB2D S3

            latch.await();

            //ToDo: important! add capacity for backpressure
            BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();


            File file = new File(OptOutUtils.FILE_PATH);
            FileInputStream fileInputStream = new FileInputStream(file);

            Connection dbConnection = DatabaseUtil.getConnection();

            latch = new CountDownLatch(threadCount);

            executorService.execute(new OptOutProducer(queue, fileInputStream, latch, logger)); //parse file
            executorService.execute(new OptOutConsumer(queue, dbConnection, latch, logger)); //update database

            latch.await();

            if (file.delete()) {
                System.out.println("Deleted the file: " + OptOutUtils.FILE_NAME);
            } else {
                System.out.println("Failed to delete the file.");
            }
        } catch (NullPointerException | InterruptedException ex) {
            logger.log(ex.getMessage());
            outputStream.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new OptOutException(ex);
        } finally {
            shutdownAndAwaitTermination(executorService, logger);
            outputStream.write("OptOut Lambda Completed".getBytes(StandardCharsets.UTF_8));
        }
    }


    private void shutdownAndAwaitTermination(ExecutorService executorService, LambdaLogger logger) {
        logger.log("Call ThreadPoll shutdown");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
