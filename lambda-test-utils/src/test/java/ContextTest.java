import gov.cms.ab2d.testutils.TestContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
public class ContextTest {

    @Test
    void testContext() {
        TestContext testContext = new TestContext();

        assertDoesNotThrow(() -> {
            log.info(testContext.getAwsRequestId());
            log.info(testContext.getLogGroupName());
            log.info(testContext.getLogStreamName());
            log.info(testContext.getFunctionName());
            log.info(testContext.getFunctionVersion());
            log.info(testContext.getInvokedFunctionArn());
            log.info(String.valueOf(testContext.getIdentity()));
            log.info(String.valueOf(testContext.getClientContext()));
            log.info(String.valueOf(testContext.getRemainingTimeInMillis()));
            log.info(String.valueOf(testContext.getMemoryLimitInMB()));
            log.info(String.valueOf(testContext.getLogger()));

        });
    }
}
