package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class AttributionDataShareHandler implements RequestStreamHandler {

    private static final String FILE_PATH = "/opt/";
    private static final String FILE_PARTIAL_NAME = "ab2d-beneids_";
    private static final String FILE_FORMAT = ".txt";

    private static final String SELECT_ALL_FROM_COVERAGE = "SELECT DISTINCT current_mbi FROM public.coverage";

    // Writes out a file to the FILE_PATH.
    // I.E: "ab2d-beneids_2023-08-16T12:08:56.235-0700.txt"
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
        String fileFullPath = FILE_PATH + FILE_PARTIAL_NAME + currentDate + FILE_FORMAT;

        try (FileWriter fileWriter = new FileWriter(fileFullPath)) {
            Connection dbConnection = getConnection();

            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_FROM_COVERAGE);

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            if (resultSet.next() == false) {
                logger.log("ResultSet is empty...skipping writing to file");
            } else {
                logger.log("Writing to file...");
                do {
                    String result = resultSet.getString(1);
                    if (result != null || result != "") {
                        bufferedWriter.write(result);
                    }
                } while (resultSet.next());
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (NullPointerException | SQLException ex) {
            log(ex, logger);
        } finally {
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    // Maybe since this is reused, we could put it in some kind of common Utils class
    // that can be used by all of the Lambda functions if necessary?
    // It might make sense in lambda-lib::PropertiesUtil.java?
    private Connection getConnection() throws SQLException {
        Properties properties = PropertiesUtil.loadProps();
        return DriverManager.getConnection(properties.getProperty("DB_URL"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
    }

    // Handle writing to the target destination.
    // This may not be necessary, but I am assuming we will want to store these files somewhere
    // other than the disk the container is running on. Requirements still to be determined.
    private void writeFileToFinalDestination() {
        // TODO
    }

    private void log(Exception exception, LambdaLogger logger) {
        logger.log(exception.getMessage());
        throw new AttributionDataShareException(exception);
    }

}
