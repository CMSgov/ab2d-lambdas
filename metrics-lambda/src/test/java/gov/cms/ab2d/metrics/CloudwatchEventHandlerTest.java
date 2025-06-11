package gov.cms.ab2d.metrics;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.metrics.CloudwatchEventHandler.getQueueName;
import static org.junit.jupiter.api.Assertions.*;

class CloudwatchEventHandlerTest {

    @Test
    void testQueueNameForGreenfield() {
        assertEquals("ab2d-dev-events-sqs", getQueueName("ab2d-dev"));
        assertEquals("ab2d-test-events-sqs", getQueueName("ab2d-east-impl"));
        assertEquals("ab2d-sandbox-events-sqs", getQueueName("ab2d-sbx-sandbox"));
        assertEquals("ab2d-prod-events-sqs", getQueueName("ab2d-east-prod"));

    }
}
