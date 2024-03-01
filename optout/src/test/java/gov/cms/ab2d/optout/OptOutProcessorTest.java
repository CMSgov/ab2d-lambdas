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
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static gov.cms.ab2d.optout.OptOutConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({S3MockAPIExtension.class})
public class OptOutProcessorTest {
    private static final LambdaLogger logger = mock(LambdaLogger.class);
    private static final Connection dbConnection = mock(Connection.class);
    private static final PreparedStatement statement = mock(PreparedStatement.class);
    private static final String MBI = "DUMMY000001";
    private static final String VALID_LINE = MBI + "          NAME                                                        LASTNAME                                111 DUMMY ADDRESS                                                                                                                                                            TESTDATA                                DUMMY11DUMMY20230726202307261-800TY";
    private final String INVALID_LINE = "TRL_BENEDATARSP202307260000000009";
    private final String EXPECTED_ACCEPTED_LINE = VALID_LINE + "                                                                                          Accepted  00";
    static OptOutProcessor optOutProcessor;

    @BeforeAll
    static void beforeAll() throws SQLException {
        var dbUtil = mockStatic(DatabaseUtil.class);
        dbUtil.when(DatabaseUtil::getConnection).thenReturn(dbConnection);
        when(dbConnection.prepareStatement(anyString())).thenReturn(statement);
    }

    @BeforeEach
    void beforeEach() throws URISyntaxException {
        // var creds = StaticCredentialsProvider.create(AwsSessionCredentials.create("test", "test", ""));
        optOutProcessor = spy(new OptOutProcessor(TEST_FILE_NAME, TEST_ENDPOINT, logger));
    }

    @Test
    void processTest() throws IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8));
        optOutProcessor.process();
        assertEquals(4, optOutProcessor.optOutResultMap.size());
        verify(optOutProcessor, times(4)).createOptOutInformation(anyString(), anyLong());
        verify(optOutProcessor, times(2)).updateOptOut(any(OptOutInformation.class), any(Connection.class));
        verify(optOutProcessor, times(1)).createResponseContent();
        //Because map contains records with insertion error
        Assertions.assertTrue(S3MockAPIExtension.isObjectExists(TEST_FILE_NAME));
    }

    @Test
    void createOptOutInformationValidTest1() {
        Optional<OptOutInformation> optOutInformation = optOutProcessor.createOptOutInformation(VALID_LINE, 1L);
        assertTrue(optOutInformation.isPresent());
        assertEquals(1L, optOutInformation.get().getLineNumber());
        assertEquals(VALID_LINE, optOutInformation.get().getText());
        assertEquals(MBI, optOutInformation.get().getMbi());
        assertTrue(optOutInformation.get().isOptOut());
    }

    @Test
    void createOptOutInformationValidTest2() {
        Optional<OptOutInformation> optOutInformation = optOutProcessor.createOptOutInformation(VALID_LINE.substring(0, VALID_LINE.length() - 1) + 'N', 1L);
        assertTrue(optOutInformation.isPresent());
        assertFalse(optOutInformation.get().isOptOut());
    }

    @Test
    void optOutResultMapValidTest() {
        optOutProcessor.createOptOutInformation(VALID_LINE, 1L);
        assertEquals(1, optOutProcessor.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessor.optOutResultMap.get(1L);
        assertEquals(ReasonCode.ACCEPTED, mapValue.getReasonCode());
        assertEquals(RecordStatus.ACCEPTED, mapValue.getRecordStatus());
    }

    @Test
    void createOptOutInformationInvalidTest() {
        var optOutInformation = optOutProcessor.createOptOutInformation(INVALID_LINE, 0L);
        assertFalse(optOutInformation.isPresent());
    }

    @Test
    void optOutResultMapParseErrorTest() {
        optOutProcessor.createOptOutInformation(INVALID_LINE, 0L);
        assertEquals(1, optOutProcessor.optOutResultMap.size());
        var mapValue = optOutProcessor.optOutResultMap.get(0L);
        assertEquals(ReasonCode.PARSE_ERROR, mapValue.getReasonCode());
        assertEquals(RecordStatus.REJECTED, mapValue.getRecordStatus());
    }

    @Test
    void createResponseOptOutContentTest() {
        optOutProcessor.createOptOutInformation(VALID_LINE, 1L);
        assertEquals(EXPECTED_ACCEPTED_LINE, optOutProcessor.createResponseContent());
    }

    @Test
    void createMultipleResponseOptOutContentTest() throws ParseException {
        optOutProcessor.optOutResultMap.put(0L, new OptOutResult(new OptOutInformation(0L, INVALID_LINE), RecordStatus.REJECTED, ReasonCode.PARSE_ERROR));
        optOutProcessor.optOutResultMap.put(1L, new OptOutResult(new OptOutInformation(MBI, getTestTimestamp(), true, 1L, VALID_LINE), RecordStatus.ACCEPTED, ReasonCode.ACCEPTED));
        optOutProcessor.optOutResultMap.put(2L, new OptOutResult(new OptOutInformation(2L, INVALID_LINE), RecordStatus.REJECTED, ReasonCode.PARSE_ERROR));
        var expectedText = INVALID_LINE + LINE_SEPARATOR
                + EXPECTED_ACCEPTED_LINE + LINE_SEPARATOR
                + INVALID_LINE;
        assertEquals(expectedText, optOutProcessor.createResponseContent());
    }

    @Test
    void updateOptOutTest() {
        Optional<OptOutInformation> optOutInformation = optOutProcessor.createOptOutInformation(VALID_LINE, 1L);
        assertTrue(optOutInformation.isPresent());
        optOutProcessor.updateOptOut(optOutInformation.get(), dbConnection);
        assertEquals(1, optOutProcessor.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessor.optOutResultMap.get(1L);
        assertEquals(ReasonCode.ACCEPTED, mapValue.getReasonCode());
        assertEquals(RecordStatus.ACCEPTED, mapValue.getRecordStatus());
    }

    @Test
    void updateOptOutInvalidTest() throws SQLException {
        Optional<OptOutInformation> optOutInformation = optOutProcessor.createOptOutInformation(VALID_LINE, 1L);
        when(dbConnection.prepareStatement(anyString())).thenThrow(SQLException.class);
        assertTrue(optOutInformation.isPresent());
        optOutProcessor.updateOptOut(optOutInformation.get(), dbConnection);
        assertEquals(1, optOutProcessor.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessor.optOutResultMap.get(1L);
        assertEquals(ReasonCode.INSERT_ERROR, mapValue.getReasonCode());
        assertEquals(RecordStatus.REJECTED, mapValue.getRecordStatus());
    }

    private Timestamp getTestTimestamp() throws ParseException {
        var dateFormat = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN);
        var date = dateFormat.parse("20230726");
        var time = date.getTime();
        return new Timestamp(time);
    }

}