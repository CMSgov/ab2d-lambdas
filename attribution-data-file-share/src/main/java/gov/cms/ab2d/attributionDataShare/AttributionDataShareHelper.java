package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;


import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHandlerConstants.*;

public class AttributionDataShareHelper {
    LambdaLogger logger;
    String fileName;
    String fileFullPath;
    public AttributionDataShareHelper(String fileName, String fileFullPath, LambdaLogger logger){
        this.fileName = fileName;
        this.fileFullPath = fileFullPath;
        this.logger = logger;
    }
    void copyDataToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileFullPath, true))) {
            getCopyManager().copyOut(COPY_STATEMENT, writer);
        } catch (SQLException | IOException ex) {
            String errorMessage = "An error occurred while exporting data to a file. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }
    void writeFileToFinalDestination(S3Client s3Client) {
        try {
            var objectRequest = PutObjectRequest.builder()
                    .bucket(BFD_S3_BUCKET_NAME)
                    .key(fileName)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromFile(new File(fileFullPath)));
        } catch (AmazonS3Exception ex) {
            var errorMessage = "Response AttributionDataShare file cannot be created. ";
            logger.log(errorMessage + ex.getMessage());
            throw new AttributionDataShareException(errorMessage, ex);
        }
    }

    CopyManager getCopyManager() throws SQLException {
        return new CopyManager((BaseConnection) DatabaseUtil.getConnection());
    }
}
