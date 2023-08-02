package gov.cms.ab2d.databasemanagement;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseUtil {
    private DatabaseUtil() {
    }

    public static final List<String> SCHEMAS = Stream.of("lambda", "public").collect(Collectors.toCollection(ArrayList::new));

    public static final String CREATE_SCHEMA_STATEMENT = "CREATE SCHEMA if not exists ";

    public static Connection getConnection() throws SQLException {
        Properties properties = PropertiesUtil.loadProps();
        return DriverManager.getConnection(properties.get("DB_URL") + "", properties.get("DB_USERNAME") + "", properties.get("DB_PASSWORD") + "");
    }

    public static Connection setupDb(Connection connection) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new liquibase.Liquibase("db/changelog/changelog.yaml", new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
        return connection;
    }

}
