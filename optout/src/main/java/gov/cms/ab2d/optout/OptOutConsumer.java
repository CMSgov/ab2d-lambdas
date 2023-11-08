package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import static gov.cms.ab2d.optout.OptOutUtils.BATCH_INSERT_SIZE;
import static gov.cms.ab2d.optout.OptOutUtils.UPDATE_WITH_OPTOUT;


public class OptOutConsumer implements Runnable {

    private final BlockingQueue<OptOutMessage> queue;
    private final Connection dbConnection;
    private final CountDownLatch latch;
    private final LambdaLogger logger;

    public OptOutConsumer(BlockingQueue<OptOutMessage> queue, Connection dbConnection, CountDownLatch latch, LambdaLogger logger) {
        this.queue = queue;
        this.dbConnection = dbConnection;
        this.latch = latch;
        this.logger = logger;
    }

    @Override
    public void run() {
        List<OptOutInformation> optOutBatch = new ArrayList<>();
        try {
            while (true) {
                OptOutMessage message = queue.take();
                // if this is a poison pill then process
                if (message.isPoisonPill()) {
                    process(optOutBatch);
                    break;
                }
                // collect messages for batch insert
                if (optOutBatch.size() < BATCH_INSERT_SIZE)
                    optOutBatch.add(message.getOptOutInformation());
                else {
                    process(optOutBatch);
                    optOutBatch = new ArrayList<>();
                }
            }
        } catch (InterruptedException ex) {
            logger.log("Queue consumer is failed with exception: " + ex.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    public void process(List<OptOutInformation> messages) {
        try (PreparedStatement statement = dbConnection.prepareStatement(UPDATE_WITH_OPTOUT)) {
            for (OptOutInformation optOut : messages) {
                logger.log("Mbi: " + optOut.getMbi() + ", OptOut Flag: " + optOut.isOptOut());
                OptOutUtils.prepareInsert(optOut, statement);
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            logger.log(ex.getMessage());
        }
    }
}
