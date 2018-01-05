package tk.kahsolt.hypnos.model;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class TypeMap {

    private static HashMap<Class<?>, String> type_map = new HashMap<Class<?>, String>() {
        {
            // Numeric
            put(Byte.class, "TINYINT");         put(byte.class, "TINYINT");
            put(Short.class, "SMALLINT");       put(short.class, "SMALLINT");
            put(Integer.class, "INT");          put(int.class, "INT");
            put(Long.class, "BIGINT");          put(long.class, "BIGINT");
            put(Float.class, "FLOAT");          put(float.class, "FLOAT");
            put(Double.class, "DOUBLE");        put(double.class, "DOUBLE");
            // Textual
            put(String.class, "VARCHAR");       // 可设置length()从而改变映射为CHAR/TEXT，具体参考model.Column
            put(UUID.class, "CHAR(36)");
            put(Object.class, "TEXT");
            // Temporal
            put(java.util.Date.class, "DATETIME");
            put(java.sql.Date.class, "DATE");
            put(Time.class, "TIME");
            put(Timestamp.class, "TIMESTAMP");
        }
    };

    private static HashSet<Class<?>> type_numeric = new HashSet<Class<?>>() {
        {
            add(Byte.class);    add(byte.class);
            add(Short.class);   add(short.class);
            add(Integer.class); add(int.class);
            add(Long.class);    add(long.class);
            add(Float.class);   add(float.class);
            add(Double.class);  add(double.class);
        }
    };

    private static HashSet<Class<?>> type_textual = new HashSet<Class<?>>() {
        {
            add(String.class);
            add(UUID.class);
            add(Object.class);
        }
    };

    private static HashSet<Class<?>> type_temporal = new HashSet<Class<?>>() {
        {
            add(java.util.Date.class);
            add(java.sql.Date.class);
            add(Time.class);
            add(Timestamp.class);
        }
    };

    public static String lookup(Class<?> clazz) {
        return type_map.containsKey(clazz)? type_map.get(clazz) : type_map.get(String.class);
    }

    public static boolean isNumeric(Class<?> clazz) {
        return type_numeric.contains(clazz);
    }

    public static boolean isTextual(Class<?> clazz) {
        return type_textual.contains(clazz);
    }

    public static boolean isTemporal(Class<?> clazz) {
        return type_temporal.contains(clazz);
    }

}
