package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

        //ToDo: important! add capacity for backpressure
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();

        int threadCount = 2; //producer and consumer
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //ToDo: replace on S3
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            File file = new File(Objects.requireNonNull(classLoader.getResource("optOutDummy.txt")).getFile());
            Connection dbConnection = DatabaseUtil.getConnection();

            executorService.execute(new OptOutProducer(queue, file, latch, logger));
            executorService.execute(new OptOutConsumer(queue, dbConnection, latch, logger));

            latch.await();
        } catch (NullPointerException | SQLException | InterruptedException ex) {
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
