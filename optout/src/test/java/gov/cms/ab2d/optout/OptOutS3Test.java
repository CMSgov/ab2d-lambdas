package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

public class OptOutS3Test {
    private final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
    private final CountDownLatch latch = Mockito.mock(CountDownLatch.class);

    @Test
    void optOutS3RunTest() {
        OptOutS3 optOutS3 = Mockito.spy(new OptOutS3(true, latch, logger));
        optOutS3.run();
        verify(optOutS3, times(1)).downloadFilesToDirectory();
        verify(latch).countDown();
    }
}
