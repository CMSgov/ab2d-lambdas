package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import gov.cms.ab2d.testutils.TestContext;
import org.junit.jupiter.api.Test;
import org.postgresql.copy.CopyManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHandlerConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class AttributionDataShareHandlerTest {

    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);
    @Test
    void attributionDataShareInvoke() {
        AttributionDataShareHandler handler = new AttributionDataShareHandler();
        assertDoesNotThrow(() -> handler.handleRequest(null, System.out, new TestContext()));
    }

    @Test
    void copyDataToFileTest() throws SQLException, IOException {
        CopyManager copyManager = mock(CopyManager.class);
        BufferedWriter writer = mock(BufferedWriter.class);
        String currentDate = new SimpleDateFormat(PATTERN).format(new Date());
        String fileFullPath = FILE_PATH + FILE_PARTIAL_NAME + currentDate + FILE_FORMAT;
        Path path = Paths.get(fileFullPath);
        AttributionDataShareHandler handler = new AttributionDataShareHandler();

        when(copyManager.copyOut(eq(COPY_STATEMENT), eq(writer))).thenReturn(anyLong());
        assertDoesNotThrow(() -> handler.copyDataToFile(fileFullPath, LOGGER));

        assertTrue(Files.exists(path));
        FileUtil.deleteDirectoryRecursion(path);
    }
    @Test
    void attributionDataShareExceptionTest() {
        Exception ex = mock(Exception.class);
        when(ex.getMessage()).thenReturn("Exception");
        AttributionDataShareHandler handler = new AttributionDataShareHandler();
        assertThrows(AttributionDataShareException.class, () -> handler.throwAttributionDataShareException(LOGGER, ex));
    }

}
