package gov.cms.ab2d.coveragecounts;

import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InvokeTest {

    @Test
    void
    coverageInvoke() {
        CoverageCountsHandler eventHandler = new CoverageCountsHandler();
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(null, System.out, new TestContext());
        });
    }


}
