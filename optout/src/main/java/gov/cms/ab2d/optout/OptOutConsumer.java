package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class OptOutConsumer implements Runnable {

    private final BlockingQueue<OptOutMessage> queue;
    private final CountDownLatch latch;
    private final LambdaLogger logger;

    public OptOutConsumer(BlockingQueue<OptOutMessage> queue, CountDownLatch latch, LambdaLogger logger) {
        this.queue = queue;
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
                //ToDo: update the magic batch size = 100
                if (optOutBatch.size() < 100)
                    optOutBatch.add(message.getOptOutInformation());
                else {
                    process(optOutBatch);
                    optOutBatch = new ArrayList<>();
                }
            }
        } catch (InterruptedException ex) {
            logger.log("Queue consumer is failed with exception: " + ex.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private void process(List<OptOutInformation> messages) throws InterruptedException {
        for (OptOutInformation optOut : messages) {
            logger.log("Mbi: " + optOut.getMbi() + ", OptOut Flag: " + optOut.isOptOut() + ", Effective date: " + optOut.getEffectiveDate());
        }
    }
}
