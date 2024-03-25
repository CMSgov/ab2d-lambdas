package gov.cms.ab2d.optout;

import software.amazon.awssdk.regions.Region;

public class OptOutConstants {
    public static final String ROLE_PARAM = "/ab2d/opt-out/bfd-bucket-role-arn";
    public static final String DB_HOST_PARAM = "/ab2d/opt-out/db-host";
    public static final String DB_USER_PARAM = "/ab2d/opt-out/db-user";
    public static final String DB_PASS_PARAM = "/ab2d/opt-out/db-password";
    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String HEADER_RESP = "HDR_BENEDATARSP";
    public static final String TRAILER_RESP = "TRL_BENEDATARSP";
    public static final String AB2D_HEADER_CONF = "HDR_BENECONFIRM";
    public static final String AB2D_TRAILER_CONF = "TRL_BENECONFIRM";
    public static final int MBI_INDEX_START = 0;
    public static final int MBI_INDEX_END = 11;
    public static final int OPTOUT_FLAG_INDEX = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final int EFFECTIVE_DATE_LENGTH = 8;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String CONF_FILE_NAME = "#EFT.ON.AB2D.NGD.CONF.";
    public static final String CONF_FILE_NAME_PATTERN = "'D'yyMMdd.'T'HHmmsss";
    public static final String UPDATE_STATEMENT = "UPDATE public.coverage\n" +
            "SET opt_out_flag = ?, effective_date = current_timestamp\n" +
            "WHERE current_mbi = ? OR historic_mbis iLike CONCAT( '%',?,'%')";


    private OptOutConstants() {
    }
}