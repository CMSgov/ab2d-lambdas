package gov.cms.ab2d.attributionDataShare;

import software.amazon.awssdk.regions.Region;

public class AttributionDataShareConstants {

    public static final String ROLE_PARAM = "/ab2d/test/opt-out/bfd-bucket-role-arn";
    public static final String DB_HOST_PARAM = "/ab2d/test/opt-out/db-host";
    public static final String DB_USER_PARAM = "/ab2d/test/opt-out/db-user";
    public static final String DB_PASS_PARAM = "/ab2d/test/opt-out/db-password";
    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String FILE_PATH = "/tmp/";
    public static final String REQ_FILE_NAME = "P.AB2D.NGD.REQ.";
    public static final String REQ_FILE_NAME_PATTERN = "'D'yyMMdd.'T'hhmmsss";
    public static final String FIRST_LINE = "HDR_BENEDATAREQ_";
    public static final String LAST_LINE = "TLR_BENEDATAREQ_";
    public static final String SELECT_STATEMENT = "SELECT * FROM public.current_mbi";
    public static final int CURRENT_MBI_LENGTH = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final String BUCKET_NAME_PROP = "S3_UPLOAD_BUCKET";
    public static final String UPLOAD_PATH_PROP = "S3_UPLOAD_PATH";

    private AttributionDataShareConstants() {
    }
}
