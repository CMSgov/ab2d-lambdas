package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.copy.CopyManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHandlerConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith({S3MockAPIExtension.class})
public class AttributionDataShareHelperTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);

    String FILE_NAME = FILE_PARTIAL_NAME + new SimpleDateFormat(PATTERN).format(new Date()) + FILE_FORMAT;
    String FILE_FULL_PATH = FILE_PATH + FILE_NAME;
    AttributionDataShareHelper helper;

    @BeforeEach
    public void beforeEach() {
        helper = spy(new AttributionDataShareHelper(FILE_NAME, FILE_FULL_PATH, LOGGER));
    }

    @Test
    void copyDataToFileTest() throws SQLException, IOException {
        CopyManager copyManager = mock(CopyManager.class);
        when(helper.getCopyManager()).thenReturn(copyManager);
        assertDoesNotThrow(() -> helper.copyDataToFile());

        assertTrue(Files.exists(Paths.get(FILE_FULL_PATH)));
        FileUtil.deleteDirectoryRecursion(Paths.get(FILE_FULL_PATH));
    }

    @Test
    void copyDataToFileExceptionTest() {
        assertThrows(AttributionDataShareException.class, () -> helper.copyDataToFile());
    }

    @Test
    void writeFileToFinalDestinationTest() throws IOException {
        createTestFile();
        helper.writeFileToFinalDestination(S3MockAPIExtension.S3_CLIENT);
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    @Test
    void getCopyManagerTest() throws SQLException {
        Assertions.assertNotNull(helper.getCopyManager());
    }

    private void createTestFile() throws IOException {
        PrintWriter writer = new PrintWriter(FILE_FULL_PATH, StandardCharsets.UTF_8);
        writer.println("Test");
        writer.close();
    }


}
