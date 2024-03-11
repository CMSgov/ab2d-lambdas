package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHandler implements RequestStreamHandler {

    // Writes out a file to the FILE_PATH.
    // I.E: "P.AB2D.NGD.REQ.D240209.T1122001.OUT"

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
        String fileName = REQ_FILE_NAME + currentDate + REQ_FILE_FORMAT;
        String fileFullPath = FILE_PATH + fileName;
        AttributionDataShareHelper helper = helperInit(fileName, fileFullPath, logger);
        try {
            helper.copyDataToFile(DatabaseUtil.getConnection());
            helper.writeFileToFinalDestination(getS3Client(ENDPOINT));
        } catch (NullPointerException | URISyntaxException ex) {
            throwAttributionDataShareException(logger, ex);
        } finally {
            FileUtil.deleteDirectoryRecursion(Paths.get(fileFullPath));
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    S3Client getS3Client(String endpoint) throws URISyntaxException {
        return S3Client.builder()
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint))
                .build();
    }

    AttributionDataShareHelper helperInit(String fileName, String fileFullPath, LambdaLogger logger) {
        return new AttributionDataShareHelper(fileName, fileFullPath, logger);
    }

    void throwAttributionDataShareException(LambdaLogger logger, Exception ex) {
        logger.log(ex.getMessage());
        throw new AttributionDataShareException(ex.getMessage(), ex);
    }

}
