package gov.cms.ab2d.databasemanagement;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DatabaseManagementHandler implements RequestStreamHandler {

    @SneakyThrows
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        // create the schema externally since liquibase also uses it for the log tables
        Connection connection = DatabaseUtil.getConnection();
        try (PreparedStatement stmt = connection
                .prepareStatement("CREATE SCHEMA if not exists lambda")) {
            stmt.execute();
        }
        DatabaseUtil.setupDb(connection);
        outputStream.write("{\"status\": \"database update complete\", \"Updated\":\"\" }".getBytes(StandardCharsets.UTF_8));
    }
}
