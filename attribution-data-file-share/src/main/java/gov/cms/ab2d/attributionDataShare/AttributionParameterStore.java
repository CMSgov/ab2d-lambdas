package gov.cms.ab2d.attributionDataShare;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionParameterStore {

    private final String role;
    private final String dbHost;
    private final String dbUser;
    private final String dbPassword;


    public AttributionParameterStore() {
        var ssmClient = SsmClient.builder()
                .region(S3_REGION)
                .build();

        this.role = getValueFromParameterStore(ROLE_PARAM, ssmClient);
        this.dbHost = getValueFromParameterStore(DB_HOST_PARAM, ssmClient);
        this.dbUser = getValueFromParameterStore(DB_USER_PARAM, ssmClient);
        this.dbPassword = getValueFromParameterStore(DB_PASS_PARAM, ssmClient);

        ssmClient.close();
    }

    private String getValueFromParameterStore(String key, SsmClient ssmClient) {
        var parameterRequest = GetParameterRequest.builder()
                .name(key)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(parameterRequest);
        return parameterResponse.parameter().value();
    }

    public String getDbHost(){
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
