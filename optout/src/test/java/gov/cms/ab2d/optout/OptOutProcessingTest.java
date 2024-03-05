package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({S3MockAPIExtension.class})
public class OptOutProcessingTest {
    private static final Connection dbConnection = mock(Connection.class);
    private static final PreparedStatement statement = mock(PreparedStatement.class);
    private static final String DATE = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
    private static final String MBI = "DUMMY000001";
    private static final String TRAILER_COUNT = "0000000001";

    private static String validLine(char isOptOut) {
        return MBI + isOptOut;
    }

    static OptOutProcessing optOutProcessing;

    @BeforeAll
    static void beforeAll() throws SQLException {
        var dbUtil = mockStatic(DatabaseUtil.class);
        dbUtil.when(DatabaseUtil::getConnection).thenReturn(dbConnection);
        when(dbConnection.prepareStatement(anyString())).thenReturn(statement);
    }

    @BeforeEach
    void beforeEach() throws URISyntaxException {
        // var creds = StaticCredentialsProvider.create(AwsSessionCredentials.create("test", "test", ""));
        optOutProcessing = spy(new OptOutProcessing(TEST_FILE_NAME, TEST_ENDPOINT, mock(LambdaLogger.class)));
    }

    @Test
    void processTest() throws IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8));
        optOutProcessing.process();
        assertEquals(7, optOutProcessing.optOutResultMap.size());
        // Confirmation file was created
        Assertions.assertTrue(S3MockAPIExtension.isObjectExists(CONF_FILE_NAME + new SimpleDateFormat(CONF_FILE_NAME_PATTERN).format(new Date())));
        // Response file was deleted after processing
        Assertions.assertFalse(S3MockAPIExtension.isObjectExists(TEST_FILE_NAME));

    }

    @Test
    void createTrueOptOutInformationTest() {
        var optOutInformation = optOutProcessing.createOptOutInformation(validLine('Y'));
        assertEquals(MBI, optOutInformation.getMbi());
        assertTrue(optOutInformation.getOptOutFlag());
    }

    @Test
    void createFalseOptOutInformationTest() {
        var optOutInformation = optOutProcessing.createOptOutInformation(validLine('N'));
        assertEquals(MBI, optOutInformation.getMbi());
        assertFalse(optOutInformation.getOptOutFlag());
    }

    @Test
    void createAcceptedResponseTest() {
        optOutProcessing.optOutResultMap.put(1L, new OptOutResult(new OptOutInformation(MBI, true), RecordStatus.ACCEPTED));
        var expectedLine = MBI + DATE + "Y" + RecordStatus.ACCEPTED.status + "  " + RecordStatus.ACCEPTED.code;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void createRejectedResponseTest() {
        optOutProcessing.optOutResultMap.put(1L, new OptOutResult(new OptOutInformation(MBI, false), RecordStatus.REJECTED));
        var expectedLine = MBI + "        " + "N" + RecordStatus.REJECTED.status + "  " + RecordStatus.REJECTED.code;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void updateOptOutTest() {
        var optOutInformation = optOutProcessing.createOptOutInformation(validLine('Y'));
        optOutProcessing.updateOptOut(1L, optOutInformation, dbConnection);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessing.optOutResultMap.get(1L);
        assertEquals(RecordStatus.ACCEPTED, mapValue.getRecordStatus());
    }

    @Test
    void updateOptOutExceptionTest() throws SQLException {
        var optOutInformation = optOutProcessing.createOptOutInformation(validLine('Y'));
        when(dbConnection.prepareStatement(anyString())).thenThrow(SQLException.class);
        optOutProcessing.updateOptOut(1L, optOutInformation, dbConnection);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessing.optOutResultMap.get(1L);
        assertEquals(RecordStatus.REJECTED, mapValue.getRecordStatus());
    }

}