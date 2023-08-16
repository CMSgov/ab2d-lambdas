package gov.cms.ab2d.attributionDataShare;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class AttributionDataShare {
    
    private static final String filePartialName = "ab2d-beneids_";
    private static final String fileFormat = ".txt";

    private static final String filePath = "/opt/";

    private static final String SELECT_ALL_FROM_COVERAGE = "SELECT beneficiary_id from public.coverage";

    // Returns a string with the fileFullPath to the file we wrote out.
    // I.E: "ab2d-beneids_16_08_"
    public String handleRequest(Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());

        String fileFullName = filePartialName + currentDate + fileFormat;
        
        String fileFullPath = filePath + fileFullName;

        try {
            Connection dbConnection = getConnection();
            List<String> coverageDataList = fetchCoverageData(dbConnection, SELECT_ALL_FROM_COVERAGE);

            FileWriter fileWriter = new FileWriter(fileFullPath);
            logger.log("Trying write to file: " + fileFullPath);


            for (String result : coverageDataList) {
                fileWriter.write(result + System.lineSeparator());
            }

        } catch (NullPointerException | SQLException | InterruptedException ex) {
            logger.log(ex.getMessage());
        } finally {
            fileWriter.close();
            logger.log("File was written to successfully.");
            logger.log("AttributionDataShare Lambda is completed");
        }
        return fileFullPath;
    }

    // Maybe since this is reused, we could put it in some kind of common Utils class
    // that can be used by all of the Lambda functions if necessary?
    // It might make sense in lambda-lib::PropertiesUtil.java?
    private Connection getConnection() throws SQLException {
        Properties properties = PropertiesUtil.loadProps();
        return DriverManager.getConnection(properties.getProperty("DB_URL"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
    }

    private List<String> fetchCoverageData(Connection connection, String query) throws SQLException {
        logger.log("fetching coverage arrtibution data...");

        List<String> outputData = new ArrayList<String>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultset = statement.executeQuery(query);

            while(rs.next()) {
                outputData.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            logger.log(ex.getMessage());
        } finally {
            logger.log("coverage attribution data retrieved.");
        }
    }
}
