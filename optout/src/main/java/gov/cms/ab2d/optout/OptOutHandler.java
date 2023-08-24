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

      //  downloadFileFromS3(logger);

        //ToDo: important! add capacity for backpressure
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();

        int threadCount = 2; //producer and consumer
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //ToDo: replace on S3
        try {
            InputStream fileInputStream = getClass().getResourceAsStream("/" + OptOutUtils.FILE_NAME);
/*
            for s3
            File file = new File(OptOutUtils.FILE_PATH);

 */
            Connection dbConnection = DatabaseUtil.getConnection();

            executorService.execute(new OptOutProducer(queue, fileInputStream, latch, logger));
            executorService.execute(new OptOutConsumer(queue, dbConnection, latch, logger));

            latch.await();
/*
            if (file.delete()) {
                System.out.println("Deleted the file: " + OptOutUtils.FILE_NAME);
            } else {
                System.out.println("Failed to delete the file.");
            }

 */
        } catch (NullPointerException | InterruptedException ex) {
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
        logger.log("Downloading " + OptOutUtils.FILE_NAME + " from S3 bucket " + OptOutUtils.S3_BUCKET_NAME);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(OptOutUtils.S3_REGION).build();
        try {
            S3Object o = s3.getObject(OptOutUtils.S3_BUCKET_NAME, OptOutUtils.FILE_NAME);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(OptOutUtils.FILE_PATH);
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
