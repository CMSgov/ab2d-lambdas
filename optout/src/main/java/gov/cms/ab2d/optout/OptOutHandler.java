package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

            Connection dbConnection = DatabaseUtil.getConnection();

            latch = new CountDownLatch(threadCount);

            executorService.execute(new OptOutProducer(OptOutS3.FILE_PATH, queue, latch, logger)); //parse file
            executorService.execute(new OptOutConsumer(queue, dbConnection, latch, logger)); //update database

            latch.await();
        } catch (NullPointerException | InterruptedException | CompletionException ex) {
            logger.log(ex.getMessage());
            outputStream.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new OptOutException(ex);
        } finally {
            FileUtil.deleteDirectoryRecursion(Paths.get(OptOutS3.FILE_PATH));
            shutdownAndAwaitTermination(executorService, logger);
            outputStream.write("OptOut Lambda Completed".getBytes(StandardCharsets.UTF_8));
        }
    }

     void shutdownAndAwaitTermination(ExecutorService executorService, LambdaLogger logger) {
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
