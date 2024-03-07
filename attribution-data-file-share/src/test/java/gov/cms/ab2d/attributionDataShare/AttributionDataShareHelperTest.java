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
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;
import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHelper.getExecuteQuery;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith({S3MockAPIExtension.class})
public class AttributionDataShareHelperTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);

    String FILE_NAME = REQ_FILE_NAME + new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date()) + REQ_FILE_FORMAT;
    String FILE_FULL_PATH = FILE_PATH + FILE_NAME;

    String MBI = "DUMMY000001";
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
        rs.addColumn("mbi", Collections.singletonList(MBI));
        rs.addColumn("effective_date", Collections.singletonList(null));
        rs.addColumn("opt_out_flag", Collections.singletonList(true));
        when(connection.createStatement()).thenReturn(stmt);

        when(getExecuteQuery(stmt)).thenReturn(rs);
        assertDoesNotThrow(() -> helper.copyDataToFile(connection));

        assertTrue(Files.exists(Paths.get(FILE_FULL_PATH)));
        FileUtil.deleteDirectoryRecursion(Paths.get(FILE_FULL_PATH));
    }

    @Test
    void getResponseLineTest(){
        assertEquals(MBI +"        Y", helper.getResponseLine(MBI, null, true));
        assertEquals(MBI +"20240226N", helper.getResponseLine(MBI, Timestamp.valueOf("2024-02-26 00:00:00"), false));
        assertEquals("A                  Y", helper.getResponseLine("A", null, true));
    }


    @Test
    void writeFileToFinalDestinationTest() throws IOException {
        createTestFile();
        helper.writeFileToFinalDestination(S3MockAPIExtension.S3_CLIENT);
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    private void createTestFile() throws IOException {
        PrintWriter writer = new PrintWriter(FILE_FULL_PATH, StandardCharsets.UTF_8);
        writer.println("Test");
        writer.close();
    }
}
