package tk.kahsolt.hypnos.db;

import tk.kahsolt.sqlbuilder.SQLBuilder;
import tk.kahsolt.sqlbuilder.sql.Dialect;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLEngine extends SQLEngine {

    static { sqlBuilder = new SQLBuilder(Dialect.MYSQL); }

    public MySQLEngine() { dbUri = "jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=UTF-8"; }
    public MySQLEngine(String dbUri) { this.dbUri = dbUri; }
    public MySQLEngine(String dbHost, String dbPort, String dbSchema, String dbUsername, String dbPassword) {
        dbHost = dbHost != null ? dbHost : "localhost";
        dbPort = dbPort != null ? dbPort : "3306";
        dbSchema = dbSchema != null ? dbSchema : "test";
        dbUsername = dbUsername != null ? dbUsername : "root";
        dbPassword = dbPassword != null ? dbPassword : "";
        this.dbUri = String.format("jdbc:mysql://%s:%s/%s?user=%s&&password=%s&useUnicode=true&characterEncoding=UTF-8",
                dbHost, dbPort, dbSchema, dbUsername, dbPassword);
    }

    @Override
    public void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            dbConnection = DriverManager.getConnection(this.dbUri);
            dbConnection.setAutoCommit(true);
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find jdbc driver");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL execution error");
        }
    }

}
