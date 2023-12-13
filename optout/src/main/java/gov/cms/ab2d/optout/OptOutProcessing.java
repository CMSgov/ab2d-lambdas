package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.databasemanagement.DatabaseUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutProcessing {

    private final String fileName;
    private final LambdaLogger logger;
    public SortedMap<Long, OptOutResult> optOutResultMap;

    public OptOutProcessing(String fileName, LambdaLogger logger) {
        this.fileName = fileName;
        this.logger = logger;
        this.optOutResultMap = new TreeMap<>();
    }

    public void process() {
        try {
            processFileFromS3(OptOutS3.openFileS3(fileName));
            OptOutS3.createResponseOptOutFile(createResponseContent());
            //    OptOutS3.deleteFileFromS3();
        } catch (IOException ex) {
            throw new OptOutException(ex);
        }
    }

    public void processFileFromS3(BufferedReader reader) throws IOException {
        var dbConnection = DatabaseUtil.getConnection();
        String line;
        var lineNumber = 0L;
        while ((line = reader.readLine()) != null) {
            var optOutInformation = createOptOutInformation(line, lineNumber);
            // If the file line was parsed successfully, update the optout values in the database
            optOutInformation.ifPresent(information -> updateOptOut(information, dbConnection));
            lineNumber++;
        }
    }

    public Optional<OptOutInformation> createOptOutInformation(String information, long lineNumber) throws IllegalArgumentException {
        try {
            var mbi = information.substring(MBI_INDEX_START, MBI_INDEX_END).trim();
            var effectiveDate = convertToDate(information.substring(EFFECTIVE_DATE_INDEX_START, EFFECTIVE_DATE_INDEX_END));
            var optOutFlag = (information.charAt(OPTOUT_FLAG_INDEX) == 'Y');

            var optOutInformation = new OptOutInformation(mbi, effectiveDate, optOutFlag, lineNumber, information);
            // The file line was parsed successfully
            optOutResultMap.put(lineNumber, new OptOutResult(optOutInformation, RecordStatus.ACCEPTED, ReasonCode.ACCEPTED));
            return Optional.of(optOutInformation);
        } catch (NumberFormatException | StringIndexOutOfBoundsException | ParseException ex) {
            logger.log("Lambda can not parse the line: " + lineNumber);
            // The file line was parsed with an error
            optOutResultMap.put(lineNumber, new OptOutResult(new OptOutInformation(lineNumber, information), RecordStatus.REJECTED, ReasonCode.PARSE_ERROR));
        }
        return Optional.empty();
    }

    public void updateOptOut(OptOutInformation optOutInformation, Connection dbConnection) {
        try (var statement = dbConnection.prepareStatement(UPDATE_STATEMENT)) {
            logger.log("Mbi: " + optOutInformation.getMbi() + ", OptOut Flag: " + optOutInformation.isOptOut());
            prepareInsert(optOutInformation, statement);
            statement.execute();
        } catch (SQLException ex) {
            optOutResultMap.put(optOutInformation.getLineNumber(),
                    new OptOutResult(
                            new OptOutInformation(optOutInformation.getLineNumber(), optOutInformation.getText()),
                            RecordStatus.REJECTED,
                            ReasonCode.INSERT_ERROR));
            logger.log(ex.getMessage());
        }
    }

    public String createResponseContent() {
        var responseContent = new StringBuilder();
        for (var optOutResult : optOutResultMap.entrySet()) {
            var line = optOutResult.getValue();
            var text = line.getOptOutInformation().getText();
            // First and last lines don't contain optout data and are written as is
            if (optOutResult.getKey() == 0 || optOutResult.getKey() == optOutResultMap.size() - 1) {
                responseContent.append(text);
            } else {
                var result = new StringBuilder(text);
                // Adding spaces to the end of a string to achieve the RecordStatus position index
                if (text.length() < DEFAULT_LINE_LENGTH)
                    result.append(" ".repeat(Math.max(0, DEFAULT_LINE_LENGTH - text.length())));

                result.append(String.format(RECORD_STATUS_PATTERN, line.getRecordStatus()));
                result.append(line.getReasonCode());
                responseContent.append(result);
            }
            responseContent.append(LINE_SEPARATOR);
        }
        // Remove last empty line
        responseContent.delete(responseContent.lastIndexOf(LINE_SEPARATOR), responseContent.length());
        return responseContent.toString();
    }


    private static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.isOptOut());
        statement.setTimestamp(2, optOut.getEffectiveDate());
        statement.setString(3, optOut.getMbi());
        statement.setString(4, optOut.getMbi());
        statement.addBatch();
    }

    private Timestamp convertToDate(String date) throws ParseException {
        var dateFormat = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN);
        var parsedDate = dateFormat.parse(date);
        return new Timestamp(parsedDate.getTime());
    }

}
