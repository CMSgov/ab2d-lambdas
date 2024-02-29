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
public class OptOutProcessingTest {
    private static final LambdaLogger logger = mock(LambdaLogger.class);
    private static final Connection dbConnection = mock(Connection.class);
    private static final PreparedStatement statement = mock(PreparedStatement.class);
    private static final String VALID_LINE = "DUMMY000001          NAME                                                        LASTNAME                                111 DUMMY ADDRESS                                                                                                                                                            TESTDATA                                DUMMY11DUMMY20230726202307261-800TY";
    private final String INVALID_LINE = "TRL_BENEDATARSP202307260000000009";
    private final String EXPECTED_ACCEPTED_LINE = "DUMMY000001          NAME                                                        LASTNAME                                111 DUMMY ADDRESS                                                                                                                                                            TESTDATA                                DUMMY11DUMMY20230726202307261-800TY                                                                                          Accepted  00";
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
        optOutProcessing = spy(new OptOutProcessing(TEST_FILE_NAME, TEST_ENDPOINT, logger));
    }

    @Test
    void processTest() throws IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8));
        optOutProcessing.process();
        assertEquals(4, optOutProcessing.optOutResultMap.size());
        verify(optOutProcessing, times(4)).createOptOutInformation(anyString(), anyLong());
        verify(optOutProcessing, times(2)).updateOptOut(any(OptOutInformation.class), any(Connection.class));
        verify(optOutProcessing, times(1)).createResponseContent();
        //Because map contains records with insertion error
        Assertions.assertTrue(S3MockAPIExtension.isObjectExists(TEST_FILE_NAME));
    }

    @Test
    void createOptOutInformationValidTest1() {
        Optional<OptOutInformation> optOutInformation = optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        assertTrue(optOutInformation.isPresent());
        assertEquals(1L, optOutInformation.get().getLineNumber());
        assertEquals(VALID_LINE, optOutInformation.get().getText());
        assertEquals("DUMMY000001", optOutInformation.get().getMbi());
        assertTrue(optOutInformation.get().isOptOut());
    }

    @Test
    void createOptOutInformationValidTest2() {
        Optional<OptOutInformation> optOutInformation = optOutProcessing.createOptOutInformation(VALID_LINE.substring(0, VALID_LINE.length() - 1) + 'N', 1L);
        assertTrue(optOutInformation.isPresent());
        assertFalse(optOutInformation.get().isOptOut());
    }

    @Test
    void optOutResultMapValidTest() {
        optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessing.optOutResultMap.get(1L);
        assertEquals(ReasonCode.ACCEPTED, mapValue.getReasonCode());
        assertEquals(RecordStatus.ACCEPTED, mapValue.getRecordStatus());
    }

    @Test
    void createOptOutInformationInvalidTest() {
        var optOutInformation = optOutProcessing.createOptOutInformation(INVALID_LINE, 0L);
        assertFalse(optOutInformation.isPresent());
    }

    @Test
    void optOutResultMapParseErrorTest() {
        optOutProcessing.createOptOutInformation(INVALID_LINE, 0L);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        var mapValue = optOutProcessing.optOutResultMap.get(0L);
        assertEquals(ReasonCode.PARSE_ERROR, mapValue.getReasonCode());
        assertEquals(RecordStatus.REJECTED, mapValue.getRecordStatus());
    }

    @Test
    void createResponseOptOutContentTest() {
        optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        assertEquals(EXPECTED_ACCEPTED_LINE, optOutProcessing.createResponseContent());
    }

    @Test
    void createMultipleResponseOptOutContentTest() throws ParseException {
        optOutProcessing.optOutResultMap.put(0L, new OptOutResult(new OptOutInformation(0L, INVALID_LINE), RecordStatus.REJECTED, ReasonCode.PARSE_ERROR));
        optOutProcessing.optOutResultMap.put(1L, new OptOutResult(new OptOutInformation("7GU6ME5FA64", getTestTimestamp(), true, 1L, VALID_LINE), RecordStatus.ACCEPTED, ReasonCode.ACCEPTED));
        optOutProcessing.optOutResultMap.put(2L, new OptOutResult(new OptOutInformation(2L, INVALID_LINE), RecordStatus.REJECTED, ReasonCode.PARSE_ERROR));
        var expectedText = INVALID_LINE + LINE_SEPARATOR
                + EXPECTED_ACCEPTED_LINE + LINE_SEPARATOR
                + INVALID_LINE;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void updateOptOutTest() {
        Optional<OptOutInformation> optOutInformation = optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        assertTrue(optOutInformation.isPresent());
        optOutProcessing.updateOptOut(optOutInformation.get(), dbConnection);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessing.optOutResultMap.get(1L);
        assertEquals(ReasonCode.ACCEPTED, mapValue.getReasonCode());
        assertEquals(RecordStatus.ACCEPTED, mapValue.getRecordStatus());
    }

    @Test
    void updateOptOutInvalidTest() throws SQLException {
        Optional<OptOutInformation> optOutInformation = optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        when(dbConnection.prepareStatement(anyString())).thenThrow(SQLException.class);
        assertTrue(optOutInformation.isPresent());
        optOutProcessing.updateOptOut(optOutInformation.get(), dbConnection);
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        OptOutResult mapValue = optOutProcessing.optOutResultMap.get(1L);
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
