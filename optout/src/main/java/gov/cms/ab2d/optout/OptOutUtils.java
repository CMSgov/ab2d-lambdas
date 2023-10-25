package gov.cms.ab2d.optout;

import com.amazonaws.regions.Regions;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OptOutUtils {

    private OptOutUtils() {
    }

    public static final String BFD_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";

    public static final String AB2D_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";

    public static final Regions S3_REGION = Regions.US_EAST_1;

    public static final String FILE_NAME = "optOutDummy.txt";

    public static final String FILE_PATH = "/opt/" + FILE_NAME;

    public static final int BATCH_INSERT_SIZE = 10000;
    public static final String UPDATE_WITH_OPTOUT = "UPDATE public.coverage\n" +
            "SET opt_out_flag = ?\n" +
            "WHERE current_mbi = ? OR historic_mbis LIKE CONCAT( '%',?,'%')";


    public static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.isOptOut());
        statement.setString(2, optOut.getMbi());
        statement.setString(3, optOut.getMbi());
        statement.addBatch();
    }
}





