package gov.cms.ab2d.optout;

import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OptOutHandlerTest {

    @Test
    void optOutHandlerInvoke() {
        OptOutHandler optOutHandler = new OptOutHandler();
        assertDoesNotThrow(() -> {
            optOutHandler.handleRequest(null, System.out, new TestContext());
        });
    }

}
