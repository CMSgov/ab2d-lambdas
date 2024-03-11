package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
public class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    private final static OptOutHandler handler = spy(new OptOutHandler());
    private final static OptOutProcessor OPT_OUT_PROCESSOR = mock(OptOutProcessor.class);
    private final static SQSEvent sqsEvent = mock(SQSEvent.class);
    private final static SQSEvent.SQSMessage sqsMessage = mock(SQSEvent.SQSMessage.class);

    @BeforeAll
    static void beforeAll() throws URISyntaxException, IOException {
        when(sqsEvent.getRecords()).thenReturn(Collections.singletonList(sqsMessage));
        when(sqsMessage.getBody()).thenReturn(getPayload());
        when(handler.processorInit(anyString(), anyString(), any(LambdaLogger.class))).thenReturn(OPT_OUT_PROCESSOR);
    }

    @Test
    void getBucketAndFileNamesTest() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(getPayload());
        var s3EventMessage = json.get("Message");

        var notification = S3EventNotification.parseJson(s3EventMessage.toString()).getRecords().get(0);

        assertEquals("bfdeft01/ab2d/in/optOutDummy.txt", handler.getFileName(notification));
        assertEquals("bfd-test-eft", handler.getBucketName(notification));
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
        doThrow(new OptOutException("errorMessage", new AmazonS3Exception("errorMessage"))).when(OPT_OUT_PROCESSOR).process();
        assertThrows(OptOutException.class, OPT_OUT_PROCESSOR::process);
    }

    static private String getPayload() throws IOException {
        return Files.readString(Paths.get("src/test/resources/sqsEvent.json"));
    }
}
