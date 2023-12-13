package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static gov.cms.ab2d.optout.OptOutConstants.EFFECTIVE_DATE_PATTERN;
import static gov.cms.ab2d.optout.OptOutConstants.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class OptOutProcessingTest {

    private static final MockedStatic<OptOutS3> optOutS3 = mockStatic(OptOutS3.class);
    private static final LambdaLogger logger = mock(LambdaLogger.class);
    private static final Connection dbConnection = mock(Connection.class);
    private static final PreparedStatement statement = mock(PreparedStatement.class);
    private static final String VALID_LINE = "7GU6ME5FA64          PHYLLIS                                                     FELSEN                                  195705211909 CLINTON ST                                                                                                                                                      LONGVIEW                                TX756042155F20230726202307261-800TY";
    private final String INVALID_LINE = "TRL_BENEDATARSP202307260000000009";
    private final String EXPECTED_ACCEPTED_LINE = "7GU6ME5FA64          PHYLLIS                                                     FELSEN                                  195705211909 CLINTON ST                                                                                                                                                      LONGVIEW                                TX756042155F20230726202307261-800TY                                                                                          Accepted  00";
    static OptOutProcessing optOutProcessing;

    @BeforeAll
    static void beforeAll() throws SQLException {
        var dbUtil = mockStatic(DatabaseUtil.class);
        optOutS3.when(() -> OptOutS3.openFileS3(anyString())).thenReturn(new BufferedReader(new StringReader(VALID_LINE)));
        dbUtil.when(DatabaseUtil::getConnection).thenReturn(dbConnection);
        when(dbConnection.prepareStatement(anyString())).thenReturn(statement);
    }

    @BeforeEach
    void beforeEach() {
        optOutProcessing = spy(new OptOutProcessing("", logger));
    }

    @Test
    void processTest() {
        optOutProcessing.process();
        assertEquals(1, optOutProcessing.optOutResultMap.size());
        optOutS3.verify(() -> OptOutS3.openFileS3(anyString()), times(1));
        verify(optOutProcessing, times(1)).createOptOutInformation(anyString(), anyLong());
        verify(optOutProcessing, times(1)).updateOptOut(any(OptOutInformation.class), any(Connection.class));
        verify(optOutProcessing, times(1)).createResponseContent();

        optOutS3.verify(() -> OptOutS3.createResponseOptOutFile(anyString()), times(1));
    }

    @Test
    void processFileFromS3Test() throws IOException {
        optOutProcessing.processFileFromS3(new BufferedReader(new StringReader(VALID_LINE)));
        assertEquals(1, optOutProcessing.optOutResultMap.size());
    }

    @Test
    void createOptOutInformationValidTest() {
        Optional<OptOutInformation> optOutInformation = optOutProcessing.createOptOutInformation(VALID_LINE, 1L);
        assertTrue(optOutInformation.isPresent());
        assertEquals(1L, optOutInformation.get().getLineNumber());
        assertEquals(VALID_LINE, optOutInformation.get().getText());
        assertEquals("7GU6ME5FA64", optOutInformation.get().getMbi());
        assertTrue(optOutInformation.get().isOptOut());
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
    void updateOptOutTest() throws SQLException {
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
        when(dbConnection.prepareStatement(Mockito.anyString())).thenThrow(SQLException.class);
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
