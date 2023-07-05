package gov.cms.ab2d.optout;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {

    public static final int BATCH_INSERT_SIZE = 10000;
    public static final String UPDATE_WITH_OPTOUT = "UPDATE public.coverage\n" +
            "SET opt_out_flag = ?, " +
            "effective_date = ?\n" +
            "WHERE beneficiary_id = ?";

    public static Connection getConnection() throws SQLException {
        Properties properties = PropertiesUtil.loadProps();
        return DriverManager.getConnection(properties.getProperty("DB_URL"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
    }

    public static void prepareInsert(OptOutInformation optOut, PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, optOut.isOptOut());
        statement.setTimestamp(2, optOut.getEffectiveDate());
        statement.setInt(3, optOut.getMbi());
        statement.addBatch();
    }
}
