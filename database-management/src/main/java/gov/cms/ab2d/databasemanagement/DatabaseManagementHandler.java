package gov.cms.ab2d.databasemanagement;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseManagementHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // create the schema externally since liquibase also uses it for the log tables
        LambdaLogger logger = context.getLogger();
        Connection connection;
        try {
            connection = DatabaseUtil.getConnection();
            createSchemas(DatabaseUtil.schemas, connection, logger);

            DatabaseUtil.setupDb(connection);
        } catch (Exception e) {
            logger.log(e.getMessage());
        }
        outputStream.write("{\"status\": \"database update complete\", \"Updated\":\"\" }".getBytes(StandardCharsets.UTF_8));
    }

    private void createSchemas(ArrayList<String> schemas, Connection connection, LambdaLogger logger) {
        for (String schema : schemas) {
            try (PreparedStatement stmt = connection
                    .prepareStatement(DatabaseUtil.createSchemaStatement + schema)) {
                stmt.execute();
            } catch (SQLException e) {
                logger.log(e.getMessage());
            }
        }
    }

}
