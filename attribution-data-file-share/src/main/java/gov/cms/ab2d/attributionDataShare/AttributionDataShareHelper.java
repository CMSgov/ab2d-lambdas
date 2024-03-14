package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHelper {
    LambdaLogger logger;
    String fileName;
    String fileFullPath;

    public AttributionDataShareHelper(String fileName, String fileFullPath, LambdaLogger logger) {
        this.fileName = fileName;
        this.fileFullPath = fileFullPath;
        this.logger = logger;
    }

    void copyDataToFile(Connection connection) {
        String date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        try (var stmt = connection.createStatement();
             var writer = new BufferedWriter(new FileWriter(fileFullPath, true))) {
            long startSelect = System.currentTimeMillis();
            var rs = getExecuteQuery(stmt);
            long finishSelect = System.currentTimeMillis();

            logger.log("Select TIME ms: ---------- " + (finishSelect - startSelect));

            long startWrite = System.currentTimeMillis();
            writer.write(FIRST_LINE + date);
            writer.newLine();
            long records = 0;
            while (rs.next()) {
                var line = getResponseLine(rs.getString(1), rs.getTimestamp(2), rs.getBoolean(3));
                writer.write(line);
                writer.newLine();
                records++;
            }
            writer.write(LAST_LINE + date + String.format("%010d", records));

            long finishWrite = System.currentTimeMillis();
            logger.log("Write TIME ms: ---------- " + (finishWrite - startWrite));
        } catch (SQLException | IOException ex) {
            String errorMessage = "An error occurred while exporting data to a file. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }

    String getResponseLine(String currentMbi, Timestamp effectiveDate, Boolean optOutFlag) {
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

    void writeFileToFinalDestination(S3Client s3Client) {
        logger.log("filename " + fileName);
        long startUpload = System.currentTimeMillis();
        try {
            var objectRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(getUploadPath() + fileName)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromFile(new File(fileFullPath)));
        } catch (AmazonS3Exception ex) {
            var errorMessage = "Response AttributionDataShare file cannot be created. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }

        long finishUpload = System.currentTimeMillis();
        logger.log("Upload TIME ms: ---------- " + (finishUpload - startUpload));
    }

    String mtpUpload(S3AsyncClient s3AsyncClient) {
        String currentDate = new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
        var key = REQ_FILE_NAME + currentDate;
        logger.log("MTPU KEY " + key);
        long startUpload = System.currentTimeMillis();
        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                .putObjectRequest(b -> b.bucket(getBucketName()).key(getUploadPath() + key))
                .addTransferListener(LoggingTransferListener.create())
                .source(Paths.get(fileFullPath))
                .build();

        FileUpload fileUpload = transferManager.uploadFile(uploadFileRequest);

        CompletedFileUpload uploadResult = fileUpload.completionFuture().join();

        long finishUpload = System.currentTimeMillis();
        logger.log("Multipart Upload TIME ms: ---------- " + (finishUpload - startUpload));
        return uploadResult.response().eTag();
    }

    public String getBucketName() {
        return System.getenv(BUCKET_NAME_PROP);
    }

    public String getUploadPath() {
        return System.getenv(UPLOAD_PATH_PROP) + "/";
    }

    static ResultSet getExecuteQuery(Statement statement) throws SQLException {
        return statement.executeQuery(SELECT_STATEMENT);
    }

}
