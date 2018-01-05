package tk.kahsolt.hypnos.db;

import org.apache.log4j.Logger;
import tk.kahsolt.sqlbuilder.SQLBuilder;

import java.sql.*;
import java.util.ArrayList;

public abstract class SQLEngine {

    private static final Logger logger = Logger.getLogger(SQLEngine.class);

    public static SQLBuilder sqlBuilder;

    protected String dbUri;
    protected Connection dbConnection;

    // for connection control
    public abstract void connect();
    public void disconnect() {
        try {
            if(!dbConnection.getAutoCommit()) {
                try {
                    dbConnection.commit();  // in case you forget to commit
                } catch (SQLException e) { }
            }
            dbConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // for DCL
    public void begin() {
        try {
            dbConnection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void commit() {
        try {
            dbConnection.commit();
            dbConnection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * for DDL and DML
     *   DDL正常返回值==0
     *   DML正常返回值>=0
     *   执行失败返回值==-1
     */
    public int execute(String sqlTemplate, Object... parameters) {
        logger.debug("\\Execute\\ " + sqlTemplate);
        try {
            PreparedStatement ps = dbConnection.prepareStatement(sqlTemplate);
            for (int i = 1; i <= parameters.length; i++) ps.setObject(i, parameters[i-1]);
            int effectedRows = ps.executeUpdate();
            ps.close();
            return effectedRows;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if(!dbConnection.getAutoCommit()) dbConnection.rollback();
            } catch (SQLException e1) { e1.printStackTrace(); }
        }
        return -1;
    }

    /*
     * for DQL
     *   acquire    取第一个标量Object          类型映射关系丧失，通常用于SELECT COUNT
     *   fetch      取第一行ArrayList<Object>   类型映射关系丧失，通常用于取三个内定字段的值
     *   query      取结果集ResultSet(记得关闭)  类型映射在Model.modalize()中处理，用于模型化
     */
    public Object acquire(String sqlTemplate, Object... parameters) {
        logger.debug("/Acquire/ " + sqlTemplate);
        try {
            ResultSet rs = query(sqlTemplate, parameters);
            rs.next();
            Object res = rs.getObject(1);
            rs.close();
            return res;
        } catch (SQLException e) { }
        return null;
    }
    public ArrayList<Object> fetch(String sqlTemplate, Object... parameters) {
        logger.debug("/Fetch/ " + sqlTemplate);
        try {
            ResultSet rs = query(sqlTemplate, parameters);
            ArrayList<Object> res = new ArrayList<>();
            rs.next();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) res.add(rs.getObject(i));
            rs.close();
            return res;
        } catch (SQLException e) { }
        return new ArrayList<>();
    }
    public ResultSet query(String sqlTemplate, Object... parameters) {
        logger.debug("/Query/ " + sqlTemplate);
        try {
            // FIXME: 这个实现很不安全，如果有两个以上LIKE就会失败，若确有需要请用非含参SQL查询
            if(sqlTemplate.contains(" LIKE ")) parameters[0] = String.format("%%%s%%", parameters[0]);
            PreparedStatement ps = dbConnection.prepareStatement(sqlTemplate);
            for (int i = 1; i <= parameters.length; i++) ps.setObject(i, parameters[i-1]);
            return ps.executeQuery();
        } catch (SQLException | ArrayIndexOutOfBoundsException e) { e.printStackTrace(); }
        return null;
    }

    // for DEBUG (should never use)
    @Deprecated
    private void sql(String sql) {
        String prefix = sql.trim().substring(0, 6);
        if(prefix.equalsIgnoreCase("SELECT")) {
            ResultSet rs = query(sql);
            try {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int rowCount = 0;
                for (int i = 1; i <= columnCount; i++)
                    System.out.print(String.format("%s\t", rsmd.getColumnLabel(i)));
                System.out.println();
                while (rs.next()) {
                    rowCount++;
                    for (int i = 1; i <= columnCount; i++)
                        System.out.print(String.format("%s\t", rs.getObject(i)));
                    System.out.println();
                }
                if(rowCount==0) System.out.println("Empty Result Set.");
                else if(rowCount==1) System.out.println("1 row in total.");
                else System.out.println(String.format("%d rows in total.", rowCount));
                rs.close();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            int effectedRows = execute(sql);
            if(effectedRows == -1) System.out.println("SQL execution error.");
            else System.out.println(String.format("(%d) rows effected.", effectedRows));
        }
    }

}