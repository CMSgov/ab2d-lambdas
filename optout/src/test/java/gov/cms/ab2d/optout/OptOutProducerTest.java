package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

public class OptOutProducerTest {

    private final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
    private final CountDownLatch latch = Mockito.mock(CountDownLatch.class);

    @Test
    public void optOutProducerRunTest() {
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();
        InputStream inputStream = getClass().getResourceAsStream("/" + OptOutUtils.FILE_NAME);

        new OptOutProducer(queue, inputStream, latch, logger).run();

        assertEquals(3, queue.size()); // 1 for poisonPill
        OptOutMessage message = queue.poll();
        assertNotNull(message);
        assertEquals(1000000019, message.getOptOutInformation().getMbi());
        assertTrue(message.getOptOutInformation().isOptOut());
        assertFalse(message.isPoisonPill());
        queue.poll();
        message = queue.poll();
        assertNotNull(message);
        assertNull(message.getOptOutInformation());
        assertTrue(message.isPoisonPill());

        verify(latch).countDown();
    }

    @Test
    public void invalidLineTest() {
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();
        InputStream inputStream = getClass().getResourceAsStream("/invalidLine.txt");
        assertDoesNotThrow(() -> new OptOutProducer(queue, inputStream, latch, logger).run());
        assertEquals(3, queue.size()); // 2 valid lines and 1 poisonPill
    }
}
