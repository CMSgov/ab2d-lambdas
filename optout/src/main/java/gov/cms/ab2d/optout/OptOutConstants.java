package gov.cms.ab2d.optout;

import software.amazon.awssdk.regions.Region;

public class OptOutConstants {
    public static final String BFD_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final int DEFAULT_LINE_LENGTH = 459;
    public static final int MBI_INDEX_START = 0;
    public static final int MBI_INDEX_END = 11;
    public static final int EFFECTIVE_DATE_INDEX_START = 354;
    public static final int EFFECTIVE_DATE_INDEX_END = 362;
    public static final int OPTOUT_FLAG_INDEX = 368;
    public static final String RECORD_STATUS_PATTERN = "%-10s";
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String RESPONSE_FILE_NAME = "P.AB2D.DPRF.RSP.";
    public static final String RESPONSE_FILE_NAME_PATTERN = "'D'yyMMdd.'T'hhmmssss";

    public static final String UPDATE_STATEMENT = "UPDATE public.coverage\n" +
            "SET opt_out_flag = ?, effective_date = ?\n" +
            "WHERE current_mbi = ? OR historic_mbis LIKE CONCAT( '%',?,'%')";


    private OptOutConstants() {
    }
}
