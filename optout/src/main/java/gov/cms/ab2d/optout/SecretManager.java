package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.util.Optional;

import static gov.cms.ab2d.optout.OptOutConstants.S3_REGION;

public class SecretManager {

    private SecretManager() {
    }

    public static Optional<StaticCredentialsProvider> getS3Credentials(String accessKeyId, String  secretAccessKey, String accessToken, LambdaLogger logger) {
        try (SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(S3_REGION)
                .build()) {
            var key = getValue(secretsClient, accessKeyId);
            var secret = getValue(secretsClient, secretAccessKey);
            var token = getValue(secretsClient, accessToken);

            return Optional.of(StaticCredentialsProvider.create(AwsSessionCredentials.create(key, secret, token)));
        } catch (SecretsManagerException ex) {
            logger.log(ex.awsErrorDetails().errorMessage());
        }
        return Optional.empty();
    }

    private static String getValue(SecretsManagerClient secretsClient, String secretName) {
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
        return valueResponse.secretString();
    }
}