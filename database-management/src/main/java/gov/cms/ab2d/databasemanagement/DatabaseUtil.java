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
import java.util.Properties;

public class DatabaseUtil {

    public Connection getConnection() throws SQLException {
        Properties properties = PropertiesUtil.loadProps();
        return DriverManager.getConnection(properties.get("DB_URL") + "", properties.get("DB_USERNAME") + "", properties.get("DB_PASSWORD") + "");
    }

    public Connection setupDb(Connection connection) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new liquibase.Liquibase("db/changelog/changelog.yaml", new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
        return connection;
    }

}
