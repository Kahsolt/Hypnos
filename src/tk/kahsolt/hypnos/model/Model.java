/*
 * Author : Kahsolt <kahsolt@qq.com>
 * Create Date : 2017-12-28
 * Update Date : 2018-01-04
 * License : GPLv3
 * Description : 数据表的活动记录，即模型
 */

package tk.kahsolt.hypnos.model;

import org.apache.log4j.Logger;
import tk.kahsolt.hypnos.db.MySQLEngine;
import tk.kahsolt.hypnos.db.SQLEngine;
import tk.kahsolt.hypnos.db.SQLiteEngine;
import tk.kahsolt.sqlbuilder.sql.Query;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;

public abstract class Model {

    private enum CompareOperator {
        NULL, NOT_NULL,
        EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
        BETWEEN, LIKE
    }

    private static final Logger logger = Logger.getLogger(Model.class);

    // Kernels
    public static SQLEngine dbEngine;       // DB engine injected in when hypnos.start()
    // public static Model objects;         // 模型类约定的管理器字段(写在子类里)，用一个静态实例代表整个模型(伪管理者模式)

    // Caches
    // private static boolean isDifferentialUpdate = true;  // FIXME: 如果副本占用过大再考虑设计非差分更新模式
    private HashMap<String, Object> copy;   // 保存每个模型的自定义字段值的副本，实现增量UPDATE
    private static HashMap<String, String> sqlTemplates
            = new HashMap<>();              // 缓存模板SQL语句

    // Auto-generated and managed fields
    @Column(identity = true)
    private Integer id;
    @Column(initSetCurrent = true)
    private Timestamp create_time;
    @Column(updateSetCurrent = true)
    private Timestamp update_time;

    // Methods to handle the table
    private ArrayList<Object> findByOperator(String field, CompareOperator operator, Object... values) {
        String sqlName = String.format("find_%s(%s)_%s", this.getClass().getSimpleName(), field, operator);
        String sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            Query.Condition sqlCond = dbEngine.sqlBuilder.select("*")
                    .from(this.getClass().getSimpleName())
                    .where(field);
            switch (operator) {
                case NULL:          sql = sqlCond.isnull().end(); break;
                case NOT_NULL:      sql = sqlCond.isnotnull().end(); break;
                case EQUAL:         sql = sqlCond.eq().end(); break;
                case NOT_EQUAL:     sql = sqlCond.ne().end(); break;
                case GREATER:       sql = sqlCond.gt().end(); break;
                case GREATER_EQUAL: sql = sqlCond.ge().end(); break;
                case LESS:          sql = sqlCond.lt().end(); break;
                case LESS_EQUAL:    sql = sqlCond.le().end(); break;
                case BETWEEN:       sql = sqlCond.between().end(); break;
                case LIKE:          sql = sqlCond.like().end(); break;
            }
            sqlTemplates.put(sqlName, sql);
        }
        return modelize(sql, values);
    }
    public ArrayList<Object> findNull(String field) { return findByOperator(field, CompareOperator.NULL); }
    public ArrayList<Object> findNotNull(String field) { return findByOperator(field, CompareOperator.NOT_NULL); }
    public ArrayList<Object> findEqual(String field, Object value) { return findByOperator(field, CompareOperator.EQUAL, value); }
    public ArrayList<Object> findNotEqual(String field, Object value) { return findByOperator(field, CompareOperator.NOT_EQUAL, value); }
    public ArrayList<Object> findGreater(String field, Object value) { return findByOperator(field, CompareOperator.GREATER, value); }
    public ArrayList<Object> findGreaterEqual(String field, Object value) { return findByOperator(field, CompareOperator.GREATER_EQUAL, value); }
    public ArrayList<Object> findLess(String field, Object value) { return findByOperator(field, CompareOperator.LESS, value); }
    public ArrayList<Object> findLessEqual(String field, Object value) { return findByOperator(field, CompareOperator.LESS_EQUAL, value); }
    public ArrayList<Object> findBetween(String field, Object minValue, Object maxValue) { return findByOperator(field, CompareOperator.BETWEEN, minValue, maxValue);}
    public ArrayList<Object> findLike(String field, Object value) { return findByOperator(field, CompareOperator.LIKE, value);}
    public ArrayList<Object> find(String sqlCondition) {
        String sql = String.format("SELECT * FROM %s WHERE %s;", this.getClass().getSimpleName(), sqlCondition);
        return modelize(sql);
    }
    public ArrayList<Object> all() {
        String sqlName = String.format("all_%s", this.getClass().getSimpleName());
        String sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            sql = String.format("SELECT * FROM %s;", this.getClass().getSimpleName());
            sqlTemplates.put(sqlName, sql);
        }
        return modelize(sql);
    }
    public Object get(String field, Object value) {
        String sqlName = String.format("get_%s(%s)", this.getClass().getSimpleName(), field);
        String sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            sql = dbEngine.sqlBuilder.select("*").from(this.getClass().getSimpleName())
                    .where(field).eq().orderBy("update_time", true).limit(1).end();
            sqlTemplates.put(sqlName, sql);
        }
        ArrayList<Object> res = modelize(sql, value);
        if(res.size()==1) return res.get(0);
        else return null;
    }
    public Object get(String sqlCondition) {
        String sql = String.format("SELECT * FROM %s WHERE %s ORDER BY update_time DESC LIMIT 1;",
                this.getClass().getSimpleName(), sqlCondition);
        ArrayList<Object> res = modelize(sql);
        if(res.size()==1) return res.get(0);
        else return null;
    }
    public int count() {
        String sqlName = String.format("count_%s", this.getClass().getSimpleName());
        String sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            sql = String.format("SELECT COUNT(*) FROM %s;", this.getClass().getSimpleName());
            sqlTemplates.put(sqlName, sql);
        }
        Object res = dbEngine.acquire(sql);
        if(res==null) return -1;
        else return ((Number)res).intValue();
    }
    public int count(String sqlCondition) {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s;",
                this.getClass().getSimpleName(), sqlCondition);
        Object res = dbEngine.acquire(sql);
        if(res==null) return -1;
        else return ((Number)res).intValue();
    }
    @Deprecated
    public int update(String field, Object value, String sqlCondition) {
        String sql = dbEngine.sqlBuilder.update(this.getClass().getSimpleName())
                .set(field).where(new Query.Condition().setSqlCondition(sqlCondition)).end();
        return dbEngine.execute(sql, value);
    }
    @Deprecated
    public int delete(String sqlCondition) {
        String sql = String.format("DELETE FROM %s WHERE %s;", this.getClass().getSimpleName(), sqlCondition);
        return dbEngine.execute(sql);
    }

    private ArrayList<Object> modelize(String sql, Object... values) {  // execute sql, pack results to models
        ArrayList<Object> models = new ArrayList<>();
        try {
            ResultSet rs = dbEngine.query(sql, values);
            Class clazz = this.getClass();
            Class supclazz = this.getClass().getSuperclass();
            AccessibleObject.setAccessible(clazz.getDeclaredFields(), true);
            AccessibleObject.setAccessible(supclazz.getDeclaredFields(), true);
            while (rs.next()) {
                Object model = clazz.newInstance();
                HashMap<String, Object> copy = new HashMap<>();
                try {
                    supclazz.getDeclaredField("copy").set(model, copy);
                    ArrayList<Field> fields = new ArrayList<>();
                    fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                    fields.addAll(Arrays.asList(supclazz.getDeclaredFields()));
                    for (Field field: fields) {
                        if(field.getDeclaredAnnotation(Column.class)==null) continue;
                        String name = field.getName();
                        Class<?> type = field.getType();
                        try {
                            Object val = rs.getObject(name, type);
                            field.set(model, val);
                        } catch (SQLException | IllegalAccessException e) {  // for those DB as silly as SQLite
                            // Textual
                            if(type==String.class||type==Object.class) field.set(model, rs.getString(name));
                            if(type==UUID.class) field.set(model, UUID.fromString(rs.getString(name)));
                            // Numeric
                            else if(type==Integer.class) field.set(model, rs.getInt(name));
                            else if(type==int.class) field.setInt(model, rs.getInt(name));
                            else if(type==Double.class) field.set(model, rs.getDouble(name));
                            else if(type==double.class) field.setDouble(model, rs.getDouble(name));
                            else if(type==Long.class) field.set(model, rs.getLong(name));
                            else if(type==long.class) field.setLong(model, rs.getLong(name));
                            else if(type==Float.class) field.set(model, rs.getFloat(name));
                            else if(type==float.class) field.setFloat(model, rs.getFloat(name));
                            else if(type==Short.class) field.set(model, rs.getShort(name));
                            else if(type==short.class) field.setShort(model, rs.getShort(name));
                            else if(type==Byte.class) field.set(model, rs.getByte(name));
                            else if(type==byte.class) field.setByte(model, rs.getByte(name));
                            // Temporal
                            else if(type==Timestamp.class) field.set(model, rs.getTimestamp(name));
                            else if(type==java.util.Date.class) field.set(model, new java.util.Date(rs.getDate(name).getTime()));
                            else if(type==java.sql.Date.class) field.set(model, rs.getDate(name));
                            else if(type==Time.class) field.set(model, rs.getTime(name));
                            else try {
                                    field.set(model, rs.getString(name));
                                } catch (SQLException | IllegalAccessException e1) {
                                    logger.error(String.format("Type '%s' not supported, use VARCHAR instead!", type));
                                }
                        }
                        copy.put(field.getName(), field.get(model));    // save a copy
                    }
                } catch (SQLException | NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
                models.add(model);
            }
            rs.close();
        } catch (SQLException | IllegalAccessException | InstantiationException e) { e.printStackTrace(); }
        return models;
    }
    private void sqlize() { // reflect model class, generate sql CREATE TABLE and execute it
        if(isTableExists()) return;

        Class clazz = this.getClass();
        Class supclazz = this.getClass().getSuperclass();
        AccessibleObject.setAccessible(clazz.getDeclaredFields(), true);
        AccessibleObject.setAccessible(supclazz.getDeclaredFields(), true);
        tk.kahsolt.sqlbuilder.sql.Table table = dbEngine.sqlBuilder.createTable(clazz.getSimpleName());
        try {
            // 人为控制建表时column的顺序 :(
            dumpFieldToColumn(table, supclazz.getDeclaredField("id"));
            for (Field field : clazz.getDeclaredFields())
                dumpFieldToColumn(table, field);
            dumpFieldToColumn(table, supclazz.getDeclaredField("create_time"));
            dumpFieldToColumn(table, supclazz.getDeclaredField("update_time"));
        } catch (NoSuchFieldException e) {
            logger.error("Conventional fields not found, did you extend Model?");
            return;
        }
        String sql = table.engine("InnoDB").charset("utf8").end();
        if(dbEngine.execute(sql)!=0) logger.warn("CREATE TABLE returned None-Zero value, maybe a fault.");
    }
    private boolean isTableExists() {
        String sql;
        if(dbEngine instanceof MySQLEngine) {
            sql = String.format("SELECT COUNT(*) FROM information_schema.TABLES WHERE table_name = '%s';",
                    this.getClass().getSimpleName());
        } else if(dbEngine instanceof SQLiteEngine){
            sql = String.format("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '%s';",
                    this.getClass().getSimpleName());
        } else return false;
        Object res = dbEngine.acquire(sql);
        return res!=null && ((Number)res).intValue() == 1;
    }
    private void dumpFieldToColumn(tk.kahsolt.sqlbuilder.sql.Table table, Field field) {
        Column f = field.getDeclaredAnnotation(Column.class);
        if (f==null) return;
        // Name
        tk.kahsolt.sqlbuilder.sql.Table.Column column = new tk.kahsolt.sqlbuilder.sql.Table.Column(field.getName(), table);
        // Type + length
        String t = TypeMap.lookup(field.getType());
        if(t.equals("VARCHAR")) {
            if(f.length()>0) column.type(String.format("VARCHAR(%d)", f.length()));
            else if(f.length()==0) column.type("TEXT");
            else column.type(String.format("CHAR(%d)", -f.length()));
        } else column.type(t);
        // Default
        if(!f.defaultValue().isEmpty()) {
            String strVal = f.defaultValue();
            Object val;
            try {
                val = Integer.parseInt(strVal);
            } catch (NumberFormatException e1) {
                try {
                    val = Double.parseDouble(strVal);
                } catch (NumberFormatException e2) {
                    val = strVal;
                }
            }
            column.defaultValue(val);
        }
        // PK + AI
        if(f.identity()) column.type("INTEGER").primaryKey().autoIncrement();
        // UN
        if(f.unique()) column.unique();
        // NN
        if(f.notNull()) column.notNull();
        // CT
        if(f.initSetCurrent()) column.initSetCurrent();
        if(f.updateSetCurrent()) column.updateSetCurrent();
        // FK
        if(f.referencesTo()!=Model.class) column.type("INTEGER")
                .referencesTo(f.referencesTo().getSimpleName(), f.isUpdateCascade(), f.isDeleteCascade());
        table.column(column);
    }

    // Methods to handle a tuple in table
    public boolean exists() { return id!=null; }
    public void remove() {
        if(id==null || isManager()) return;

        String sql = String.format("DELETE FROM %s WHERE id = ?;", this.getClass().getSimpleName());
        if(dbEngine.execute(sql, id)!=1) {
            logger.error("DELETE rejected, probably not exists or breaks constraints.");
            return;
        }
        id = null;
    }
    public void save() {    // INSERT or UPDATE changed fields only
        if(id==null) insert();
        else update(true);
    }
    @Deprecated
    public void push() {    // INSERT or UPDATE all fields, you'd better ALWAYS use save()
        if(id==null) insert();
        else update(false);
    }

    private void insert() {
        if(isManager()) return;

        Class<? extends Model> clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        copy = new HashMap<String, Object>();
        for (Field field : fields) {
            if(field.getDeclaredAnnotation(Column.class)==null) continue;
            try {
                Object val = field.get(this);
                copy.put(field.getName(), val); // save copy
                columns.add(field.getName());
                if(val instanceof UUID) val = val.toString();
                values.add(val);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        String[] cols = columns.toArray(new String[columns.size()]);
        Object[] vals = values.toArray(new Object[values.size()]);
        String sqlName;
        sqlName = "insert_%s" + this.getClass().getSimpleName();
        String sql = sqlTemplates.get(sqlName);
        if (sql == null) {
            sql = dbEngine.sqlBuilder.insert(this.getClass().getSimpleName())
                    .into(cols).values().end();
            sqlTemplates.put(sqlName, sql);
        }
        if (dbEngine.execute(sql, vals) != 1) {
            logger.error("INSERT rejected, probably breaks constraints.");
            return;
        }

        sqlName = "insert_getmeta_%s" + this.getClass().getSimpleName();
        sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            if(dbEngine instanceof MySQLEngine)
                sql = dbEngine.sqlBuilder.select("id", "create_time", "update_time").from(this.getClass().getSimpleName())
                        .where("id").eq("SELECT @@IDENTITY").end();
            else
                sql = dbEngine.sqlBuilder.select("id", "create_time", "update_time").from(this.getClass().getSimpleName())
                        .where("id").eq("SELECT MAX(id) FROM " + this.getClass().getSimpleName()).end();
            sqlTemplates.put(sqlName, sql);
        }
        ArrayList<Object> res = dbEngine.fetch(sql);
        id = ((Number) res.get(0)).intValue();
        create_time = res.get(1) instanceof Timestamp ? (Timestamp) res.get(1) : Timestamp.valueOf(res.get(1).toString());
        update_time = res.get(2) instanceof Timestamp ? (Timestamp) res.get(2) : Timestamp.valueOf(res.get(2).toString());
    }
    private void update(boolean diffOnly) {
        if(isManager()) return;

        Class<? extends Model> clazz = this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);
        Query sqlUpdate = dbEngine.sqlBuilder.update(this.getClass().getSimpleName());
        ArrayList<Object> values = new ArrayList<>();
        if(diffOnly) {
            for (Field field : fields) {
                if (field.getDeclaredAnnotation(Column.class) == null) continue;
                try {
                    String name = field.getName();
                    Object val = field.get(this);
                    if(val!=copy.get(name)) {   // infer field changes
                        copy.put(name, val);
                        sqlUpdate.set(name);
                        if(val instanceof UUID) val = val.toString();   // FIXME: 上传数据类型转换，特殊处理是不好的设计
                        values.add(val);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if(values.size()==0) return;    // infers no changes at all
        } else {
            for (Field field : fields) {
                if (field.getDeclaredAnnotation(Column.class)==null) continue;
                try {
                    String name = field.getName();
                    Object val = field.get(this);
                    copy.put(name, val);
                    sqlUpdate.set(name);
                    if(val instanceof UUID) val = val.toString();   // FIXME: 上传数据类型转换，特殊处理是不好的设计
                    values.add(val);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        String sql = sqlUpdate.where("id").eq().end();
        values.add(id);
        Object[] vals = values.toArray(new Object[values.size()]);
        if (dbEngine.execute(sql, vals) != 1) {
            logger.error("UPDATE rejected, probably not exists or breaks constraints.");
            return;
        }

        String sqlName = "update_getmeta_%s" + this.getClass().getSimpleName();
        sql = sqlTemplates.get(sqlName);
        if(sql==null) {
            sql = String.format("SELECT update_time FROM %s WHERE id = ?;",
                    this.getClass().getSimpleName());
            sqlTemplates.put(sqlName, sql);
        }
        Object res = dbEngine.acquire(sql, id);
        update_time = res instanceof Timestamp ? (Timestamp) res : Timestamp.valueOf(res.toString());
    }
    private boolean isManager() {
        try {
            this.getClass().getField("objects").setAccessible(true);
            if(this.getClass().getField("objects").get(this)==this) {
                logger.warn("Should never save/push/remove the manager 'objects'.");
                return true;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) { /* then it's save to save */ }
        return false;
    }

    // Getter for the sake of security
    public Integer getId() { return id; }
    public Timestamp getCreateTime() { return create_time; }
    public Timestamp getUpdateTime() { return update_time; }
}
