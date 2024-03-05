package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URISyntaxException;
import java.util.Collections;

import static gov.cms.ab2d.optout.OptOutConstants.TEST_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
public class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private final static OptOutHandler handler = spy(new OptOutHandler());
    private final static OptOutProcessing optOutProcessing = mock(OptOutProcessing.class);
    private final static SQSEvent sqsEvent = mock(SQSEvent.class);
    private final static SQSEvent.SQSMessage sqsMessage = mock(SQSEvent.SQSMessage.class);

    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        when(sqsEvent.getRecords()).thenReturn(Collections.singletonList(sqsMessage));
        when(sqsMessage.getBody()).thenReturn(TEST_FILE_NAME);
        when(handler.processingInit(anyString(), any(LambdaLogger.class))).thenReturn(optOutProcessing);
    }

    @Test
    void optOutHandlerInvoke() {
        Context context = mock(Context.class);
        LambdaLogger logger = mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(logger);

        assertDoesNotThrow(() -> handler.handleRequest(sqsEvent, context));
    }

    @Test
    void optOutHandlerException() {
        doThrow(new OptOutException("errorMessage", new AmazonS3Exception("errorMessage"))).when(optOutProcessing).process();
        assertThrows(OptOutException.class, optOutProcessing::process);
    }
}
