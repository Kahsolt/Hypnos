package tk.kahsolt.hypnos.db;

import tk.kahsolt.sqlbuilder.SQLBuilder;
import tk.kahsolt.sqlbuilder.sql.Dialect;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class SQLiteEngine extends SQLEngine {

    static { sqlBuilder = new SQLBuilder(Dialect.SQLITE); }

    public SQLiteEngine() { dbUri = String.format("jdbc:sqlite://%s%ssqlite3.db", System.getProperty("user.dir"), System.getProperty("file.separator")); }
    public SQLiteEngine(String dbUri) { this.dbUri = dbUri.startsWith("jdbc:sqlite://") ? dbUri: "jdbc:sqlite://" + dbUri; }

    @Override
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            Properties pro = new Properties();
            pro.put("date_string_format", "yyyy-MM-dd HH:mm:ss");
            dbConnection = DriverManager.getConnection(this.dbUri, pro);
            dbConnection.setAutoCommit(true);
            dbConnection.createStatement().execute("PRAGMA foreign_keys = ON;");
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find jdbc driver");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL execution error");
        }
    }

}
