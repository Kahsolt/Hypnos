/*
 * Author : Kahsolt <kahsolt@qq.com>
 * Create Date : 2017-12-28
 * Update Date : 2018-01-05
 * Version : v0.2
 * License : GPLv3
 * Description : 主引擎：数据库配置、生成、连接
 */

package tk.kahsolt.hypnos;

import org.apache.log4j.Logger;
import tk.kahsolt.hypnos.db.SQLEngine;
import tk.kahsolt.hypnos.db.MySQLEngine;
import tk.kahsolt.hypnos.db.SQLiteEngine;
import tk.kahsolt.hypnos.model.*;
import tk.kahsolt.sqlbuilder.SQLBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public final class Hypnos {

    private static final Logger logger = Logger.getLogger(Hypnos.class);

    private String dbUri;
    private SQLEngine dbEngine;
    private ArrayList<Class<? extends Model>> modelList = new ArrayList<>();

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
        try {
            Field field = Model.class.getDeclaredField("dbEngine");
            field.setAccessible(true);
            field.set(null, dbEngine);
            field = Model.class.getDeclaredField("sqlBuilder");
            field.set(null, SQLEngine.sqlBuilder);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            logger.error("Failed to inject dbEngine/sqlBuilder into Model.");
        }
        dbEngine.connect();

        for (Class<? extends Model> model: modelList) {
            try {
                Object mod = model.newInstance();
                try {
                    Field field = model.getDeclaredField("objects");
                    field.setAccessible(true);
                    field.set(null, mod);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    boolean isManagerFound = false;
                    for(Field field : model.getDeclaredFields()) {
                        if(field.getDeclaredAnnotation(Manager.class)!=null) {
                            field.setAccessible(true);
                            field.set(null, mod);
                            isManagerFound = true;
                            break;
                        }
                    }
                    if(!isManagerFound)
                        logger.warn("Manager field not found in model " + model.getSimpleName());
                }
                Method method = model.getSuperclass().getDeclaredMethod("sqlize");
                method.setAccessible(true);
                method.invoke(mod);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e1) {
                e1.printStackTrace();
                logger.error("Failed wakeup model " + model.getSimpleName());
            }
        }
    }
    public void stop() { dbEngine.disconnect(); }

}
