package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.testutils.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;
import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstantsTest.*;
import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHelper.getExecuteQuery;
import static gov.cms.ab2d.attributionDataShare.S3MockAPIExtension.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith({S3MockAPIExtension.class})
public class AttributionDataShareTest {
    @SuppressWarnings({"rawtypes", "unused"})
    @Container
    private static final PostgreSQLContainer POSTGRE_SQL_CONTAINER = new AB2DPostgresqlContainer();
    LambdaLogger LOGGER = mock(LambdaLogger.class);

    @Test
    void copyDataToFileTest() throws SQLException {
        when(connection.createStatement()).thenReturn(stmt);

        var currentDate = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        String content = FIRST_LINE + currentDate + LINE_SEPARATOR +
                MBI_1 + DATE + 'Y' + LINE_SEPARATOR +
                MBI_2 + LINE_SEPARATOR +
                LAST_LINE + currentDate + "0000000002";
        when(getExecuteQuery(stmt)).thenReturn(resultSet());
        assertEquals(content, AttributionDataShareHelper.getFileContent(connection, LOGGER));
    }

    @Test
    void getResponseLineTest() {
        assertEquals(MBI_1, AttributionDataShareHelper.getResponseLine(MBI_1, null, null));
        assertEquals(MBI_2 + "20240226N", AttributionDataShareHelper.getResponseLine(MBI_2, DATE_TIME, false));
        assertEquals("A          20240226Y", AttributionDataShareHelper.getResponseLine("A", DATE_TIME, true));
    }


    @Test
    void writeFileToFinalDestinationTest() {
        AttributionDataShareHelper.writeFileToS3Bucket(MBI_1, FILE_NAME, S3_CLIENT, LOGGER);
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    @Test
    void getBucketNameTest() {

        assertEquals(getBucketName(), AttributionDataShareHelper.getBucketName());
    }

    @Test
    void getUploadPathTest() {

        assertEquals(getUploadPath(), AttributionDataShareHelper.getUploadPath());
    }

}
