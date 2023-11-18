package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.*;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareHandlerConstants.*;

public class AttributionDataShareHandler implements RequestStreamHandler {

    // Writes out a file to the FILE_PATH.
    // I.E: "ab2d-beneids_2023-08-16T12:08:56.235-0700.txt"
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat(PATTERN).format(new Date());
        String fileFullPath = FILE_PATH + FILE_PARTIAL_NAME + currentDate + FILE_FORMAT;
        try {
            copyDataToFile(fileFullPath, logger);
            writeFileToFinalDestination();
        } catch (NullPointerException ex) {
            throwAttributionDataShareException(logger, ex);
        } finally {
            FileUtil.deleteDirectoryRecursion(Paths.get(fileFullPath));
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    void copyDataToFile(String filePath, LambdaLogger logger) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            Connection dbConnection = DatabaseUtil.getConnection();
            CopyManager copyManager = new CopyManager((BaseConnection) dbConnection);
            copyManager.copyOut(COPY_STATEMENT, writer);
        } catch (SQLException ex) {
            logger.log(ex.getMessage());
        } catch (IOException ex) {
            throwAttributionDataShareException(logger, ex);
        }
    }

    // Handle writing to the target destination.
    // This may not be necessary, but I am assuming we will want to store these files somewhere
    // other than the disk the container is running on. Requirements still to be determined.
    private void writeFileToFinalDestination() {
        // TODO
    }

    void throwAttributionDataShareException(LambdaLogger logger, Exception ex) {
        logger.log(ex.getMessage());
        throw new AttributionDataShareException(ex);
    }

}
