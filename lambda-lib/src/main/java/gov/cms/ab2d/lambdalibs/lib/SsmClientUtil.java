package gov.cms.ab2d.lambdalibs.lib;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

public class SsmClientUtil {

    public static SsmClient getClient(){
        return  SsmClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}
