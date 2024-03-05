package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutProcessing {
    private final LambdaLogger logger;
    public SortedMap<Long, OptOutResult> optOutResultMap;
    private final OptOutS3 optOutS3;

    public OptOutProcessing(String fileName, String endpoint, LambdaLogger logger) throws URISyntaxException {
        this.logger = logger;
        this.optOutResultMap = new TreeMap<>();
        var s3Client = S3Client.builder()
                //    .credentialsProvider(credentials)
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint))
                .build();
        optOutS3 = new OptOutS3(s3Client, fileName, logger);
    }

    public void process() {
        processFileFromS3(optOutS3.openFileS3());
        optOutS3.createResponseOptOutFile(createResponseContent());
    }

    public void processFileFromS3(BufferedReader reader) {
        var dbConnection = DatabaseUtil.getConnection();
        String line;
        var lineNumber = 0L;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HEADER_RESP) && !line.startsWith(TRAILER_RESP)) {
                    var optOutInformation = createOptOutInformation(line);
                    updateOptOut(lineNumber, optOutInformation, dbConnection);
                }
                lineNumber++;
            }
        } catch (IOException ex) {
            logger.log("An error occurred during file processing. " + ex.getMessage());
            throw new OptOutException("An error occurred during file processing.", ex);
        }
    }

    public OptOutInformation createOptOutInformation(String information) {
        var mbi = information.substring(MBI_INDEX_START, MBI_INDEX_END).trim();
        var optOutFlag = (information.charAt(OPTOUT_FLAG_INDEX) == 'Y');
        return new OptOutInformation(mbi, optOutFlag);
    }

    public void updateOptOut(long lineNumber, OptOutInformation optOutInformation, Connection dbConnection) {
        try (var statement = dbConnection.prepareStatement(UPDATE_STATEMENT)) {
            prepareInsert(optOutInformation, statement);
            statement.execute();
            optOutResultMap.put(lineNumber, new OptOutResult(optOutInformation, RecordStatus.ACCEPTED));
        } catch (SQLException ex) {
            logger.log("There is an insertion error on the line " + lineNumber);
            logger.log(ex.getMessage());
            optOutResultMap.put(lineNumber, new OptOutResult(optOutInformation, RecordStatus.REJECTED));
        }
    }

    public String createResponseContent() {
        String date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        var responseContent = new StringBuilder();
        responseContent.append(AB2D_HEADER_CONF).append(date);
        responseContent.append(LINE_SEPARATOR);

        for (var optOutResult : optOutResultMap.entrySet()) {
            var line = optOutResult.getValue();
            var info = line.getOptOutInformation();
            responseContent.append(info.getMbi());

            if (line.getRecordStatus() == RecordStatus.ACCEPTED)
                responseContent.append(date);
            else
                responseContent.append(" ".repeat(EFFECTIVE_DATE_LENGTH));

            responseContent.append((info.getOptOutFlag()) ? 'Y' : 'N');

            responseContent.append(String.format(RECORD_STATUS_PATTERN, line.getRecordStatus().status));
            responseContent.append(line.getRecordStatus().code);
            responseContent.append(LINE_SEPARATOR);
        }
        responseContent.append(AB2D_TRAILER_CONF).append(date).append(String.format("%010d", optOutResultMap.size()));

        return responseContent.toString();
    }

    private static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.getOptOutFlag());
        statement.setString(2, optOut.getMbi());
        statement.setString(3, optOut.getMbi());
        statement.addBatch();
    }

}
