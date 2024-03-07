package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
public class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private final static OptOutHandler handler = spy(new OptOutHandler());
    private final static OptOutProcessor OPT_OUT_PROCESSOR = mock(OptOutProcessor.class);
    private final static S3Event s3event = mock(S3Event.class);

    @BeforeAll
    static void beforeAll() throws URISyntaxException, IOException {
        var payload = Files.readString(Paths.get("src/test/resources/s3event.json"));
        S3EventNotification records = S3EventNotification.parseJson(payload);
        var record = records.getRecords().get(0);
        var srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        when(s3event.getRecords()).thenReturn(records.getRecords());
        when(handler.processorInit(eq(srcBucket), eq(srcKey), any(LambdaLogger.class))).thenReturn(OPT_OUT_PROCESSOR);
    }

//    @Test
//    void optOutHandlerInvoke() {
//        Context context = mock(Context.class);
//        LambdaLogger logger = mock(LambdaLogger.class);
//        when(context.getLogger()).thenReturn(logger);
//
//        assertDoesNotThrow(() -> handler.handleRequest(s3event, context));
//    }

    @Test
    void optOutHandlerException() {
        doThrow(new OptOutException("errorMessage", new AmazonS3Exception("errorMessage"))).when(OPT_OUT_PROCESSOR).process();
        assertThrows(OptOutException.class, OPT_OUT_PROCESSOR::process);
    }
}
