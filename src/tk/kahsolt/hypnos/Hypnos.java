/*
 * Author : Kahsolt <kahsolt@qq.com>
 * Date : 2017-12-28
 * License : GPLv3
 * Description : 主引擎：数据库配置、生成、连接
 */

package tk.kahsolt.hypnos;

import org.apache.log4j.Logger;
import tk.kahsolt.hypnos.db.MySQLEngine;
import tk.kahsolt.hypnos.db.SQLEngine;
import tk.kahsolt.hypnos.db.SQLiteEngine;
import tk.kahsolt.hypnos.model.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public final class Hypnos {

    private static final Logger logger = Logger.getLogger(Hypnos.class);

    private SQLEngine dbEngine;
    private ArrayList<Class<? extends Model>> modelList = new ArrayList<>();

    private String dbUri;

    public Hypnos() { }
    public Hypnos(String dbUri) { this.dbUri = dbUri; }

    public void register(Class<? extends Model> clazz) {
        modelList.add(clazz);
    }
    public void start() {
        if(dbUri==null) {
            dbEngine = new SQLiteEngine();
        } else {
            if(dbUri.startsWith("jdbc:mysql")) dbEngine = new MySQLEngine(dbUri);
            else dbEngine = new SQLiteEngine(dbUri);
        }
        dbEngine.connect();

        boolean isEngineInjected = false;
        for (Class<? extends Model> model: modelList) {
            try {
                Class<?> supclazz = model.getSuperclass();
                if(!isEngineInjected){
                    Field field = supclazz.getDeclaredField("dbEngine");
                    field.set(null, dbEngine);
                    isEngineInjected = true;
                }
                Object mod = model.newInstance();
                try {
                    Field field = model.getDeclaredField("objects");
                    field.setAccessible(true);
                    field.set(mod, model.newInstance());
                } catch (NoSuchFieldException e) {
                    logger.warn("Manager field 'objects' not found in model " + model.getSimpleName());
                }
                Method method = supclazz.getDeclaredMethod("sqlize");
                method.setAccessible(true);
                method.invoke(mod);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Failed to inject engine into model " + model.getSimpleName());
            } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e1) {
                e1.printStackTrace();
                logger.error("Failed wakeup model " + model.getSimpleName());
            }
        }
    }
    public void stop() {
        dbEngine.disconnect();
    }

}
