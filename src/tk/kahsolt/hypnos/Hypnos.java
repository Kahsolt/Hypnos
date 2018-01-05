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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

public final class Hypnos {

    private static final Logger logger = Logger.getLogger(Hypnos.class);

    private String dbUri;
    private SQLEngine dbEngine;
    private ArrayList<Class<? extends Model>> modelList = new ArrayList<>();
    private boolean prefetch = false;

    public Hypnos() { }
    public Hypnos(String dbUri) { this.dbUri = dbUri; }

    public void register(Class<? extends Model> clazz) {
        modelList.add(clazz);
    }
    public void start() {
        dbEngine = dbUri==null ? new SQLiteEngine() :
                dbUri.startsWith("jdbc:mysql") ? new MySQLEngine(dbUri) : new SQLiteEngine(dbUri);
        dbEngine.connect();

        boolean configureModel = false;
        HashMap<Class<? extends Model>, Object> managers = new HashMap<>();
        HashMap<Class<? extends Model>, ArrayList<Object>> caches = new HashMap<>();
        HashMap<Class<? extends Model>, Boolean> modes = new HashMap<>();
        for (Class<? extends Model> clazz: modelList) {
            if(!configureModel) {
                try {
                    Class<?> supclazz = clazz.getSuperclass();
                    AccessibleObject.setAccessible(supclazz.getDeclaredFields(), true);
                    Field field = supclazz.getDeclaredField("dbEngine");
                    field.setAccessible(true);
                    field.set(null, dbEngine);
                    supclazz.getDeclaredField("sqlBuilder").set(null, SQLEngine.sqlBuilder);
                    field = supclazz.getDeclaredField("managers");
                    field.setAccessible(true);
                    field.set(null, managers);
                    field = supclazz.getDeclaredField("caches");
                    field.setAccessible(true);
                    field.set(null, caches);
                    field = supclazz.getDeclaredField("modes");
                    field.setAccessible(true);
                    field.set(null, modes);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    logger.error("Failed configuring base Model.");
                }
                configureModel = true;
            }

            Class<?> supclazz = clazz.getSuperclass();
            modes.put(clazz, false);
            boolean foundManager = false;
            boolean foundCache = false;
            try {
                Object mod = clazz.newInstance();
                // 建表/验证存在
                Method method = supclazz.getDeclaredMethod("sqlize");
                method.setAccessible(true);
                method.invoke(mod);

                // 注册Manager/Cache
                for(Field field : clazz.getDeclaredFields()) {
                    if(!foundManager && field.getDeclaredAnnotation(Manager.class)!=null) {
                        field.setAccessible(true);
                        field.set(null, mod);
                        managers.put(clazz, mod);
                        foundManager = true;
                    }
                    if(!foundCache && field.getDeclaredAnnotation(Cache.class)!=null) {
                        foundCache = true;
                        method = supclazz.getDeclaredMethod("all");
                        @SuppressWarnings("unchecked")
                        ArrayList<Object> cache = (ArrayList) method.invoke(mod);
                        caches.put(clazz, cache);
                        modes.put(clazz, true);
                    }
                    if(foundManager && foundCache) break;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                logger.error("Failed wakeup model " + clazz.getSimpleName());
            } finally {
                if(foundManager) logger.info(String.format("Manager found for model '%s'.", clazz.getSimpleName()));
                else logger.warn(String.format("No manager found for model '%s', not recommended.", clazz.getSimpleName()));;
                if(foundCache) logger.info(String.format("Cache found for model '%s', works in offline mode.", clazz.getSimpleName()));
                else logger.info(String.format("No cache found for model '%s', differential update enabled.", clazz.getSimpleName()));;
            }
        }
    }
    public void stop() { dbEngine.disconnect(); }

}
