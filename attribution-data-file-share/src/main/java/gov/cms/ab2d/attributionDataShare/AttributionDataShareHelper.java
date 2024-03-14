package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHelper {

    static String getFileContent(Connection connection, LambdaLogger logger) {
        var date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        var content = new StringBuilder();
        try (var stmt = connection.createStatement()) {
            var rs = getExecuteQuery(stmt);
            content.append(FIRST_LINE).append(date).append(LINE_SEPARATOR);
            long records = 0;
            while (rs.next()) {
                var line = getResponseLine(rs.getString(1), rs.getTimestamp(2), rs.getBoolean(3));
                content.append(line).append(LINE_SEPARATOR);
                records++;
            }
            content.append(LAST_LINE).append(date).append(String.format("%010d", records));
            return content.toString();
        } catch (SQLException ex) {
            String errorMessage = "An error occurred while exporting data to a file. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }

    static String getResponseLine(String currentMbi, Timestamp effectiveDate, Boolean optOutFlag) {
        var result = new StringBuilder();
        result.append(currentMbi);
        // Adding spaces to the end of a string to achieve the required position index
        if (currentMbi.length() < CURRENT_MBI_LENGTH)
            result.append(" ".repeat(Math.max(0, CURRENT_MBI_LENGTH - currentMbi.length())));

        if (effectiveDate != null) {
            result.append(new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(effectiveDate));
            result.append((optOutFlag) ? 'Y' : 'N');
        }
        return result.toString();
    }

    public static void writeFileToS3Bucket(String fileContent, String fileName, S3Client s3Client, LambdaLogger logger) {
        try {
            var objectRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(getUploadPath() + fileName)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromString(fileContent));
        } catch (AmazonS3Exception ex) {
            var errorMessage = "Response AttributionDataShare file cannot be created. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }

    public static String getBucketName() {
        return System.getenv(BUCKET_NAME_PROP);
    }

    public static String getUploadPath() {
        return System.getenv(UPLOAD_PATH_PROP) + "/";
    }

    static ResultSet getExecuteQuery(Statement statement) throws SQLException {
        return statement.executeQuery(SELECT_STATEMENT);
    }

    private AttributionDataShareHelper() {
    }

}
