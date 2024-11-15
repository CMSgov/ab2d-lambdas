package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.ParameterStoreUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutProcessor {
    private final LambdaLogger logger;
    public List<OptOutInformation> optOutInformationList;
    public boolean isRejected;

    ParameterStoreUtil parameterStore;

    public OptOutProcessor(LambdaLogger logger) {
        this.logger = logger;
        this.optOutInformationList = new ArrayList<>();
        isRejected = false;
        parameterStore = ParameterStoreUtil.getParameterStore(ROLE_PARAM, DB_HOST_PARAM, DB_USER_PARAM, DB_PASS_PARAM);
    }

    public OptOutResults process(String fileName, String bfdBucket, String endpoint) throws URISyntaxException {
        var optOutS3 = new OptOutS3(getS3Client(endpoint), fileName, bfdBucket, logger);
        processFileFromS3(optOutS3.openFileS3());
        updateOptOut();
        var name = optOutS3.createResponseOptOutFile(createResponseContent());
        logger.log("File with name " + name + " was uploaded to bucket: " + bfdBucket);
        if (!isRejected)
            optOutS3.deleteFileFromS3();
        return getOptOutResults();
    }

    public S3Client getS3Client(String endpoint) throws URISyntaxException {
        var client = S3Client.builder()
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint));

        if (endpoint.equals(ENDPOINT)) {
            var stsClient = StsClient
                    .builder()
                    .region(S3_REGION)
                    .build();

            var request = AssumeRoleRequest
                    .builder()
                    .roleArn(parameterStore.getRole())
                    .roleSessionName("roleSessionName")
                    .build();

            var credentials = StsAssumeRoleCredentialsProvider
                    .builder()
                    .stsClient(stsClient)
                    .refreshRequest(request)
                    .build();

            client.credentialsProvider(credentials);
        }
        return client.build();
    }

    public void processFileFromS3(BufferedReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HEADER_RESP) && !line.startsWith(TRAILER_RESP)) {
                    var optOutInformation = createOptOutInformation(line);
                    optOutInformationList.add(optOutInformation);
                }
            }
        } catch (IOException ex) {
            logger.log("An error occurred during file processing. " + ex.getMessage());
        }
    }

    public OptOutInformation createOptOutInformation(String information) {
        var mbi = information.substring(MBI_INDEX_START, MBI_INDEX_END).trim();
        var optOutFlag = (information.charAt(OPTOUT_FLAG_INDEX) == 'Y');
        return new OptOutInformation(mbi, optOutFlag);
    }

    public void updateOptOut() {
        try (var dbConnection = DriverManager.getConnection(parameterStore.getDbHost(), parameterStore.getDbUser(), parameterStore.getDbPassword());
             var statement = dbConnection.prepareStatement(UPDATE_STATEMENT)) {
            for (var optOutInformation : optOutInformationList) {
                statement.setBoolean(1, optOutInformation.getOptOutFlag());
                statement.setString(2, optOutInformation.getMbi());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            logger.log("There is an insertion error " + ex.getMessage());
            isRejected = true;
        }
    }

    public String createResponseContent() {
        var date = new SimpleDateFormat(EFFECTIVE_DATE_PATTERN).format(new Date());
        var responseContent = new StringBuilder()
                .append(AB2D_HEADER_CONF)
                .append(date)
                .append(LINE_SEPARATOR);
        var recordStatus = getRecordStatus();
        var effectiveDate = getEffectiveDate(date);

        for (var optOutResult : optOutInformationList) {
            responseContent.append(optOutResult.getMbi())
                    .append(effectiveDate)
                    .append((optOutResult.getOptOutFlag()) ? 'Y' : 'N')
                    .append(recordStatus)
                    .append(LINE_SEPARATOR);
        }
        var lastLine = new StringBuilder()
                .append(AB2D_TRAILER_CONF)
                .append(date)
                .append(String.format("%010d", optOutInformationList.size()));
        responseContent.append(lastLine);
        logger.log("File trailer: " + lastLine);
        return responseContent.toString();
    }

    public OptOutResults getOptOutResults() {
        int totalOptedIn = 0;
        int totalOptedOut = 0;

        try (var dbConnection = DriverManager.getConnection(parameterStore.getDbHost(), parameterStore.getDbUser(), parameterStore.getDbPassword());
             var statement = dbConnection.createStatement();
             ResultSet rs = statement.executeQuery(COUNT_STATEMENT)
        ) {
            while (rs.next()) {
                totalOptedIn = rs.getInt("optin");
                totalOptedOut = rs.getInt("optout");
            }

            int numberOptedIn = 0;
            int numberOptedOut = 0;

            for (OptOutInformation optOut : optOutInformationList) {
                if (optOut.getOptOutFlag()) {
                    numberOptedIn++;
                } else {
                    numberOptedOut++;
                }
            }
            return new OptOutResults(numberOptedIn, numberOptedOut, totalOptedIn, totalOptedOut);
        } catch (SQLException ex) {
           logger.log("There is an error " + ex.getMessage());
        }
        return null;
    }

    public String getRecordStatus() {
        return (isRejected) ? RecordStatus.REJECTED.toString() : RecordStatus.ACCEPTED.toString();
    }

    public String getEffectiveDate(String date) {
        return (isRejected) ? " ".repeat(EFFECTIVE_DATE_LENGTH) : date;
    }

}