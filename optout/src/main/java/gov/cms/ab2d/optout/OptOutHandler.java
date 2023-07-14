package gov.cms.ab2d.optout;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.*;

public class OptOutHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("OptOut Lambda is started");

      //  downloadFileFromS3(logger);

        //ToDo: important! add capacity for backpressure
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();

        int threadCount = 2; //producer and consumer
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //ToDo: replace on S3
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            File file = new File(Objects.requireNonNull(classLoader.getResource(Constants.fileName)).getFile());
            //for s3
          //  File file = new File(Constants.filePath);
            Connection dbConnection = DatabaseUtil.getConnection();

            executorService.execute(new OptOutProducer(queue, file, latch, logger));
            executorService.execute(new OptOutConsumer(queue, dbConnection, latch, logger));

            latch.await();

//            if (file.delete()) {
//                System.out.println("Deleted the file: " + Constants.fileName);
//            } else {
//                System.out.println("Failed to delete the file.");
//            }
        } catch (NullPointerException | SQLException | InterruptedException ex) {
            logger.log(ex.getMessage());
            outputStream.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            throw new OptOutException(ex);
        } finally {
            shutdownAndAwaitTermination(executorService, logger);
            outputStream.write("OptOut Lambda Completed".getBytes(StandardCharsets.UTF_8));
        }
    }
    //Commented to avoid unnecessary work with temporary credentials
/*
    private void downloadFileFromS3(LambdaLogger logger) {
        logger.log("Downloading " + Constants.fileName + " from S3 bucket " + Constants.s3BucketName);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Constants.s3Region).build();
        try {
            S3Object o = s3.getObject(Constants.s3BucketName, Constants.fileName);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(Constants.filePath);
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
        } catch (AmazonServiceException ex) {
            logger.log(ex.getErrorMessage());
            throw new OptOutException(ex);
        } catch (IOException ex) {
            logger.log(ex.getMessage());
            throw new OptOutException(ex);
        }
    }

 */

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
