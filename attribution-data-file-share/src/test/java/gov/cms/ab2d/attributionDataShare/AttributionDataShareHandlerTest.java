package gov.cms.ab2d.attributionDataShare;

import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
public class AttributionDataShareHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();

    @Test
    void attributionDataShareInvoke() {
        AttributionDataShareHandler attributionDataShareHandler = new AttributionDataShareHandler();

        assertDoesNotThrow(() -> {
            attributionDataShareHandler.handleRequest(null, System.out, new TestContext());
        });
    }

}
