package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class Utils implements Runnable{
    String fileFullPath;
    String select;
    Connection connection;
    BufferedWriter writer;
    LambdaLogger logger;
    private final CountDownLatch latch;

    public Utils(String fileFullPath, String select, Connection connection, BufferedWriter writer, CountDownLatch latch, LambdaLogger logger) {
        this.fileFullPath = fileFullPath;
        this.select = select;
        this.connection = connection;
        this.writer = writer;
        this.logger = logger;
        this.latch = latch;
    }

    @Override
    public void run() {
        String date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        try (var stmt = connection.createStatement()){
            long startSelect = System.currentTimeMillis();
            var rs = getExecuteQuery(select, stmt);
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
        finally {
            latch.countDown();
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

    static ResultSet getExecuteQuery(String select, Statement statement) throws SQLException {
        return statement.executeQuery(select);
    }

}
