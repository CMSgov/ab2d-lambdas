package gov.cms.ab2d.attributionDataShare;

import software.amazon.awssdk.regions.Region;

public class AttributionDataShareConstants {

    private AttributionDataShareConstants() {
    }

    public static final String BFD_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";
    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String FILE_PATH = "/tmp/";
    public static final String FILE_PARTIAL_NAME = "ab2d-beneids_";
    public static final String FILE_FORMAT = ".txt";
    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static final String FIRST_LINE = "HDR_BENEDATAREQ_";
    public static final String LAST_LINE = "TLR_BENEDATAREQ_";
    public static final String SELECT_STATEMENT = "SELECT * FROM public.current_mbi";
    public static final int CURRENT_MBI_LENGTH = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final int EFFECTIVE_DATE_LENGTH = 8;
}
