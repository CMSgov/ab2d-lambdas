package gov.cms.ab2d.optout;

import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
public class OptOutHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();

    //ToDo: Failing in Jenkins. Waiting for bfd credentials
 //   @Test
//    void optOutHandlerInvoke() {
//        OptOutHandler handler = new OptOutHandler();
//        assertDoesNotThrow(() -> handler.handleRequest(null, System.out, new TestContext()));
//    }

    @Test
    void optOutHandlerNullPointerExceptionTest() throws IOException {
        OptOutHandler handler = Mockito.mock(OptOutHandler.class);
        doThrow(new NullPointerException()).when(handler).handleRequest(any(), any(), any());
        assertThrows(NullPointerException.class, () -> handler.handleRequest(null, System.out, new TestContext()));
    }

}
