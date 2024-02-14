package gov.cms.ab2d.attributionDataShare;

import software.amazon.awssdk.regions.Region;

public class AttributionDataShareHandlerConstants {

    private AttributionDataShareHandlerConstants() {
    }

    public static final String BFD_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";
    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String FILE_PATH = "/tmp/";
    public static final String FILE_PARTIAL_NAME = "ab2d-beneids_";
    public static final String FILE_FORMAT = ".txt";
    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String COPY_STATEMENT = "COPY public.current_mbi TO STDOUT";
}
