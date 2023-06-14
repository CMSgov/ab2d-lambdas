package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

public class OptOutConsumerTest {

    @Test
    public void optOutConsumerRunTest() throws InterruptedException {
        CountDownLatch latch = Mockito.mock(CountDownLatch.class);
        LambdaLogger logger = Mockito.mock(LambdaLogger.class);
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();

        queue.put(new OptOutMessage(new OptOutInformation(1, OffsetDateTime.now(), true), false));
        queue.put(new OptOutMessage(null, true));

        new OptOutConsumer(queue, latch, logger).run();

        assertTrue(queue.isEmpty());

        verify(latch).countDown();
    }
}