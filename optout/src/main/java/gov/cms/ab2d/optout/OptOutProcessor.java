package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutProcessor {
    private final LambdaLogger logger;
    public SortedMap<Long, OptOutInformation> optOutInformationMap;
    public boolean isRejected;

    public OptOutProcessor(LambdaLogger logger) {
        this.logger = logger;
        this.optOutInformationMap = new TreeMap<>();
        isRejected = false;
    }

    public void process(String fileName, String bfdBucket, String endpoint) throws URISyntaxException {
        var optOutS3 = new OptOutS3(getS3Client(endpoint), fileName, bfdBucket, logger);



        logger.log("S3Client was created");
        processFileFromS3(optOutS3.openFileS3());
        optOutS3.createResponseOptOutFile(createResponseContent());
        if (!isRejected)
            optOutS3.deleteFileFromS3();
    }

    public S3Client getS3Client(String endpoint) throws URISyntaxException {
        var client = S3Client.builder()
                .region(S3_REGION)
                .endpointOverride(new URI(endpoint));

        if (endpoint.equals(ENDPOINT)) {

            var role = getValueFromParameterStore(ROLE_PARAM);
            var dbHost = getValueFromParameterStore(DB_HOST_PARAM);
            logger.log("ROLE: " + role);
            logger.log("DB_HOST: " + dbHost);

            var credentials = StsAssumeRoleCredentialsProvider
                    .builder()
                    .stsClient(StsClient
                            .builder()
                            .region(S3_REGION)
                            .build())
                    .refreshRequest(AssumeRoleRequest
                            .builder()
                            .roleArn("arn:aws:iam::577373831711:role/bfd-test-eft-ab2d-bucket-role")
                            .roleSessionName("roleSessionName")
                            .build())
                    .build();
            client.credentialsProvider(credentials);
        }

        return client.build();
    }

    public String getValueFromParameterStore(String key) {
        var ssmClient = SsmClient.builder()
                .region(S3_REGION)
                .build();
        var parameterRequest = GetParameterRequest.builder()
                .name(key)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(parameterRequest);

        logger.log("PARAMETER: " + parameterResponse.parameter().value());

        return parameterResponse.parameter().value();
    }

    public Connection getConnection() {
        logger.log("DB_ U--------:" + System.getenv("AB2D_DB_USER"));
        logger.log("DB_ P--------:" + System.getenv("AB2D_DB_PASSWORD"));
        Properties properties = PropertiesUtil.loadProps();
        try {
            return DriverManager.getConnection(properties.get("DB_URL") + "", properties.get("DB_USERNAME") + "", properties.get("DB_PASSWORD") + "");
        }
        catch (SQLException ex){
            logger.log("Unable to get connection to ab2d database" + ex.getMessage());
            throw new OptOutException("Unable to get connection to ab2d database", ex);
        }
    }

    public void processFileFromS3(BufferedReader reader) {
        var dbConnection = getConnection();
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

    public String getEffectiveDate(String date) {
        return (isRejected) ? " ".repeat(EFFECTIVE_DATE_LENGTH) : date;
    }

    private static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.getOptOutFlag());
        statement.setString(2, optOut.getMbi());
        statement.setString(3, optOut.getMbi());
        statement.addBatch();
    }

}