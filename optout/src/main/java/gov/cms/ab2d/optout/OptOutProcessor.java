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

public class OptOutProcessor {
    private final LambdaLogger logger;
    public SortedMap<Long, OptOutInformation> optOutInformationMap;
    public boolean isRejected;
    private final OptOutS3 optOutS3;

    public OptOutProcessor(String fileName, String endpoint, LambdaLogger logger) throws URISyntaxException {
        this.logger = logger;
        this.optOutInformationMap = new TreeMap<>();
        var s3Client = S3Client.builder()
                //    .credentialsProvider(credentials)
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint))
                .build();
        isRejected = false;
        optOutS3 = new OptOutS3(s3Client, fileName, logger);
    }

    public void process() {
        processFileFromS3(optOutS3.openFileS3());
        optOutS3.createResponseOptOutFile(createResponseContent());
        if (!isRejected)
            optOutS3.deleteFileFromS3();
    }

    public void processFileFromS3(BufferedReader reader) {
        var dbConnection = DatabaseUtil.getConnection();
        String line;
        var lineNumber = 0L;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HEADER_RESP) && !line.startsWith(TRAILER_RESP)) {
                    var optOutInformation = createOptOutInformation(line);
                    optOutInformationMap.put(lineNumber, optOutInformation);
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
        } catch (SQLException ex) {
            logger.log("There is an insertion error on the line " + lineNumber);
            logger.log(ex.getMessage());
            isRejected = true;
        }
    }

    public String createResponseContent() {
        var date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        var responseContent = new StringBuilder();
        responseContent.append(AB2D_HEADER_CONF).append(date);
        responseContent.append(LINE_SEPARATOR);
        var recordStatus = getRecordStatus();
        var effectiveDate = getEffectiveDate(date);

        for (var optOutResult : optOutInformationMap.entrySet()) {
            var info = optOutResult.getValue();

            responseContent.append(info.getMbi())
                    .append(effectiveDate)
                    .append((info.getOptOutFlag()) ? 'Y' : 'N')
                    .append(recordStatus)
                    .append(LINE_SEPARATOR);
        }
        responseContent.append(AB2D_TRAILER_CONF).append(date).append(String.format("%010d", optOutInformationMap.size()));

        return responseContent.toString();
    }

    public String getRecordStatus() {
        return (isRejected) ? RecordStatus.REJECTED.toString() : RecordStatus.ACCEPTED.toString();
    }

    public String getEffectiveDate(String date){
        return (isRejected) ? " ".repeat(EFFECTIVE_DATE_LENGTH) : date;
    }

    private static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.getOptOutFlag());
        statement.setString(2, optOut.getMbi());
        statement.setString(3, optOut.getMbi());
        statement.addBatch();
    }

}