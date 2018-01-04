package tk.kahsolt.hypnos.model;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
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
            // Temporal
            put(java.util.Date.class, "DATETIME");
            put(java.sql.Date.class, "DATE");
            put(Time.class, "TIME");
            put(Timestamp.class, "TIMESTAMP");
            // General
            put(Object.class, "TEXT");
        }
    };

    public static String lookup(Class<?> clazz) {
        return type_map.containsKey(clazz)? type_map.get(clazz) : type_map.get(String.class);
    }

}
