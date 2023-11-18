package gov.cms.ab2d.attributionDataShare;

public class AttributionDataShareHandlerConstants {

    private AttributionDataShareHandlerConstants() {
    }

    public static final String FILE_PATH = "/tmp/";
    public static final String FILE_PARTIAL_NAME = "ab2d-beneids_";
    public static final String FILE_FORMAT = ".txt";
    public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String COPY_STATEMENT = "COPY public.current_mbi TO STDOUT";
}
