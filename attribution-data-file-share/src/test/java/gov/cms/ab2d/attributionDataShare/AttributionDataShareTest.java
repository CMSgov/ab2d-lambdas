package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.mockrunner.mock.jdbc.MockResultSet;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;
import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHelper.getExecuteQuery;
import static gov.cms.ab2d.attributionDataShare.S3MockAPIExtension.getBucketName;
import static gov.cms.ab2d.attributionDataShare.S3MockAPIExtension.getUploadPath;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith({S3MockAPIExtension.class})
public class AttributionDataShareTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);
    String FILE_NAME = REQ_FILE_NAME + new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
    String FILE_FULL_PATH = FILE_PATH + FILE_NAME;
    String MBI_1 = "DUMMY000001";
    String MBI_2 = "DUMMY000002";
    Timestamp DATE = Timestamp.valueOf("2024-02-26 00:00:00");
    AttributionDataShareHelper helper;

    @BeforeEach
    public void beforeEach() {
        helper = spy(new AttributionDataShareHelper(FILE_NAME, FILE_FULL_PATH, LOGGER));
    }

    @Test
    void copyDataToFileTest() throws IOException, SQLException {
        var connection = mock(Connection.class);
        var stmt = mock(Statement.class);
        var rs = new MockResultSet("");
        rs.addColumn("mbi", Arrays.asList(MBI_1, MBI_2));
        rs.addColumn("effective_date", Arrays.asList(DATE, null));
        rs.addColumn("opt_out_flag", Arrays.asList(true, null));
        when(connection.createStatement()).thenReturn(stmt);

        when(getExecuteQuery(stmt)).thenReturn(rs);
        assertDoesNotThrow(() -> helper.copyDataToFile(connection));

        assertTrue(Files.exists(Paths.get(FILE_FULL_PATH)));
        FileUtil.deleteDirectoryRecursion(Paths.get(FILE_FULL_PATH));
    }

    @Test
    void getResponseLineTest() {
        assertEquals(MBI_1, helper.getResponseLine(MBI_1, null, null));
        assertEquals(MBI_2 + "20240226N", helper.getResponseLine(MBI_2, DATE, false));
        assertEquals("A          20240226Y", helper.getResponseLine("A", DATE, true));
    }


    @Test
    void writeFileToFinalDestinationTest() throws IOException {
        createTestFile();
        helper.writeFileToFinalDestination(S3MockAPIExtension.S3_CLIENT);
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    @Test
    void getBucketNameTest() {
        assertEquals(getBucketName(), helper.getBucketName());
    }

    @Test
    void getUploadPathTest() {
        assertEquals(getUploadPath(), helper.getUploadPath());
    }

    private void createTestFile() throws IOException {
        PrintWriter writer = new PrintWriter(FILE_FULL_PATH, StandardCharsets.UTF_8);
        writer.println(MBI_1);
        writer.close();
    }
}
