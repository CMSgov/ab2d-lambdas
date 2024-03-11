package gov.cms.ab2d.optout;

import software.amazon.awssdk.regions.Region;

public class OptOutConstants {
    public static final String ENDPOINT = "https://s3.amazonaws.com";
    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final String TEST_FILE_NAME = "optOutDummy.txt";
    public static final String TEST_BFD_BUCKET_NAME = "bfd-test-eft";
    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String HEADER_RESP = "HDR_BENEDATARSP";
    public static final String TRAILER_RESP = "TLR_BENEDATARSP";
    public static final String AB2D_HEADER_CONF = "HDR_BENECONFIRM";
    public static final String AB2D_TRAILER_CONF = "TLR_BENECONFIRM";
    public static final int MBI_INDEX_START = 0;
    public static final int MBI_INDEX_END = 11;
    public static final int OPTOUT_FLAG_INDEX = 11;
    public static final String EFFECTIVE_DATE_PATTERN = "yyyyMMdd";
    public static final int EFFECTIVE_DATE_LENGTH = 8;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String CONF_FILE_NAME = "P.AB2D.NGD.CONF.";
    public static final String CONF_FILE_NAME_PATTERN = "'D'yyMMdd.'T'hhmmsss";
    public static final String CONF_FILE_FORMAT = ".OUT.txt";
    public static final String UPDATE_STATEMENT = "UPDATE public.coverage\n" +
            "SET opt_out_flag = ?, effective_date = current_timestamp\n" +
            "WHERE current_mbi = ? OR historic_mbis LIKE CONCAT( '%',?,'%')";


    private OptOutConstants() {
    }
}