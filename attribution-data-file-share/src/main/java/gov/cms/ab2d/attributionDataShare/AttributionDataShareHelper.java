package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHelper {

    static ArrayList<String> getFileContent(Connection connection, LambdaLogger logger) {
        var result = new ArrayList<String>();
        var date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        try (var stmt = connection.createStatement()) {
            var rs = getExecuteQuery(stmt);
            result.add(FIRST_LINE + date + LINE_SEPARATOR);
            int records = 0;
            long totalRecords = 0;
            String chunkLine = "";
            while (rs.next()) {
                var line = getResponseLine(
                        rs.getString(1), rs.getTimestamp(2), rs.getBoolean(3)) + LINE_SEPARATOR;
                chunkLine += line;
                records++;
                totalRecords++;
                if (records == 32768) {
                    result.add(chunkLine);
                    chunkLine = "";
                    records = 0;
                }
            }
            result.add(chunkLine);
            result.add(LAST_LINE + date + String.format("%010d", totalRecords));
          //  content.append(LAST_LINE).append(date).append(String.format("%010d", totalRecords));
            return result;
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

    public static void multiPartUploadFileToS3Bucket(ArrayList<String> fileContent, String fileName, S3Client s3Client, LambdaLogger logger) {

        // Initiate the multipart upload.
        var createMultipartUploadResponse = s3Client.createMultipartUpload(b -> b
                .bucket(getBucketName())
                .key(getUploadPath() + fileName));
        var uploadId = createMultipartUploadResponse.uploadId();
        // Upload the parts of the file.

        List<CompletedPart> completedParts = new ArrayList<>();
        for (int partNumber = 1; partNumber < fileContent.size(); partNumber++) {

            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(getBucketName())
                    .key(getUploadPath() + fileName)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartResponse partResponse = s3Client.uploadPart(
                    uploadPartRequest,
                    RequestBody.fromString(fileContent.get(partNumber)));

            CompletedPart part = CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(partResponse.eTag())
                    .build();
            completedParts.add(part);
        }
        CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                .bucket(getBucketName())
                .key(getUploadPath() + fileName)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload -> completedMultipartUpload.parts(completedParts))
                .build();

        // Complete the multipart upload.
        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
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
