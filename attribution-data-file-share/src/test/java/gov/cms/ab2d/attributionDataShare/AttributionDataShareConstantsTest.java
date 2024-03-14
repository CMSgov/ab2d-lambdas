package gov.cms.ab2d.attributionDataShare;

import com.mockrunner.mock.jdbc.MockResultSet;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.REQ_FILE_NAME;
import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.REQ_FILE_NAME_PATTERN;
import static org.mockito.Mockito.mock;

public class AttributionDataShareConstantsTest {

    public static final String TEST_ENDPOINT = "http://127.0.0.1:8001";
    public static final String FILE_NAME = REQ_FILE_NAME + new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
    public static final String MBI_1 = "DUMMY000001";
    public static final String MBI_2 = "DUMMY000002";
    public static final Timestamp DATE_TIME = Timestamp.valueOf("2024-02-26 00:00:00");
    public static final String DATE = "20240226";

    public static final Connection connection = mock(Connection.class);
    public static final Statement stmt = mock(Statement.class);

    public static MockResultSet resultSet() {
        var rs = new MockResultSet("");
        rs.addColumn("mbi", Arrays.asList(MBI_1, MBI_2));
        rs.addColumn("effective_date", Arrays.asList(DATE_TIME, null));
        rs.addColumn("opt_out_flag", Arrays.asList(true, null));
        return rs;
    }

    private AttributionDataShareConstantsTest() {
    }
}
