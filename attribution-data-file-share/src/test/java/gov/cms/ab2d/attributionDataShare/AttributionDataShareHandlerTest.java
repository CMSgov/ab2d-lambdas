package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URISyntaxException;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstantsTest.TEST_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Testcontainers
class AttributionDataShareHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);
    AttributionParameterStore parameterStore = new AttributionParameterStore("", "", "", "");
    AttributionDataShareHandler handler = spy(new AttributionDataShareHandler());
//
//    @Test
//    void attributionDataShareInvoke() throws SQLException {
//       var  mockParameterStore = mockStatic(AttributionParameterStore.class);
//        mockParameterStore
//                .when(AttributionParameterStore::getParameterStore)
//                .thenReturn(parameterStore);
//
//        mockStatic(DriverManager.class)
//                .when(() ->  DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(connection);
//        when(connection.createStatement()).thenReturn(stmt);
//        when(getExecuteQuery(stmt)).thenReturn(resultSet());
//        assertDoesNotThrow(() -> handler.handleRequest(null, System.out, new TestContext()));
//    }

    @Test
    void attributionDataShareExceptionTest() {
        Exception ex = mock(Exception.class);
        when(ex.getMessage()).thenReturn("Exception");
        assertThrows(AttributionDataShareException.class, () -> handler.throwAttributionDataShareException(LOGGER, ex));
    }

    @Test
    void getS3ClientTest() throws URISyntaxException {
        assertNotNull(handler.getS3Client(TEST_ENDPOINT, parameterStore));
    }
}
