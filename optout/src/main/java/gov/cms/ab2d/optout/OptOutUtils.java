package gov.cms.ab2d.optout;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class OptOutUtils {

    private OptOutUtils() {
    }

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





