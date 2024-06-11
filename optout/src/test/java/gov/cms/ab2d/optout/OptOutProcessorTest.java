package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;
import static gov.cms.ab2d.optout.OptOutConstantsTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({S3MockAPIExtension.class})
class OptOutProcessorTest {
    private static final ResultSet resultSet = mock(ResultSet.class);
    private static final PreparedStatement preparedStatement = mock(PreparedStatement.class);
    private static final Connection dbConnection = mock(Connection.class);
    private static final MockedStatic<OptOutParameterStore> parameterStore = mockStatic(OptOutParameterStore.class);
    private static final String DATE = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
    private static final String MBI = "DUMMY000001";
    private static final String TRAILER_COUNT = "0000000001";
    private static String validLine(char isOptOut) {
        return MBI + isOptOut;
    }
    static OptOutProcessor optOutProcessing;

    @BeforeAll
    static void beforeAll() throws SQLException {
        parameterStore.when(OptOutParameterStore::getParameterStore).thenReturn(new OptOutParameterStore("", "", "", ""));

        mockStatic(DriverManager.class)
                .when(() ->  DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(dbConnection);
        when(dbConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(dbConnection.createStatement()).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery(anyString())).thenReturn(resultSet);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8), TEST_FILE_NAME);
        parameterStore.when(OptOutParameterStore::getParameterStore).thenReturn(new OptOutParameterStore("", "", "", ""));
        optOutProcessing = spy(new OptOutProcessor(mock(LambdaLogger.class)));
        optOutProcessing.isRejected = false;
    }

    @AfterEach
    void afterEach() {
        S3MockAPIExtension.deleteFile(TEST_FILE_NAME);
    }

    @Test
    void processTest() throws URISyntaxException {
        optOutProcessing.isRejected = false;
        OptOutResults results = optOutProcessing.process(TEST_FILE_NAME, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT);
        assertEquals(7, optOutProcessing.optOutInformationList.size());
        
        assertEquals(3, results.getOptInToday());
        assertEquals(4, results.getOptOutToday());
        assertEquals(optOutProcessing.optOutInformationList.size(), results.getTotalToday());
    }

    @Test
    void processEmptyFileTest() throws IOException, URISyntaxException {
        var emptyFileName = "emptyDummy.txt";
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + emptyFileName), StandardCharsets.UTF_8), emptyFileName);
        OptOutResults results = optOutProcessing.process(emptyFileName, TEST_BFD_BUCKET_NAME, TEST_ENDPOINT);
        assertEquals(0, optOutProcessing.optOutInformationList.size());
        assertEquals(optOutProcessing.optOutInformationList.size(), results.getTotalToday());
        S3MockAPIExtension.deleteFile(emptyFileName);
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
        optOutProcessing.optOutInformationList.add(new OptOutInformation(MBI, true));
        var expectedLine = MBI + DATE + "Y" + RecordStatus.ACCEPTED;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void createRejectedResponseTest() {
        optOutProcessing.isRejected = true;
        optOutProcessing.optOutInformationList.add(new OptOutInformation(MBI, false));
        var expectedLine = MBI + "        " + "N" + RecordStatus.REJECTED;
        var expectedText = AB2D_HEADER_CONF + DATE + LINE_SEPARATOR
                + expectedLine + LINE_SEPARATOR
                + AB2D_TRAILER_CONF + DATE + TRAILER_COUNT;
        assertEquals(expectedText, optOutProcessing.createResponseContent());
    }

    @Test
    void updateOptOutTest() {
        optOutProcessing.updateOptOut();
        assertFalse(optOutProcessing.isRejected);
    }

    @Test
    void updateOptOutExceptionTest() throws SQLException {
        when(dbConnection.prepareStatement(anyString())).thenThrow(SQLException.class);
        optOutProcessing.updateOptOut();
        // Insertion error exists
        assertTrue(optOutProcessing.isRejected);
        assertTrue(S3MockAPIExtension.isObjectExists(TEST_FILE_NAME));
    }

    @Test
    void getEffectiveDateTest() {
        optOutProcessing.isRejected = false;
        assertEquals(DATE, optOutProcessing.getEffectiveDate(DATE));
        optOutProcessing.isRejected = true;
        assertEquals("        ", optOutProcessing.getEffectiveDate(DATE));
    }

    @Test
    void getRecordStatusTest() {
        optOutProcessing.isRejected = false;
        assertEquals(RecordStatus.ACCEPTED.toString(), optOutProcessing.getRecordStatus());
        optOutProcessing.isRejected = true;
        assertEquals(RecordStatus.REJECTED.toString(), optOutProcessing.getRecordStatus());
    }

    @Test
    void getOptOutResultsTest() throws SQLException {
        final String optInResultSetString = "optin";
        final String optOutResultSetString = "optout";

        final int optInTotalCount = 9;
        final int optOutTotalCount = 7;

        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getInt(optInResultSetString)).thenReturn(optInTotalCount);
        when(resultSet.getInt(optOutResultSetString)).thenReturn(optOutTotalCount);

        optOutProcessing.optOutInformationList.add(new OptOutInformation(MBI, true));
        optOutProcessing.optOutInformationList.add(new OptOutInformation("DUMMY000002", false));

        OptOutResults results = optOutProcessing.getOptOutResults();
        assertNotNull(results);
        assertEquals(1, results.getOptInToday());
        assertEquals(1, results.getOptOutToday());
        assertEquals(optInTotalCount, results.getOptInTotal());
        assertEquals(optOutTotalCount, results.getOptOutTotal());
    }

}