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
    
    private static final String filePartialName = "ab2d-beneids_";
    private static final String fileFormat = ".txt";

    private static final String filePath = "/opt/";

    private static final String SELECT_ALL_FROM_COVERAGE = "SELECT beneficiary_id from public.coverage";

    // Returns a string with the fileFullPath to the file we wrote out.
    // I.E: "ab2d-beneids_2023-08-16T12:08:56.235-0700.txt"
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());

        String fileFullPath = filePath + filePartialName + currentDate + fileFormat;

        try (FileWriter fileWriter = new FileWriter(fileFullPath)) {
            Connection dbConnection = getConnection();
            List<String> coverageDataList = fetchCoverageData(dbConnection, logger);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            logger.log("Trying write to file: " + fileFullPath);

            for (String result : coverageDataList) {
                bufferedWriter.write(result + System.lineSeparator());
            }
            logger.log("File was written to successfully.");
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

    private List<String> fetchCoverageData(Connection connection, LambdaLogger logger) throws SQLException {
        logger.log("fetching coverage arrtibution data...");

        List<String> outputData = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultset = statement.executeQuery(SELECT_ALL_FROM_COVERAGE);

            while(resultset.next()) {
                // This will need to be changed to support multiple data columns.
                // Currently will just return the value in the first column only.
                outputData.add(resultset.getString(1));
            }
        } catch (SQLException ex) {
            logger.log(ex.getMessage());
        } finally {
            logger.log("coverage attribution data retrieved.");
        }
        return outputData;
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
