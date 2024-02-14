package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URISyntaxException;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHandlerConstants.TEST_ENDPOINT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class AttributionDataShareHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);

    AttributionDataShareHelper helper = mock(AttributionDataShareHelper.class);
    AttributionDataShareHandler handler = spy(new AttributionDataShareHandler());

    @Test
    void attributionDataShareInvoke() {
        when(handler.helperInit(anyString(), anyString(), any(LambdaLogger.class))).thenReturn(helper);
        assertDoesNotThrow(() -> handler.handleRequest(null, System.out, new TestContext()));
    }

    @Test
    void attributionDataShareExceptionTest() {
        Exception ex = mock(Exception.class);
        when(ex.getMessage()).thenReturn("Exception");
        assertThrows(AttributionDataShareException.class, () -> handler.throwAttributionDataShareException(LOGGER, ex));
    }

    @Test
    void getS3ClientTest() throws URISyntaxException {
        assertNotNull(handler.getS3Client(TEST_ENDPOINT));
    }
}
