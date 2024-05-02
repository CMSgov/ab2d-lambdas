package gov.cms.ab2d.attributiondatashare;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import static gov.cms.ab2d.attributiondatashare.AttributionDataShareConstants.*;

public class AttributionParameterStore {

    private final String role;
    private final String dbHost;
    private final String dbUser;
    private final String dbPassword;

    public AttributionParameterStore(String role, String dbHost, String dbUser, String dbPassword) {
        this.role = role;
        this.dbHost = dbHost;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public static AttributionParameterStore getParameterStore() {
        var ssmClient = SsmClient.builder()
                .region(S3_REGION)
                .build();

        var role = getValueFromParameterStore(ROLE_PARAM, ssmClient);
        var dbHost = getValueFromParameterStore(DB_HOST_PARAM, ssmClient);
        var dbUser = getValueFromParameterStore(DB_USER_PARAM, ssmClient);
        var dbPassword = getValueFromParameterStore(DB_PASS_PARAM, ssmClient);

        ssmClient.close();
        return new AttributionParameterStore(role, dbHost, dbUser, dbPassword);
    }

    private static String getValueFromParameterStore(String key, SsmClient ssmClient) {
        var parameterRequest = GetParameterRequest.builder()
                .name(key)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(parameterRequest);
        return parameterResponse.parameter().value();
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getRole() {
        return role;
    }
}



