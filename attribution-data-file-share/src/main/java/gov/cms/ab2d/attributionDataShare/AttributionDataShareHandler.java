package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHandler implements RequestStreamHandler {

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
        String fileName = REQ_FILE_NAME + currentDate;
        var parameterStore = AttributionParameterStore.getParameterStore();
        try (var dbConnection = DriverManager.getConnection(parameterStore.getDbHost(), parameterStore.getDbUser(), parameterStore.getDbPassword())) {
            long start = System.currentTimeMillis();

            var content = AttributionDataShareHelper.getFileContent(dbConnection, logger);
            AttributionDataShareHelper.writeFileToS3Bucket(content, fileName, getS3Client(ENDPOINT, parameterStore), logger);

            long finish = System.currentTimeMillis();
            logger.log("TIME ms: ---------- " + (finish - start));
        } catch (NullPointerException | URISyntaxException | SQLException ex) {
            throwAttributionDataShareException(logger, ex);
        } finally {
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    public S3Client getS3Client(String endpoint, AttributionParameterStore parameterStore) throws URISyntaxException {
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

    void throwAttributionDataShareException(LambdaLogger logger, Exception ex) {
        logger.log(ex.getMessage());
        throw new AttributionDataShareException(ex.getMessage(), ex);
    }

}
